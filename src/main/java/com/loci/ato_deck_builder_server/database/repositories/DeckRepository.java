package com.loci.ato_deck_builder_server.database.repositories;

import com.loci.ato_deck_builder_server.api.deck.objects.WebDeck;
import com.loci.ato_deck_builder_server.database.objects.Deck;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DeckRepository extends R2dbcRepository<Deck, Integer> {
    @Query("""
            SELECT * FROM deck
            WHERE title ILIKE $1 AND char_id LIKE $4
            ORDER BY likes DESC, title
            LIMIT $2 OFFSET $3
            """)
    Flux<Deck> findByTitle_likes(String name, int limit, long offset, String charId);

    @Query("""
            SELECT * FROM deck
            WHERE title ILIKE $1 AND char_id LIKE $4 AND user_id = $5
            ORDER BY likes DESC, title
            LIMIT $2 OFFSET $3
            """)
    Flux<Deck> findByTitle_likes(String name, int limit, long offset, String charId, String userId);

    @Query("""
            SELECT * FROM deck
            WHERE title ILIKE $1 AND char_id LIKE $4 AND user_id != $5
            ORDER BY likes DESC, title
            LIMIT $2 OFFSET $3
            """)
    Flux<Deck> findByTitle_likes_unowned(String name, int limit, long offset, String charId, String userId);

    @Query("""
            SELECT * FROM deck
            WHERE title ILIKE $1 AND char_id LIKE $4
            ORDER BY title
            LIMIT $2 OFFSET $3
            """)
    Flux<Deck> findByTitle_title(String name, int limit, long offset, String charId);

    @Query("""
            SELECT * FROM deck
            WHERE title ILIKE $1 AND char_id LIKE $4 AND user_id = $5
            ORDER BY title
            LIMIT $2 OFFSET $3
            """)
    Flux<Deck> findByTitle_title(String name, int limit, long offset, String charId, String userId);

    @Query("""
            SELECT * FROM deck
            WHERE title ILIKE $1 AND char_id LIKE $4 AND user_id != $5
            ORDER BY title
            LIMIT $2 OFFSET $3
            """)
    Flux<Deck> findByTitle_title_unowned(String name, int limit, long offset, String charId, String userId);

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

    @Query("""
            UPDATE deck
            SET title = $2, description = $3, char_id = $4
            WHERE deck_id = $1
            """)
    Mono<Void> updateDeck(int deckId, String title, String description, String characterId);

    @Query("""
            SELECT user_id FROM deck
            WHERE deck_id = $1
            """)
    Mono<String> getUserId(int deckId);
}
