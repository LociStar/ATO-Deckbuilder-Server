package com.loci.ato_deck_builder_server.api.card.objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagedLibrary {
    private List<LibraryEntry> entries;
    private int page;
    private int pages;
    private long total;
    private Map<String, Long> letterCounts;
    private FacetCounts facetCounts;
}
