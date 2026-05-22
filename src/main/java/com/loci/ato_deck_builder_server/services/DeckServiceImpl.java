package com.loci.ato_deck_builder_server.services;

import com.loci.ato_deck_builder_server.api.deck.objects.PagedWebDeck;
import com.loci.ato_deck_builder_server.api.deck.objects.WebDeck;
import com.loci.ato_deck_builder_server.database.objects.CharacterCard;
import com.loci.ato_deck_builder_server.database.objects.Deck;
import com.loci.ato_deck_builder_server.database.objects.DeckCard;
import com.loci.ato_deck_builder_server.database.objects.WebCard;
import com.loci.ato_deck_builder_server.database.repositories.CardRepository;
import com.loci.ato_deck_builder_server.database.repositories.DeckCardRepository;
import com.loci.ato_deck_builder_server.database.repositories.DeckRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(DeckServiceImpl.class);

    private static final Map<String, Integer> SHARD_COST = Map.of(
            "Common", 60,
            "Uncommon", 180,
            "Rare", 420,
            "Epic", 1260,
            "Mythic", 1940
    );

    private static final Set<String> ALLOWED_DIFFICULTIES = Set.of(
            "ADVENTURER",
            "MADNESS 1", "MADNESS 2", "MADNESS 3", "MADNESS 4", "MADNESS 5"
    );

    private static final int MAX_TAGS = 3;
    private static final int MAX_TAG_LENGTH = 12;

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final DeckCardRepository deckCardRepository;
    private final KeycloakService keycloakService;
    private final CharacterService characterService;

    private final ConcurrentHashMap<String, PagedWebDeck> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Set<String>> deckIdToCacheKeys = new ConcurrentHashMap<>();

    @Autowired
    public DeckServiceImpl(DeckRepository deckRepository, CardRepository cardRepository,
                           DeckCardRepository deckCardRepository, KeycloakService keycloakService,
                           CharacterService characterService) {
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
        this.deckCardRepository = deckCardRepository;
        this.keycloakService = keycloakService;
        this.characterService = characterService;
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
            deckList.forEach(deck -> deckIdToCacheKeys.compute(deck.getId(), (deckId, cacheKeys) -> {
                if (cacheKeys == null) {
                    cacheKeys = ConcurrentHashMap.newKeySet();
                }
                cacheKeys.add(key);
                return cacheKeys;
            }));

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
        String difficulty = normalizeDifficulty(webDeck.getDifficulty());
        String[] tags = normalizeTags(webDeck.getTags());

        return computeShards(webDeck).flatMap(shards -> {
            Mono<Integer> savedDeck = deckRepository.insertDeck(
                    webDeck.getTitle(), webDeck.getDescription(), webDeck.getCharacterId(), username,
                    shards, difficulty, tags);

            return savedDeck.flatMap(id -> {
                List<DeckCard> cards = webDeck.toDeckCards(id);
                return deckCardRepository.saveAll(cards)
                        .then(Mono.fromRunnable(cache::clear))
                        .thenReturn(id);
            });
        });
    }

    @Override
    public Mono<Void> updateDeck(int deckId, WebDeck webDeck, String username) {
        String difficulty = normalizeDifficulty(webDeck.getDifficulty());
        String[] tags = normalizeTags(webDeck.getTags());

        return deckRepository.getUserId(deckId)
                .flatMap(userId -> {
                    if (!userId.equals(username)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this deck"));
                    }
                    return computeShards(webDeck).flatMap(shards ->
                            deckCardRepository.deleteByDeckId(deckId)
                                    .thenMany(Flux.fromIterable(webDeck.toDeckCards(deckId)))
                                    .flatMap(deckCardRepository::save)
                                    .then(deckRepository.updateDeck(deckId, webDeck.getTitle(), webDeck.getDescription(), webDeck.getCharacterId(),
                                            shards, difficulty, tags))
                                    .doOnSuccess(aVoid -> {
                                        Set<String> cacheKeys = deckIdToCacheKeys.remove(deckId);
                                        if (cacheKeys != null) {
                                            cacheKeys.forEach(cache::remove);
                                        }
                                    }));
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

    @Override
    public Mono<Long> backfillShards() {
        logger.info("Starting shards backfill");
        return deckRepository.findAll()
                .concatMap(deck -> getDeckCards(deck)
                        .collectList()
                        .flatMap(cards -> {
                            WebDeck webDeck = deck.toWebDeck();
                            webDeck.setCardList(cards);
                            return computeShards(webDeck).flatMap(newShards -> {
                                Integer oldShards = deck.getShards();
                                if (oldShards != null && oldShards.intValue() == newShards) {
                                    return Mono.just(0);
                                }
                                logger.info("Deck {} shards {} -> {}", deck.getId(), oldShards, newShards);
                                return deckRepository.updateShards(deck.getId(), newShards).thenReturn(1);
                            });
                        }))
                .reduce(0L, (acc, updated) -> acc + updated)
                .doOnSuccess(count -> {
                    cache.clear();
                    logger.info("Shards backfill complete: {} decks updated", count);
                });
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

    private Mono<Integer> computeShards(WebDeck webDeck) {
        List<WebCard> cardList = webDeck.getCardList();
        if (cardList == null || cardList.isEmpty()) return Mono.just(0);

        String characterId = webDeck.getCharacterId();
        if (characterId == null) {
            return Mono.just(computeShards(cardList, List.of()));
        }

        return characterService.getCharacterDeck(characterId)
                .collectList()
                .map(baseCards -> computeShards(cardList, baseCards));
    }

    private int computeShards(List<WebCard> cardList, List<CharacterCard> baseCards) {
        Map<String, Integer> baseCardCounts = new HashMap<>();
        if (baseCards != null) {
            for (CharacterCard baseCard : baseCards) {
                baseCardCounts.merge(baseCard.getId(), 1, Integer::sum);
            }
        }

        int total = 0;
        for (WebCard card : cardList) {
            String version = card.getVersion();
            String baseCardId = ("No\n".equals(version) || version == null)
                    ? card.getId()
                    : card.getId().replaceAll("[ab]$", "");

            int baseCardCount = baseCardCounts.getOrDefault(baseCardId, 0);
            if (baseCardCount > 0) {
                Integer baseCardCost = SHARD_COST.get(card.getRarity());
                if (baseCardCost != null) {
                    total -= baseCardCost;
                }
                baseCardCounts.put(baseCardId, baseCardCount - 1);
            }

            Integer base = SHARD_COST.get(card.getRarity());
            int baseCost = base == null ? 0 : base;

            String originalRarity = card.getOriginalRarity();
            if (originalRarity == null) {
                total += baseCost;
            } else if (originalRarity.equals(card.getRarity())) {
                total += baseCost * 2;
            } else {
                Integer orig = SHARD_COST.get(originalRarity);
                int originalCost = orig == null ? 0 : orig;
                total += originalCost + baseCost;
            }
        }
        return total;
    }

    private String normalizeDifficulty(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        String upper = trimmed.toUpperCase();
        if (!ALLOWED_DIFFICULTIES.contains(upper)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown difficulty: " + raw);
        }
        return upper;
    }

    private String[] normalizeTags(String[] raw) {
        if (raw == null || raw.length == 0) return null;
        List<String> out = new ArrayList<>();
        for (String tag : raw) {
            if (tag == null) continue;
            String trimmed = tag.trim();
            if (trimmed.isEmpty()) continue;
            String upper = trimmed.toUpperCase();
            if (upper.length() > MAX_TAG_LENGTH) {
                upper = upper.substring(0, MAX_TAG_LENGTH);
            }
            out.add(upper);
            if (out.size() >= MAX_TAGS) break;
        }
        return out.isEmpty() ? null : out.toArray(new String[0]);
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
