package com.loci.ato_deck_builder_server.api.deck.objects;

import com.loci.ato_deck_builder_server.database.objects.Deck;
import com.loci.ato_deck_builder_server.database.objects.DeckCard;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class WebDeck extends Deck {
    private List<DeckCard> cards;

    public Deck toDeck() {
        Deck deck = new Deck();
        deck.setId(this.getId());
        deck.setTitle(this.getTitle());
        deck.setDescription(this.getDescription());
        deck.setLikes(this.getLikes());
        deck.setCharacterId(this.getCharacterId());
        deck.setUserId(this.getUserId());
        return deck;
    }
}
