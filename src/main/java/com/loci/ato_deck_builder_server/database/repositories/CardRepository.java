package com.loci.ato_deck_builder_server.database.repositories;

import com.loci.ato_deck_builder_server.database.objects.Card;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CardRepository extends R2dbcRepository<Card, String> {
    @Query("""
            INSERT INTO card (card_id, name, class, version)
            VALUES ($1, $2, $3, $4)
            ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, class = EXCLUDED.class, version = EXCLUDED.version
            RETURNING card_id;""")
    Mono<String> insert(String id, String name, String cardClass, String version);

    @Query("""
            INSERT INTO card (*)
            VALUES (:id, :name, :class, :version)
            ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, class = EXCLUDED.class, version = EXCLUDED.version
            RETURNING card_id;""")
    Mono<String> insert2(Card card);

    @Query("SELECT * FROM card WHERE class = $1")
    Flux<Card> findByCardClass(String id);

    @Query("""
            SELECT * FROM card_details
            ORDER BY card_id
            LIMIT $1 OFFSET $2
            """)
    Flux<Card> findPage(int limit, long offset);

    @Query("""
            SELECT * FROM card
            WHERE name ILIKE $1
            ORDER BY card_id
            LIMIT $2 OFFSET $3
            """)
    Flux<Card> findByNameContaining(String name, int limit, long offset);
}
