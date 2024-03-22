package com.loci.ato_deck_builder_server.api.character;

import com.loci.ato_deck_builder_server.database.repositories.CharacterRepository;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Component
public class CharacterHandler {
    private final CharacterRepository characterRepository;

    public CharacterHandler(CharacterRepository characterRepository) {
        this.characterRepository = characterRepository;
    }

    public Mono<ServerResponse> getAllCharacters(ServerRequest request) {
        return ServerResponse.ok().body(characterRepository.findAll(), Character.class);
    }

    public Mono<ServerResponse> getCharacterImage(ServerRequest request) {
        String id = request.pathVariable("id");
        String imagePathEnv = System.getenv("IMAGE_PATH");
        Path imagePath = Paths.get(imagePathEnv + "/Chars", id + ".webp");

        Flux<DataBuffer> imageFlux = DataBufferUtils.readAsynchronousFileChannel(
                () -> AsynchronousFileChannel.open(imagePath, StandardOpenOption.READ),
                new DefaultDataBufferFactory(), 4096);

        return ServerResponse.ok().contentType(MediaType.valueOf("image/webp")).body(imageFlux, DataBuffer.class);
    }
}
