package com.loci.ato_deck_builder_server.services;

import com.loci.ato_deck_builder_server.database.objects.Card;
import com.loci.ato_deck_builder_server.database.objects.WebCard;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CardService {
    Mono<Iterable<WebCard>> getFilteredCards(int page, int size, String searchQuery, String charClass, String secondaryCharClass);

    Mono<Iterable<Card>> getCardsByCardClass(String cardClassId);

    Flux<DataBuffer> getCardImage(String cardId);

    Flux<DataBuffer> getCardSprite(String cardId);
}
