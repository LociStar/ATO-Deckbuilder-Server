package com.loci.ato_deck_builder_server.database.repositories;

import com.loci.ato_deck_builder_server.database.objects.DeckCard;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DeckCardRepository extends R2dbcRepository<DeckCard, String> {
    @Query("SELECT * FROM deck_card WHERE deck_id = $1")
    Flux<DeckCard> findByDeckId(int id);

    @Query("DELETE FROM deck_card WHERE deck_id = $1")
    Mono<Void> deleteByDeckId(int deckId);
}
