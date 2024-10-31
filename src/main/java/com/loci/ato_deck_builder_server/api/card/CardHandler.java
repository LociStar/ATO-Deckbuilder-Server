package com.loci.ato_deck_builder_server.api.card;

import com.loci.ato_deck_builder_server.services.CardService;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
public class CardHandler {

    private final CardService cardService;

    public CardHandler(CardService cardService) {
        this.cardService = cardService;
    }

    public Mono<ServerResponse> getFilteredCards(ServerRequest request) {
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("10"));
        String searchQuery = "%" + request.queryParam("searchQuery").orElse("") + "%";
        String charClass = "%" + request.queryParam("charClass").orElse("") + "%";
        String secondaryCharClass = request.queryParam("secondaryCharClass").orElse("");

        return cardService.getFilteredCards(page, size, searchQuery, charClass, secondaryCharClass)
                .flatMap(cards -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(cards))
                .onErrorResume(e -> ServerResponse.status(500).bodyValue(e.getMessage()));
    }

    public Mono<ServerResponse> getCardsByCardClass(ServerRequest request) {
        String id = request.pathVariable("id");

        return cardService.getCardsByCardClass(id)
                .flatMap(cards -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(cards))
                .onErrorResume(e -> ServerResponse.status(500).bodyValue(e.getMessage()));
    }

    public Mono<ServerResponse> getCardImageUrl(ServerRequest request) {
        String id = request.pathVariable("id");

        return ServerResponse.ok()
                .contentType(MediaType.valueOf("image/webp"))
                .header("Cache-Control", "public, max-age=259200")
                .body(cardService.getCardImage(id), DataBuffer.class)
                .onErrorResume(e -> {
                    if (e instanceof ResponseStatusException ex) {
                        return ServerResponse.status(ex.getStatusCode()).bodyValue(Objects.requireNonNull(ex.getReason()));
                    }
                    return ServerResponse.status(500).bodyValue(e.getMessage());
                });
    }

    public Mono<ServerResponse> getCardSpriteUrl(ServerRequest request) {
        String id = request.pathVariable("id");

        return ServerResponse.ok()
                .contentType(MediaType.valueOf("image/webp"))
                .header("Cache-Control", "public, max-age=259200")
                .body(cardService.getCardSprite(id), DataBuffer.class)
                .onErrorResume(e -> {
                    if (e instanceof ResponseStatusException ex) {
                        return ServerResponse.status(ex.getStatusCode()).bodyValue(Objects.requireNonNull(ex.getReason()));
                    }
                    return ServerResponse.status(500).bodyValue(e.getMessage());
                });
    }
}
