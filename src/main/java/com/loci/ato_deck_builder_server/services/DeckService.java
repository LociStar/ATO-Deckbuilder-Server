package com.loci.ato_deck_builder_server.services;

import com.loci.ato_deck_builder_server.api.deck.objects.PagedWebDeck;
import com.loci.ato_deck_builder_server.api.deck.objects.WebDeck;
import reactor.core.publisher.Mono;

public interface DeckService {
    Mono<PagedWebDeck> getDecks(int page, int size, String searchQuery, String charId,
                                String ownedFilter, String userName, boolean sortByLikesFirst);

    Mono<WebDeck> getDeckDetails(int deckId);

    Mono<Integer> uploadDeck(WebDeck webDeck, String username);

    Mono<Void> updateDeck(int deckId, WebDeck webDeck, String username);

    Mono<Void> deleteDeck(int deckId, String username);

    Mono<Void> likeDeck(int deckId, String username);

    Mono<Void> unlikeDeck(int deckId, String username);

    Mono<Boolean> isLiked(int deckId, String username);
}
