package com.loci.ato_deck_builder_server.services;

import com.loci.ato_deck_builder_server.api.deck.objects.PagedWebDeck;
import com.loci.ato_deck_builder_server.api.deck.objects.WebDeck;
import com.loci.ato_deck_builder_server.database.objects.Deck;
import com.loci.ato_deck_builder_server.database.objects.DeckCard;
import com.loci.ato_deck_builder_server.database.objects.WebCard;
import com.loci.ato_deck_builder_server.database.repositories.CardRepository;
import com.loci.ato_deck_builder_server.database.repositories.DeckCardRepository;
import com.loci.ato_deck_builder_server.database.repositories.DeckRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class DeckServiceImpl implements DeckService {

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final DeckCardRepository deckCardRepository;
    private final KeycloakService keycloakService;

    private final ConcurrentHashMap<String, PagedWebDeck> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Set<String>> deckIdToCacheKeys = new ConcurrentHashMap<>();

    @Autowired
    public DeckServiceImpl(DeckRepository deckRepository, CardRepository cardRepository,
                           DeckCardRepository deckCardRepository, KeycloakService keycloakService) {
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
        this.deckCardRepository = deckCardRepository;
        this.keycloakService = keycloakService;
    }

    @Override
    public Mono<PagedWebDeck> getDecks(int page, int size, String searchQuery, String charId,
                                       String ownedFilter, String userName, boolean sortByLikesFirst) {
        String key = searchQuery + "-" + charId + "-" + ownedFilter + "-" + userName + "-" + sortByLikesFirst + "-" + page + "-" + size;

        // Check if the PagedWebDeck is in the cache
        PagedWebDeck cachedPagedWebDeck = cache.get(key);
        if (cachedPagedWebDeck != null) {
            return Mono.just(cachedPagedWebDeck);
        }

        Mono<DecksAndCount> decksAndCountMono;

        if (ownedFilter.equals("owned") || ownedFilter.equals("unowned")) {
            decksAndCountMono = getUserId(userName).flatMap(userId -> {
                Flux<Deck> decks;
                Mono<Long> totalCountMono;

                if (ownedFilter.equals("owned")) {
                    decks = sortByLikesFirst ?
                            deckRepository.findOwnedDecksByTitleOrderByLikes(searchQuery, charId, userId, size, (long) page * size) :
                            deckRepository.findOwnedDecksByTitleOrderByTitle(searchQuery, charId, userId, size, (long) page * size);

                    totalCountMono = deckRepository.countOwnedDecksByTitle(searchQuery, charId, userId);
                } else { // unowned
                    decks = sortByLikesFirst ?
                            deckRepository.findUnownedDecksByTitleOrderByLikes(searchQuery, charId, userId, size, (long) page * size) :
                            deckRepository.findUnownedDecksByTitleOrderByTitle(searchQuery, charId, userId, size, (long) page * size);

                    totalCountMono = deckRepository.countUnownedDecksByTitle(searchQuery, charId, userId);
                }

                return Mono.just(new DecksAndCount(decks, totalCountMono));
            });
        } else {
            Flux<Deck> decks = sortByLikesFirst ?
                    deckRepository.findDecksByTitleOrderByLikes(searchQuery, charId, size, (long) page * size) :
                    deckRepository.findDecksByTitleOrderByTitle(searchQuery, charId, size, (long) page * size);

            Mono<Long> totalCountMono = deckRepository.countDecksByTitle(searchQuery, charId);

            decksAndCountMono = Mono.just(new DecksAndCount(decks, totalCountMono));
        }

        return decksAndCountMono.flatMap(decksAndCount ->
                processDecks(decksAndCount.decks, decksAndCount.totalCountMono, key, size)
        );
    }

    private Mono<PagedWebDeck> processDecks(Flux<Deck> decks, Mono<Long> totalCountMono, String key, int size) {
        return Mono.zip(decks.collectList(), totalCountMono).flatMap(tuple -> {
            List<Deck> deckList = tuple.getT1();
            Long totalCount = tuple.getT2();

            // Update deckIdToCacheKeys atomically
            deckList.forEach(deck -> {
                deckIdToCacheKeys.compute(deck.getId(), (deckId, cacheKeys) -> {
                    if (cacheKeys == null) {
                        cacheKeys = ConcurrentHashMap.newKeySet();
                    }
                    cacheKeys.add(key);
                    return cacheKeys;
                });
            });

            // Collect unique userIds
            Set<String> userIds = deckList.stream()
                    .map(Deck::getUserId)
                    .collect(Collectors.toSet());

            // Batch fetch usernames
            return Flux.fromIterable(userIds)
                    .flatMap(userId -> getUsername(userId)
                            .map(username -> new AbstractMap.SimpleEntry<>(userId, username)))
                    .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                    .map(userIdToUsernameMap -> {
                        List<WebDeck> webDeckList = deckList.stream()
                                .map(deck -> {
                                    String username = userIdToUsernameMap.get(deck.getUserId());
                                    return createWebDeck(deck, new ArrayList<>(), username);
                                })
                                .collect(Collectors.toList());

                        int totalPages = (int) Math.ceil((double) totalCount / size);
                        PagedWebDeck pagedWebDeck = new PagedWebDeck(webDeckList, totalPages);
                        cache.put(key, pagedWebDeck);
                        return pagedWebDeck;
                    });
        });
    }

    @Override
    public Mono<WebDeck> getDeckDetails(int deckId) {
        Mono<Deck> deckMono = deckRepository.findById(deckId);

        return deckMono.flatMap(this::createWebDeck)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found")));
    }

    @Override
    public Mono<Integer> uploadDeck(WebDeck webDeck, String username) {
        Mono<Integer> savedDeck = deckRepository.insertDeck(webDeck.getTitle(), webDeck.getDescription(), webDeck.getCharacterId(), username);

        return savedDeck.flatMap(id -> {
            List<DeckCard> cards = webDeck.toDeckCards(id);
            return deckCardRepository.saveAll(cards)
                    .then(Mono.fromRunnable(cache::clear))
                    .thenReturn(id);
        });
    }

    @Override
    public Mono<Void> updateDeck(int deckId, WebDeck webDeck, String username) {
        return deckRepository.getUserId(deckId)
                .flatMap(userId -> {
                    if (!userId.equals(username)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this deck"));
                    }
                    // Delete existing cards and update deck
                    return deckCardRepository.deleteByDeckId(deckId)
                            .thenMany(Flux.fromIterable(webDeck.toDeckCards(deckId)))
                            .flatMap(deckCardRepository::save)
                            .then(deckRepository.updateDeck(deckId, webDeck.getTitle(), webDeck.getDescription(), webDeck.getCharacterId()))
                            .doOnSuccess(aVoid -> {
                                // Clear the cache entries associated with the modified deck
                                Set<String> cacheKeys = deckIdToCacheKeys.remove(deckId);
                                if (cacheKeys != null) {
                                    cacheKeys.forEach(cache::remove);
                                }
                            });
                });
    }

    @Override
    public Mono<Void> deleteDeck(int deckId, String username) {
        return deckRepository.getUserId(deckId)
                .flatMap(userId -> {
                    if (!userId.equals(username)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this deck"));
                    }
                    return deckCardRepository.deleteByDeckId(deckId)
                            .then(deckRepository.deleteById(deckId))
                            .then(Mono.fromRunnable(cache::clear))
                            .then();
                });
    }

    @Override
    public Mono<Void> likeDeck(int deckId, String username) {
        return deckRepository.countLikes(deckId, username)
                .flatMap(count -> {
                    if (count == 0) {
                        return deckRepository.insertLike(deckId, username)
                                .then(deckRepository.incrementLikes(deckId))
                                .doOnSuccess(aVoid -> {
                                    // Clear the cache entries associated with the liked deck
                                    Set<String> cacheKeys = deckIdToCacheKeys.get(deckId);
                                    if (cacheKeys != null) {
                                        cacheKeys.forEach(cache::remove);
                                    }
                                });
                    } else {
                        return Mono.empty();
                    }
                });
    }

    @Override
    public Mono<Void> unlikeDeck(int deckId, String username) {
        return deckRepository.countLikes(deckId, username)
                .flatMap(count -> {
                    if (count == 1) {
                        return deckRepository.removeLike(deckId, username)
                                .then(deckRepository.decrementLikes(deckId))
                                .doOnSuccess(aVoid -> {
                                    // Clear the cache entries associated with the unliked deck
                                    Set<String> cacheKeys = deckIdToCacheKeys.get(deckId);
                                    if (cacheKeys != null) {
                                        cacheKeys.forEach(cache::remove);
                                    }
                                });
                    } else {
                        return Mono.empty();
                    }
                });
    }

    @Override
    public Mono<Boolean> isLiked(int deckId, String username) {
        return deckRepository.countLikes(deckId, username)
                .map(count -> count == 1);
    }

    // Private helper methods
    private Mono<WebDeck> createWebDeck(Deck deck) {
        return getDeckCards(deck)
                .collectList()
                .flatMap(cards -> getUsername(deck.getUserId())
                        .map(username -> createWebDeck(deck, cards, username)));
    }

    private Flux<WebCard> getDeckCards(Deck deck) {
        return deckCardRepository.findByDeckId(deck.getId())
                .flatMap(deckCard -> Flux.range(0, deckCard.getAmount())
                        .flatMap(i -> cardRepository.findWebCardById(deckCard.getCardId()))
                        .map(webCard -> {
                            webCard.setChapter(deckCard.getChapter());
                            return webCard;
                        }));
    }

    private Mono<String> getUsername(String userId) {
        return keycloakService.getUsername(userId);
    }

    private Mono<String> getUserId(String username) {
        return keycloakService.getUserId(username);
    }

    private WebDeck createWebDeck(Deck deck, List<WebCard> cards, String username) {
        WebDeck webDeck = deck.toWebDeck();
        webDeck.setCardList(cards);
        webDeck.setUsername(username);
        return webDeck;
    }

    // Helper class to hold decks and total count
    private static class DecksAndCount {
        Flux<Deck> decks;
        Mono<Long> totalCountMono;

        public DecksAndCount(Flux<Deck> decks, Mono<Long> totalCountMono) {
            this.decks = decks;
            this.totalCountMono = totalCountMono;
        }
    }
}
