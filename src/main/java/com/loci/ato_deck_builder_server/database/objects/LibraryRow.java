package com.loci.ato_deck_builder_server.database.objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LibraryRow {
    @Column("name")
    private String name;

    @Column("letter")
    private String letter;

    @Column("category")
    private String category;

    @Column("card_class")
    private String cardClass;

    @Column("type")
    private String type;

    @Column("target_side")
    private String targetSide;

    @Column("target_type")
    private String targetType;

    @Column("target_position")
    private String targetPosition;

    @Column("rarity")
    private String rarity;

    @Column("cost")
    private int cost;

    @Column("card_id")
    private String cardId;
}
