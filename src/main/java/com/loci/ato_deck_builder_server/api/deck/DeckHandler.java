package com.loci.ato_deck_builder_server.api.deck;

import com.loci.ato_deck_builder_server.api.deck.objects.WebDeck;
import com.loci.ato_deck_builder_server.database.objects.Deck;
import com.loci.ato_deck_builder_server.database.objects.DeckCard;
import com.loci.ato_deck_builder_server.database.repositories.DeckCardRepository;
import com.loci.ato_deck_builder_server.database.repositories.DeckRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class DeckHandler {
    private final DeckRepository charRepository;
    private final DeckCardRepository deckCardRepository;

    public DeckHandler(DeckRepository charRepository, DeckCardRepository deckCardRepository) {
        this.charRepository = charRepository;
        this.deckCardRepository = deckCardRepository;
    }

    public Mono<ServerResponse> getDecks(ServerRequest request) {
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("10"));
        String searchQuery = "%" + request.queryParam("searchQuery").orElse("") + "%";
        Pageable pageable = PageRequest.of(page, size);
        Flux<Deck> decks = charRepository.findByTitleContaining(searchQuery, pageable.getPageSize(), pageable.getOffset());
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(decks.collectList(), Deck.class);
    }

    public Mono<ServerResponse> uploadDeck(ServerRequest request) {
        return request.bodyToMono(WebDeck.class)
                .flatMap(webDeck -> {
                    Mono<Deck> savedDeck = charRepository.save(webDeck.toDeck());
                    Flux<DeckCard> savedCards = deckCardRepository.saveAll(webDeck.getCards());
                    return Mono.zip(savedDeck, savedCards.collectList());
                })
                .flatMap(tuple -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(tuple.getT1()));
    }
}
