package com.loci.ato_deck_builder_server.database.objects;

import lombok.*;
import org.springframework.data.relational.core.mapping.Column;

@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class CardWeb extends Card {
    private String rarity;
    @Column("originalRarity")
    private String originalRarity;
}
