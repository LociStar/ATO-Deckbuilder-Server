package com.loci.ato_deck_builder_server.services;

import com.loci.ato_deck_builder_server.database.objects.Description;
import reactor.core.publisher.Mono;

public interface DescriptionService {
    Mono<Description> getDescription(String id);
}
