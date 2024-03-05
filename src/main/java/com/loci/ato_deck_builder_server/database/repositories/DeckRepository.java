package com.loci.ato_deck_builder_server.database.repositories;

import com.loci.ato_deck_builder_server.database.objects.Deck;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface DeckRepository extends R2dbcRepository<Deck, String> {
    @Query("""
            SELECT * FROM deck
            WHERE title ILIKE $1
            ORDER BY title
            LIMIT $2 OFFSET $3
            """)
    Flux<Deck> findByTitleContaining(String name, int limit, long offset);
}
