package com.loci.ato_deck_builder_server.database.repositories;

import com.loci.ato_deck_builder_server.api.deck.objects.WebDeck;
import com.loci.ato_deck_builder_server.database.objects.Deck;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DeckRepository extends R2dbcRepository<Deck, Integer> {
    @Query("""
            SELECT * FROM deck
            WHERE title ILIKE $1 AND char_id LIKE $4
            LIMIT $2 OFFSET $3
            """)
    Flux<Deck> findByTitle(String name, int limit, long offset, String charId);

    @Query("""
            INSERT INTO deck (title, description, char_id, user_id)
            VALUES ($1, $2, $3, $4)
            RETURNING deck_id
            """)
    Mono<Integer> insertDeck(String title, String description, String characterId, String userId);

    @Query("""
            SELECT * FROM deck
            WHERE deck_id = $1
            """)
    Mono<WebDeck> findWebDeckById(int id);

    @Query("INSERT INTO deck_likes (deck_id, user_id) VALUES ($1, $2)")
    Mono<Void> insertLike(int id, String userId);

    @Query("UPDATE deck SET likes = likes + 1 WHERE deck_id = $1")
    Mono<Void> incrementLikes(int id);

    @Query("DELETE FROM deck_likes WHERE deck_id = $1 AND user_id = $2")
    Mono<Void> removeLike(int id, String userId);

    @Query("UPDATE deck SET likes = likes - 1 WHERE deck_id = $1")
    Mono<Void> decrementLikes(int id);

    @Query("SELECT COUNT(*) FROM deck_likes WHERE deck_id = $1 AND user_id = $2")
    Mono<Integer> countLikes(int id, String userId);

    @Query("SELECT COUNT(d) FROM Deck d WHERE d.title LIKE $1")
    Mono<Integer> countByTitleContaining(String searchQuery);
}
