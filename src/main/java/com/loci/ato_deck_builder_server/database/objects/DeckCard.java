package com.loci.ato_deck_builder_server.database.objects;

import lombok.*;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("deck_card")
public class DeckCard {
    @Column("deck_id")
    private int deckId;
    @Column("card_id")
    private String cardId;
}
