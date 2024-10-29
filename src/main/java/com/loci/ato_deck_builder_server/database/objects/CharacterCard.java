package com.loci.ato_deck_builder_server.database.objects;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("character_card")
public class CharacterCard {
    @Id
    @Column("card_id")
    private String id;
    @Column("character_id")
    private String character_id;
    @Column("units_in_deck")
    private int units_in_deck;
    private String name;
    private String card_class;
    private String version;
    private String rarity;
    @Column("originalRarity")
    private String originalRarity;
    @Column("energyCost")
    private int energyCost;
}
