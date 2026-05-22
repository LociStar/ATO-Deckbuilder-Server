package com.loci.ato_deck_builder_server.api.card.objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LibraryStats {
    private long total;
    private long tiers;
    private long heroes;
    private long monsters;
    private long items;
    private long boons;
    private long injuries;
    private long specials;
    private String gameVersion;
    private String updatedAt;
    private List<String> knownTypes;
    private List<String> knownClasses;
}
