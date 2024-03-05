package com.loci.ato_deck_builder_server.api.character;

import com.loci.ato_deck_builder_server.database.repositories.CharacterRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class CharacterHandler {
    private final CharacterRepository characterRepository;

    public CharacterHandler(CharacterRepository characterRepository) {
        this.characterRepository = characterRepository;
    }

    public Mono<ServerResponse> getAllCharacters(ServerRequest request) {
        return ServerResponse.ok().body(characterRepository.findAll(), Character.class);
    }
}
