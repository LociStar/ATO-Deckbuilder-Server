package com.loci.ato_deck_builder_server.api.character;

import com.loci.ato_deck_builder_server.database.objects.Deck;
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
public class CharHandler {
    private final DeckRepository charRepository;

    public CharHandler(DeckRepository charRepository) {
        this.charRepository = charRepository;
    }

    public Mono<ServerResponse> getCharBuilds(ServerRequest request) {
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("10"));
        String searchQuery = "%" + request.queryParam("searchQuery").orElse("") + "%";
        Pageable pageable = PageRequest.of(page, size);
        Flux<Deck> decks = charRepository.findByTitleContaining(searchQuery, pageable.getPageSize(), pageable.getOffset());
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(decks.collectList(), Deck.class);
    }
}
