package com.loci.ato_deck_builder_server.api.description;

import com.loci.ato_deck_builder_server.services.DescriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class DescriptionHandler {

    private final DescriptionService descriptionService;

    @Autowired
    public DescriptionHandler(DescriptionService descriptionService) {
        this.descriptionService = descriptionService;
    }

    public Mono<ServerResponse> getDescription(ServerRequest request) {
        return descriptionService.getDescription(request.pathVariable("id"))
                .flatMap(description -> ServerResponse.ok().bodyValue(description))
                .switchIfEmpty(ServerResponse.notFound().build());
    }
}
