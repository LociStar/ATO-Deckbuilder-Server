package com.loci.ato_deck_builder_server.database.repositories;

import com.loci.ato_deck_builder_server.database.objects.Perks;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PerksRepository extends R2dbcRepository<Perks, String>  {
    @Query("""
            SELECT * FROM perks
            WHERE title ILIKE $1
            ORDER BY title
            LIMIT $2 OFFSET $3
            """)
    Flux<Perks> findByTitle_likes(String searchQuery, int pageSize, long offset);

    @Query("""
            INSERT INTO perks (title, data, uid)
            VALUES ($1, $2, $3)
            RETURNING id
            """)
    Mono<Integer> insertDeck(String title, String data, String uid);
}
