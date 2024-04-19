package com.loci.ato_deck_builder_server.api.deck;

import com.loci.ato_deck_builder_server.api.deck.objects.PagedWebDeck;
import com.loci.ato_deck_builder_server.api.deck.objects.WebDeck;
import com.loci.ato_deck_builder_server.database.objects.Deck;
import com.loci.ato_deck_builder_server.database.objects.DeckCard;
import com.loci.ato_deck_builder_server.database.objects.WebCard;
import com.loci.ato_deck_builder_server.database.repositories.CardRepository;
import com.loci.ato_deck_builder_server.database.repositories.DeckCardRepository;
import com.loci.ato_deck_builder_server.database.repositories.DeckRepository;
import com.loci.ato_deck_builder_server.services.KeycloakService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DeckHandler is a component that handles all the operations related to Decks.
 */
@Component
public class DeckHandler {
    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final DeckCardRepository deckCardRepository;
    private final KeycloakService keycloakService;
    private final ConcurrentHashMap<String, PagedWebDeck> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Set<String>> deckIdToCacheKeys = new ConcurrentHashMap<>();

    /**
     * Constructor for DeckHandler.
     *
     * @param deckRepository     Repository for Deck operations.
     * @param cardRepository     Repository for Card operations.
     * @param deckCardRepository Repository for DeckCard operations.
     */
    public DeckHandler(DeckRepository deckRepository, CardRepository cardRepository, DeckCardRepository deckCardRepository) {
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
        this.deckCardRepository = deckCardRepository;
        this.keycloakService = new KeycloakService();
    }

    /**
     * Handles the GET request to fetch Decks.
     *
     * @param request The incoming ServerRequest.
     * @return A ServerResponse containing the requested Decks.
     */
    public Mono<ServerResponse> getDecks(ServerRequest request) {
        int page = Integer.parseInt(request.queryParam("page").orElse("0")) - 1;
        int size = Integer.parseInt(request.queryParam("size").orElse("10"));
        Pageable pageable = PageRequest.of(page, size);
        String searchQuery = "%" + request.queryParam("searchQuery").orElse("") + "%";
        String charId = "%" + request.queryParam("charId").orElse("") + "%";
        String ownedFilter = request.queryParam("ownedFilter").orElse("all").toLowerCase();
        String userName = request.queryParam("userName").orElse("");
        boolean sortByLikesFirst = Boolean.parseBoolean(request.queryParam("sortByLikesFirst").orElse("true"));

        String key = searchQuery + "-" + charId + "-" + ownedFilter + "-" + userName + "-" + sortByLikesFirst + "-" + page + "-" + size;

        // Check if the PagedWebDeck is in the cache
        PagedWebDeck cachedPagedWebDeck = cache.get(key);
        if (cachedPagedWebDeck != null) {
            return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(cachedPagedWebDeck);
        }

        // Get the decks
        Flux<Deck> decks = sortByLikesFirst ?
                deckRepository.findByTitle_likes(searchQuery, pageable.getPageSize(), pageable.getOffset(), charId) :
                deckRepository.findByTitle_title(searchQuery, pageable.getPageSize(), pageable.getOffset(), charId);
        if (ownedFilter.equals("owned")) {
            decks = getUserId(userName).flatMapMany(userId -> sortByLikesFirst ?
                    deckRepository.findByTitle_likes(searchQuery, pageable.getPageSize(), pageable.getOffset(), charId, userId) :
                    deckRepository.findByTitle_title(searchQuery, pageable.getPageSize(), pageable.getOffset(), charId, userId));
        } else if (ownedFilter.equals("unowned")) {
            decks = getUserId(userName).flatMapMany(userId -> sortByLikesFirst ?
                    deckRepository.findByTitle_likes_unowned(searchQuery, pageable.getPageSize(), pageable.getOffset(), charId, userId) :
                    deckRepository.findByTitle_title_unowned(searchQuery, pageable.getPageSize(), pageable.getOffset(), charId, userId));
        }

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

        return pagedWebDeckMono.flatMap(pagedWebDeck -> {
            cache.put(key, pagedWebDeck);
            return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(pagedWebDeck);
        });
    }

    /**
     * Handles the POST request to upload a Deck.
     *
     * @param request The incoming ServerRequest.
     * @return A ServerResponse indicating the result of the operation.
     */
    public Mono<ServerResponse> uploadDeck(ServerRequest request) {
        Mono<Authentication> authenticationMono = ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication);
        return request.bodyToMono(WebDeck.class)
                .flatMap(webDeck -> {
                    Mono<Integer> savedDeck = authenticationMono.flatMap(authentication -> deckRepository.insertDeck(webDeck.getTitle(), webDeck.getDescription(), webDeck.getCharacterId(), authentication.getName()));

                    return savedDeck.flatMap(id -> {
                        List<DeckCard> cards = webDeck.toDeckCards(id);
                        return deckCardRepository.saveAll(cards).then(ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(id))
                                .then(Mono.fromRunnable(cache::clear));
                    });
                });
    }

    public Mono<ServerResponse> updateDeck(ServerRequest request) {
        int deckId = Integer.parseInt(request.pathVariable("id"));
        return request.bodyToMono(WebDeck.class)
                .zipWith(ReactiveSecurityContextHolder.getContext().map(SecurityContext::getAuthentication))
                .flatMap(tuple -> {
                    WebDeck webDeck = tuple.getT1();
                    Authentication authentication = tuple.getT2();
                    return deckRepository.getUserId(deckId)
                            .flatMap(userId -> {
                                System.out.println(userId);
                                if (!userId.equals(authentication.getName())) {
                                    return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this deck"));
                                }
                                // Delete existing cards
                                return deckCardRepository.deleteByDeckId(deckId)
                                        .thenMany(Flux.fromIterable(webDeck.toDeckCards(deckId))) // Convert WebCards to DeckCards
                                        .flatMap(deckCardRepository::save) // Save new cards
                                        .then(deckRepository.updateDeck(deckId, webDeck.getTitle(), webDeck.getDescription(), webDeck.getCharacterId()))
                                        .then(ServerResponse.ok().build());
                            });
                })
                .onErrorResume(ResponseStatusException.class, e -> ServerResponse.status(e.getStatusCode()).bodyValue(e.getMessage()))
                .doOnSuccess(response -> {
                    // Clear the cache entries associated with the modified deck
                    Set<String> cacheKeys = deckIdToCacheKeys.get(deckId);
                    if (cacheKeys != null) {
                        cacheKeys.forEach(cache::remove);
                    }
                });
    }

    /**
     * Handles the GET request to fetch the details of a specific Deck.
     *
     * @param serverRequest The incoming ServerRequest.
     * @return A ServerResponse containing the requested Deck details.
     */
    public Mono<ServerResponse> getDeckDetails(ServerRequest serverRequest) {
        int id = Integer.parseInt(serverRequest.pathVariable("id"));
        Mono<Deck> deckMono = deckRepository.findById(id);

        return deckMono.flatMap(this::createWebDeck)
                .flatMap(webDeck -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(webDeck));
    }


    private Mono<WebDeck> createWebDeck(Deck deck) {
        return getDeckCards(deck)
                .collectList()
                .flatMap(cards -> getUsername(deck)
                        .map(username -> createWebDeck(deck, cards, username)));
    }

    private Flux<WebCard> getDeckCards(Deck deck) {
        return deckCardRepository.findByDeckId(deck.getId())
                .flatMap(deckCard -> Flux.range(0, deckCard.getAmount())
                        .flatMap(i -> cardRepository.findWebCardById(deckCard.getCardId())));
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

    /**
     * Handles the POST request to like a Deck.
     *
     * @param serverRequest The incoming ServerRequest.
     * @return A ServerResponse indicating the result of the operation.
     */
    public Mono<ServerResponse> likeDeck(ServerRequest serverRequest) {
        Mono<Authentication> authenticationMono = ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication);
        return authenticationMono.flatMap(authentication -> {
            int id = Integer.parseInt(serverRequest.pathVariable("id"));
            return deckRepository.countLikes(id, authentication.getName())
                    .flatMap(count -> {
                        if (count == 0) {
                            return deckRepository.insertLike(id, authentication.getName())
                                    .then(deckRepository.incrementLikes(id))
                                    .then(ServerResponse.ok().build());
                        } else {
                            return ServerResponse.ok().build();
                        }
                    });
        });
    }

    /**
     * Handles the POST request to unlike a Deck.
     *
     * @param serverRequest The incoming ServerRequest.
     * @return A ServerResponse indicating the result of the operation.
     */
    public Mono<ServerResponse> unlikeDeck(ServerRequest serverRequest) {
        Mono<Authentication> authenticationMono = ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication);
        return authenticationMono.flatMap(authentication -> {
            int id = Integer.parseInt(serverRequest.pathVariable("id"));
            return deckRepository.countLikes(id, authentication.getName())
                    .flatMap(count -> {
                        if (count == 1) {
                            return deckRepository.removeLike(id, authentication.getName())
                                    .then(deckRepository.decrementLikes(id))
                                    .then(ServerResponse.ok().build());
                        } else {
                            return ServerResponse.ok().build();
                        }
                    });
        });
    }

    /**
     * Handles the GET request to check if a Deck is liked by the current user.
     *
     * @param serverRequest The incoming ServerRequest.
     * @return A ServerResponse indicating whether the Deck is liked.
     */
    public Mono<ServerResponse> isLiked(ServerRequest serverRequest) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(authentication -> {
                    int id = Integer.parseInt(serverRequest.pathVariable("id"));
                    return deckRepository.countLikes(id, authentication.getName())
                            .flatMap(count -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(count == 1));
                });
    }

    public Mono<ServerResponse> deleteDeck(ServerRequest serverRequest) {
        return deckRepository.getUserId(Integer.parseInt(serverRequest.pathVariable("id")))
                .zipWith(ReactiveSecurityContextHolder.getContext().map(SecurityContext::getAuthentication))
                .flatMap(tuple -> {
                    String userId = tuple.getT1();
                    String username = tuple.getT2().getName();
                    if (!userId.equals(username)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this deck"));
                    }
                    return deckCardRepository.deleteByDeckId(Integer.parseInt(serverRequest.pathVariable("id")))
                            .then(deckRepository.deleteById(Integer.parseInt(serverRequest.pathVariable("id")))
                                    .then(ServerResponse.ok().build()));
                })
                .onErrorResume(ResponseStatusException.class, e -> ServerResponse.status(e.getStatusCode()).bodyValue(e.getMessage()));
    }
}
