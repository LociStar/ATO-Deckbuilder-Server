package com.loci.ato_deck_builder_server.database.repositories;

import com.loci.ato_deck_builder_server.database.objects.CharacterCard;
import com.loci.ato_deck_builder_server.database.objects.WebCard;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CharacterCardRepository extends R2dbcRepository<CharacterCard, String> {
    @Query("SELECT * FROM character_card WHERE character_id = $1")
    Flux<CharacterCard> findByCharacterId(String character_id);

    @Query("""
        SELECT
            card.card_id,
            card.name,
            card.card_class AS card_class,
            card.version,
            card_detail.card_rarity AS rarity,
            card_detail.energy_cost AS energyCost,
            original_card_details.card_rarity AS originalRarity,
            character_card.units_in_deck,
            character_card.character_id
        FROM
            character_card
                JOIN card ON character_card.card_id = card.card_id
                JOIN card_detail ON card.card_id = card_detail.card_id
                LEFT JOIN card_detail AS original_card_details ON LOWER(card_detail.upgraded_from) = original_card_details.card_id
        WHERE character_card.character_id = $1
        ORDER BY card.card_id
        """)
    Flux<CharacterCard> findCardsByCharacterIdWithoutDuplicates(String character_id);
}
