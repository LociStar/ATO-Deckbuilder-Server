package com.loci.ato_deck_builder_server.api.character;

import com.loci.ato_deck_builder_server.database.objects.CharacterCard;
import com.loci.ato_deck_builder_server.services.CharacterService;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class CharacterHandler {

    private final CharacterService characterService;

    public CharacterHandler(CharacterService characterService) {
        this.characterService = characterService;
    }

    public Mono<ServerResponse> getAllCharacters(ServerRequest ignored) {
        return characterService.getAllCharacters()
                .flatMap(characters -> ServerResponse.ok().bodyValue(characters));
    }

    public Mono<ServerResponse> getCharacterImage(ServerRequest request) {
        String id = request.pathVariable("id");

        return ServerResponse.ok()
                .contentType(MediaType.valueOf("image/webp"))
                .header("Cache-Control", "public, max-age=2592000")
                .body(characterService.getCharacterImage(id), DataBuffer.class);
    }

    public Mono<ServerResponse> getCharacterDeck(ServerRequest serverRequest) {
        String characterId = serverRequest.pathVariable("id");

        return ServerResponse.ok()
                .body(characterService.getCharacterDeck(characterId), CharacterCard.class);
    }
}
