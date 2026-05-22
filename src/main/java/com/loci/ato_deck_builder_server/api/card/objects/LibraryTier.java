package com.loci.ato_deck_builder_server.api.card.objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LibraryTier {
    private String rarity;
    private int cost;
    private String cardId;
    private String description;
}
