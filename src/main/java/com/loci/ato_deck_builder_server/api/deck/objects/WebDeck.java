package com.loci.ato_deck_builder_server.api.deck.objects;

import com.loci.ato_deck_builder_server.database.objects.Card;
import com.loci.ato_deck_builder_server.database.objects.Deck;
import com.loci.ato_deck_builder_server.database.objects.DeckCard;
import lombok.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class WebDeck extends Deck {
    private List<Card> cardList;


    /**
     * Converts the WebDeck to a list of DeckCards
     * @param id - id of the deck
     * @return - list of deck cards
     */
    public List<DeckCard> toDeckCards(int id) {
        Map<String, DeckCard> deckCardMap = new HashMap<>();

        for (Card card : cardList) {
            DeckCard deckCard = deckCardMap.get(card.getId());

            if (deckCard != null) {
                deckCard.setAmount(deckCard.getAmount() + 1);
            } else {
                deckCard = DeckCard.builder().deckId(id).cardId(card.getId()).amount(1).build();
                deckCardMap.put(card.getId(), deckCard);
            }
        }

        return new ArrayList<>(deckCardMap.values());
    }

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
