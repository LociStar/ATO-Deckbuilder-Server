package com.loci.ato_deck_builder_server.services;

import com.loci.ato_deck_builder_server.database.objects.Character;
import com.loci.ato_deck_builder_server.database.objects.CharacterCard;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface CharacterService {
    Mono<List<Character>> getAllCharacters();

    Flux<DataBuffer> getCharacterImage(String characterId);

    Flux<CharacterCard> getCharacterDeck(String characterId);
}

