package com.loci.ato_deck_builder_server.api.perk;

import com.loci.ato_deck_builder_server.api.deck.objects.DeckIdResponse;
import com.loci.ato_deck_builder_server.services.PerksService;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
public class PerksHandler {

    private final PerksService perksService;

    public PerksHandler(PerksService perksService) {
        this.perksService = perksService;
    }

    public Mono<ServerResponse> uploadPerk(ServerRequest request) {
        return request.bodyToMono(WebPerk.class)
                .zipWith(ReactiveSecurityContextHolder.getContext().map(SecurityContext::getAuthentication))
                .flatMap(tuple -> {
                    WebPerk webPerk = tuple.getT1();
                    String username = tuple.getT2().getName();
                    return perksService.uploadPerk(webPerk, username)
                            .flatMap(id -> ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(new DeckIdResponse(id)));
                })
                .onErrorResume(this::handleException);
    }

    public Mono<ServerResponse> getAllPerks(ServerRequest request) {
        int page = Integer.parseInt(request.queryParam("page").orElse("0")) - 1;
        int size = Integer.parseInt(request.queryParam("size").orElse("10"));
        String searchQuery = "%" + request.queryParam("searchQuery").orElse("") + "%";

        return perksService.getAllPerks(page, size, searchQuery)
                .flatMap(pagedPerks -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(pagedPerks))
                .onErrorResume(this::handleException);
    }

    public Mono<ServerResponse> getPerks(ServerRequest request) {
        String id = request.pathVariable("id");

        return perksService.getPerks(id)
                .flatMap(perks -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(perks))
                .onErrorResume(this::handleException);
    }

    public Mono<ServerResponse> getPerkImage(ServerRequest serverRequest) {
        String id = serverRequest.pathVariable("id");

        return ServerResponse.ok()
                .contentType(MediaType.valueOf("image/webp"))
                .header("Cache-Control", "public, max-age=2592000")
                .body(perksService.getPerkImage(id), DataBuffer.class)
                .onErrorResume(this::handleException);
    }

    public Mono<ServerResponse> getPerkDetails(ServerRequest serverRequest) {
        String id = serverRequest.pathVariable("id");

        return perksService.getPerkDetails(id)
                .flatMap(perk -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(perk))
                .onErrorResume(this::handleException);
    }

    public Mono<ServerResponse> getAllMainPerks(ServerRequest serverRequest) {
        return perksService.getPerkNodes()
                .collectList()
                .flatMap(perkNodes -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(perkNodes))
                .onErrorResume(this::handleException);
    }

    public Mono<ServerResponse> deletePerk(ServerRequest serverRequest) {
        String id = serverRequest.pathVariable("id");

        return ReactiveSecurityContextHolder.getContext()
                .flatMap(context -> {
                    String username = context.getAuthentication().getName();

                    // First, get the perk details to verify ownership
                    return perksService.getPerks(id)
                            .flatMap(perk -> {
                                if (!perk.getUid().equals(username)) {
                                    // If the current user is not the owner, throw a Forbidden status
                                    return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to delete this perk."));
                                }

                                // If the ownership matches, proceed to delete the perk
                                return perksService.deletePerk(id)
                                        .then(ServerResponse.noContent().build());
                            });
                })
                .onErrorResume(this::handleException); // Handle any errors
    }


    public Mono<ServerResponse> updatePerk(ServerRequest serverRequest) {
        String id = serverRequest.pathVariable("id");

        // First, get the perk details to verify ownership and then update it
        return serverRequest.bodyToMono(WebPerk.class)
                .zipWith(ReactiveSecurityContextHolder.getContext().map(SecurityContext::getAuthentication))
                .flatMap(tuple -> {
                    WebPerk webPerk = tuple.getT1();
                    String username = tuple.getT2().getName();

                    return perksService.getPerks(id)
                            .flatMap(perk -> {
                                if (!perk.getUid().equals(username)) {
                                    // If the current user is not the owner, throw a Forbidden status
                                    return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to update this perk."));
                                }

                                // If the ownership matches, proceed to update the perk
                                return perksService.updatePerk(id, webPerk)
                                        .then(Mono.defer(() -> ServerResponse.ok()
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .bodyValue(new DeckIdResponse(Integer.parseInt(id)))));
                            });
                })
                .onErrorResume(this::handleException); // Handle any errors
    }

    private Mono<ServerResponse> handleException(Throwable e) {
        if (e instanceof ResponseStatusException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return ServerResponse.notFound().build();
            } else {
                return ServerResponse.status(ex.getStatusCode()).bodyValue(Objects.requireNonNull(ex.getReason()));
            }
        }
        return ServerResponse.status(500).bodyValue(e.getMessage());
    }

}
