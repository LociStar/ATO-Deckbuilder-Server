package com.loci.ato_deck_builder_server.services;

import com.loci.ato_deck_builder_server.api.perk.PagedPerks;
import com.loci.ato_deck_builder_server.api.perk.WebPerk;
import com.loci.ato_deck_builder_server.database.objects.PerkDetails;
import com.loci.ato_deck_builder_server.database.objects.Perks;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PerksService {
    Mono<Integer> uploadPerk(WebPerk webPerk, String username);

    Mono<PagedPerks> getAllPerks(int page, int size, String searchQuery);

    Mono<Perks> getPerks(String id);

    Flux<DataBuffer> getPerkImage(String id);

    Mono<PerkDetails> getPerkDetails(String id);
}
