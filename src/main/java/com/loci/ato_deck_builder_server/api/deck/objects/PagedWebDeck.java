package com.loci.ato_deck_builder_server.api.deck.objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PagedWebDeck {
    private List<WebDeck> decks;
    private int pages;
}
