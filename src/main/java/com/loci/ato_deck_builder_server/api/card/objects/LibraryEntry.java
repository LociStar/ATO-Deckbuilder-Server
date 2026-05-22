package com.loci.ato_deck_builder_server.api.card.objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LibraryEntry {
    private String name;
    private String letter;
    private String category;

    @JsonProperty("class")
    private String cardClass;

    private String type;
    private String target;
    private List<LibraryTier> tiers;
    private List<String> tags;
    private Integer firstChapter;
    private List<String> sources;
}
