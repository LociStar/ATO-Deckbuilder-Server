// DeckHandler.java
package com.loci.ato_deck_builder_server.api.deck;

import com.loci.ato_deck_builder_server.api.deck.objects.DeckIdResponse;
import com.loci.ato_deck_builder_server.api.deck.objects.WebDeck;
import com.loci.ato_deck_builder_server.services.DeckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
public class DeckHandler {

    private final DeckService deckService;

    @Autowired
    public DeckHandler(DeckService deckService) {
        this.deckService = deckService;
    }

    public Mono<ServerResponse> getDecks(ServerRequest request) {
        int page = Integer.parseInt(request.queryParam("page").orElse("0")) - 1;
        int size = Integer.parseInt(request.queryParam("size").orElse("10"));
        String searchQuery = "%" + request.queryParam("searchQuery").orElse("") + "%";
        String charId = "%" + request.queryParam("charId").orElse("") + "%";
        String ownedFilter = request.queryParam("ownedFilter").orElse("all").toLowerCase();
        String userName = request.queryParam("userName").orElse("");
        boolean sortByLikesFirst = Boolean.parseBoolean(request.queryParam("sortByLikesFirst").orElse("true"));

        return deckService.getDecks(page, size, searchQuery, charId, ownedFilter, userName, sortByLikesFirst)
                .flatMap(pagedWebDeck -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(pagedWebDeck))
                .onErrorResume(e -> {
                    if (e instanceof ResponseStatusException ex) {
                        return ServerResponse.status(ex.getStatusCode()).bodyValue(Objects.requireNonNull(ex.getReason()));
                    }
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValue(e.getMessage());
                });
    }

    public Mono<ServerResponse> getDeckDetails(ServerRequest serverRequest) {
        int id = Integer.parseInt(serverRequest.pathVariable("id"));

        return deckService.getDeckDetails(id)
                .flatMap(webDeck -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(webDeck))
                .onErrorResume(e -> {
                    if (e instanceof ResponseStatusException ex) {
                        return ServerResponse.status(ex.getStatusCode()).bodyValue(Objects.requireNonNull(ex.getReason()));
                    }
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValue(e.getMessage());
                });
    }

    public Mono<ServerResponse> uploadDeck(ServerRequest request) {
        Mono<Authentication> authenticationMono = ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication);

        return request.bodyToMono(WebDeck.class)
                .zipWith(authenticationMono)
                .flatMap(tuple -> {
                    WebDeck webDeck = tuple.getT1();
                    String username = tuple.getT2().getName();
                    return deckService.uploadDeck(webDeck, username)
                            .flatMap(deckId -> ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(new DeckIdResponse(deckId)));
                })
                .onErrorResume(e -> {
                    if (e instanceof ResponseStatusException ex) {
                        return ServerResponse.status(ex.getStatusCode()).bodyValue(Objects.requireNonNull(ex.getReason()));
                    }
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValue(e.getMessage());
                });
    }

    public Mono<ServerResponse> updateDeck(ServerRequest request) {
        int deckId = Integer.parseInt(request.pathVariable("id"));
        Mono<Authentication> authenticationMono = ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication);

        return request.bodyToMono(WebDeck.class)
                .zipWith(authenticationMono)
                .flatMap(tuple -> {
                    WebDeck webDeck = tuple.getT1();
                    String username = tuple.getT2().getName();
                    return deckService.updateDeck(deckId, webDeck, username)
                            .then(ServerResponse.ok().build());
                })
                .onErrorResume(e -> {
                    if (e instanceof ResponseStatusException ex) {
                        return ServerResponse.status(ex.getStatusCode()).bodyValue(Objects.requireNonNull(ex.getReason()));
                    }
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValue(e.getMessage());
                });
    }

    public Mono<ServerResponse> deleteDeck(ServerRequest serverRequest) {
        int deckId = Integer.parseInt(serverRequest.pathVariable("id"));
        Mono<Authentication> authenticationMono = ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication);

        return authenticationMono.flatMap(authentication -> {
                    String username = authentication.getName();
                    return deckService.deleteDeck(deckId, username)
                            .then(ServerResponse.ok().build());
                })
                .onErrorResume(e -> {
                    if (e instanceof ResponseStatusException ex) {
                        return ServerResponse.status(ex.getStatusCode()).bodyValue(Objects.requireNonNull(ex.getReason()));
                    }
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValue(e.getMessage());
                });
    }

    public Mono<ServerResponse> likeDeck(ServerRequest serverRequest) {
        Mono<Authentication> authenticationMono = ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication);
        int deckId = Integer.parseInt(serverRequest.pathVariable("id"));

        return authenticationMono.flatMap(authentication -> {
                    String username = authentication.getName();
                    return deckService.likeDeck(deckId, username)
                            .then(ServerResponse.ok().build());
                })
                .onErrorResume(e -> {
                    if (e instanceof ResponseStatusException ex) {
                        return ServerResponse.status(ex.getStatusCode()).bodyValue(Objects.requireNonNull(ex.getReason()));
                    }
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValue(e.getMessage());
                });
    }

    public Mono<ServerResponse> unlikeDeck(ServerRequest serverRequest) {
        Mono<Authentication> authenticationMono = ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication);
        int deckId = Integer.parseInt(serverRequest.pathVariable("id"));

        return authenticationMono.flatMap(authentication -> {
                    String username = authentication.getName();
                    return deckService.unlikeDeck(deckId, username)
                            .then(ServerResponse.ok().build());
                })
                .onErrorResume(e -> {
                    if (e instanceof ResponseStatusException ex) {
                        return ServerResponse.status(ex.getStatusCode()).bodyValue(Objects.requireNonNull(ex.getReason()));
                    }
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValue(e.getMessage());
                });
    }

    public Mono<ServerResponse> isLiked(ServerRequest serverRequest) {
        Mono<Authentication> authenticationMono = ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication);
        int deckId = Integer.parseInt(serverRequest.pathVariable("id"));

        return authenticationMono.flatMap(authentication -> {
                    String username = authentication.getName();
                    return deckService.isLiked(deckId, username)
                            .flatMap(isLiked -> ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(isLiked));
                })
                .onErrorResume(e -> {
                    if (e instanceof ResponseStatusException ex) {
                        return ServerResponse.status(ex.getStatusCode()).bodyValue(Objects.requireNonNull(ex.getReason()));
                    }
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValue(e.getMessage());
                });
    }
}
