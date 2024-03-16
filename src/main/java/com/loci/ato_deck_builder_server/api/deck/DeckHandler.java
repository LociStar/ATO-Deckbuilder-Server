package com.loci.ato_deck_builder_server.api.deck;

import com.loci.ato_deck_builder_server.api.deck.objects.WebDeck;
import com.loci.ato_deck_builder_server.database.objects.Card;
import com.loci.ato_deck_builder_server.database.objects.Deck;
import com.loci.ato_deck_builder_server.database.objects.DeckCard;
import com.loci.ato_deck_builder_server.database.objects.WebCard;
import com.loci.ato_deck_builder_server.database.repositories.CardRepository;
import com.loci.ato_deck_builder_server.database.repositories.DeckCardRepository;
import com.loci.ato_deck_builder_server.database.repositories.DeckRepository;
import com.loci.ato_deck_builder_server.services.KeycloakService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Component
public class DeckHandler {
    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final DeckCardRepository deckCardRepository;
    private final KeycloakService keycloakService;

    public DeckHandler(DeckRepository deckRepository, CardRepository cardRepository, DeckCardRepository deckCardRepository) {
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
        this.deckCardRepository = deckCardRepository;
        this.keycloakService = new KeycloakService();
    }

    public Mono<ServerResponse> getDecks(ServerRequest request) {
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("10"));
        String searchQuery = "%" + request.queryParam("searchQuery").orElse("") + "%";
        int upgradeCostReduction = Integer.parseInt(request.queryParam("upgradeCostReduction").orElse("0"));
        int craftingCostReduction = Integer.parseInt(request.queryParam("craftingCostReduction").orElse("0"));
        Pageable pageable = PageRequest.of(page, size);
        Flux<Deck> decks = deckRepository.findByTitleContaining(searchQuery, pageable.getPageSize(), pageable.getOffset());
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(decks.collectList(), Deck.class);
    }

    public Mono<ServerResponse> uploadDeck(ServerRequest request) {
        Mono<Authentication> authenticationMono = ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication);
        return request.bodyToMono(WebDeck.class)
                .flatMap(webDeck -> {
                    Mono<Integer> savedDeck = authenticationMono.flatMap(authentication -> deckRepository.insertDeck(webDeck.getTitle(), webDeck.getDescription(), webDeck.getCharacterId(), authentication.getName()));

                    return savedDeck.map(id -> {
                        List<DeckCard> cards = webDeck.toDeckCards(id);
                        return deckCardRepository.saveAll(cards);
                    });
                }).flatMap(Flux::collectList)
                .then(ServerResponse.ok().build());
    }

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

    private WebDeck createWebDeck(Deck deck, List<WebCard> cards, String username) {
        WebDeck webDeck = deck.toWebDeck();
        webDeck.setCardList(cards);
        webDeck.setUsername(username);
        return webDeck;
    }
}
