package com.loci.ato_deck_builder_server.database.repositories;

import com.loci.ato_deck_builder_server.database.objects.CardDetail;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface CardDetailRepository extends R2dbcRepository<CardDetail, String> {

    @Query("SELECT sprite FROM card_detail WHERE card_id = $1")
    Mono<String> findSpriteById(String id);
}
