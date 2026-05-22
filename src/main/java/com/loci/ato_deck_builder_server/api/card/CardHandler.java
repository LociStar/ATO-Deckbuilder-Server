package com.loci.ato_deck_builder_server.api.card;

import com.loci.ato_deck_builder_server.services.CardService;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;
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

    public Mono<ServerResponse> getLibrary(ServerRequest request) {
        int page = parseInt(request.queryParam("page").orElse("0"), 0);
        int size = clamp(parseInt(request.queryParam("size").orElse("40"), 40), 1, 200);
        String sort = request.queryParam("sort").orElse("name_asc");
        String searchQuery = request.queryParam("searchQuery").orElse("").trim();
        String letter = request.queryParam("letter").orElse("").trim();
        String[] rarities = splitCsv(request.queryParam("rarity").orElse(""));
        String[] classes = splitCsv(request.queryParam("class").orElse(""));
        String[] types = splitCsv(request.queryParam("type").orElse(""));
        String[] categories = splitCsv(request.queryParam("category").orElse(""));
        int costMin = parseInt(request.queryParam("costMin").orElse(""), Integer.MIN_VALUE);
        int costMax = parseInt(request.queryParam("costMax").orElse(""), Integer.MAX_VALUE);

        return cardService.getLibrary(page, size, sort, searchQuery, letter, rarities, classes, types, categories, costMin, costMax)
                .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                        .bodyValue(result))
                .onErrorResume(e -> ServerResponse.status(500).bodyValue(e.getMessage()));
    }

    public Mono<ServerResponse> getLibraryStats(ServerRequest request) {
        return cardService.getLibraryStats()
                .flatMap(stats -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                        .bodyValue(stats))
                .onErrorResume(e -> ServerResponse.status(500).bodyValue(e.getMessage()));
    }

    private static String[] splitCsv(String raw) {
        if (raw == null || raw.isBlank()) return new String[0];
        return java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    private static int parseInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
