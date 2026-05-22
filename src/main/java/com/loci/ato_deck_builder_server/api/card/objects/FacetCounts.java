package com.loci.ato_deck_builder_server.api.card.objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacetCounts {
    private Map<String, Long> rarity;

    @JsonProperty("class")
    private Map<String, Long> cardClass;

    private Map<String, Long> type;
    private Map<String, Long> category;
}
