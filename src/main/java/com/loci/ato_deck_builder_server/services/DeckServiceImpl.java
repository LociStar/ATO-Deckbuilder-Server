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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

        // Get the decks
        Flux<Deck> decks = sortByLikesFirst ?
                deckRepository.findByTitle_likes(searchQuery, size, (long) page * size, charId) :
                deckRepository.findByTitle_title(searchQuery, size, (long) page * size, charId);

        if (ownedFilter.equals("owned")) {
            return getUserId(userName).flatMapMany(userId -> {
                Flux<Deck> userDecks = sortByLikesFirst ?
                        deckRepository.findByTitle_likes(searchQuery, size, (long) page * size, charId, userId) :
                        deckRepository.findByTitle_title(searchQuery, size, (long) page * size, charId, userId);
                return processDecks(userDecks, key, size);
            }).next();
        } else if (ownedFilter.equals("unowned")) {
            return getUserId(userName).flatMapMany(userId -> {
                Flux<Deck> userDecks = sortByLikesFirst ?
                        deckRepository.findByTitle_likes_unowned(searchQuery, size, (long) page * size, charId, userId) :
                        deckRepository.findByTitle_title_unowned(searchQuery, size, (long) page * size, charId, userId);
                return processDecks(userDecks, key, size);
            }).next();
        } else {
            return processDecks(decks, key, size).next();
        }
    }

    private Flux<PagedWebDeck> processDecks(Flux<Deck> decks, String key, int size) {
        // Convert the decks to WebDecks (Add username)
        Flux<WebDeck> webDecks = decks.flatMapSequential(deck -> {
            Set<String> cacheKeys = deckIdToCacheKeys.getOrDefault(deck.getId(), new HashSet<>());
            cacheKeys.add(key);
            deckIdToCacheKeys.put(deck.getId(), cacheKeys);
            return getUsername(deck)
                    .map(username -> createWebDeck(deck, new ArrayList<>(), username));
        });

        // Create a PagedWebDeck object
        Mono<PagedWebDeck> pagedWebDeckMono = webDecks.collectList().map(webDeckList -> {
            // Calculate the total number of pages
            int totalPages = (int) Math.ceil((double) (webDeckList.size() + 1) / size);
            return new PagedWebDeck(webDeckList, totalPages);
        });

        return pagedWebDeckMono.doOnSuccess(pagedWebDeck -> cache.put(key, pagedWebDeck)).flux();
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
                    // Delete existing cards
                    return deckCardRepository.deleteByDeckId(deckId)
                            .thenMany(Flux.fromIterable(webDeck.toDeckCards(deckId))) // Convert WebCards to DeckCards
                            .flatMap(deckCardRepository::save) // Save new cards
                            .then(deckRepository.updateDeck(deckId, webDeck.getTitle(), webDeck.getDescription(), webDeck.getCharacterId()))
                            .doOnSuccess(aVoid -> {
                                // Clear the cache entries associated with the modified deck
                                Set<String> cacheKeys = deckIdToCacheKeys.get(deckId);
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
                .flatMap(cards -> getUsername(deck)
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

    private Mono<String> getUsername(Deck deck) {
        return keycloakService.getUsername(deck.getUserId());
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
}
