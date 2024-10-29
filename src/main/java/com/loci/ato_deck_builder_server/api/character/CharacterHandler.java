package com.loci.ato_deck_builder_server.api.character;

import com.loci.ato_deck_builder_server.database.objects.CharacterCard;
import com.loci.ato_deck_builder_server.database.repositories.CharacterCardRepository;
import com.loci.ato_deck_builder_server.database.repositories.CharacterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CharacterHandler {
    private static final Logger logger = LoggerFactory.getLogger(CharacterHandler.class);
    private final CharacterRepository characterRepository;
    private final CharacterCardRepository characterCardRepository;
    private final ConcurrentHashMap<String, Flux<DataBuffer>> imageCache = new ConcurrentHashMap<>();

    public CharacterHandler(CharacterRepository characterRepository, CharacterCardRepository characterCardRepository) {
        this.characterRepository = characterRepository;
        this.characterCardRepository = characterCardRepository;
        preloadImages();
    }

    public Mono<ServerResponse> getAllCharacters(ServerRequest request) {
        return ServerResponse.ok().body(characterRepository.findAll(), Character.class);
    }

    private void preloadImages() {
        String imagePathEnv = System.getenv("IMAGE_PATH");
        Path dirPath = Paths.get(imagePathEnv + "/Chars");
        long totalSize = 0;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, "*.{webp}")) {
            for (Path imagePath : stream) {
                String id = imagePath.getFileName().toString().replace(".webp", "");

                Flux<DataBuffer> imageFlux = DataBufferUtils.readAsynchronousFileChannel(
                        () -> AsynchronousFileChannel.open(imagePath, StandardOpenOption.READ),
                        new DefaultDataBufferFactory(), 4096);

                imageCache.put(id, imageFlux);

                // Add the size of the file to the total size
                totalSize += Files.size(imagePath);
            }
        } catch (IOException e) {
            logger.error("Error while preloading images", e);
        }

        // Convert the total size to MB and log it
        double totalSizeInMB = totalSize / (1024.0 * 1024.0);
        logger.info("Total CharacterImages cache size: {} MB", totalSizeInMB);
    }

    public Mono<ServerResponse> getCharacterImage(ServerRequest request) {
        String id = request.pathVariable("id");
        String imagePathEnv = System.getenv("IMAGE_PATH");
        Path imagePath = Paths.get(imagePathEnv + "/Chars", id + ".webp");

        Flux<DataBuffer> imageFlux = imageCache.computeIfAbsent(id, key -> DataBufferUtils.readAsynchronousFileChannel(
                () -> AsynchronousFileChannel.open(imagePath, StandardOpenOption.READ),
                new DefaultDataBufferFactory(), 4096));

        return ServerResponse.ok()
                .contentType(MediaType.valueOf("image/webp"))
                .header("Cache-Control", "public, max-age=2592000")
                .body(imageFlux, DataBuffer.class);
    }

    public Mono<ServerResponse> getCharacterDeck(ServerRequest serverRequest) {
        String character_id = serverRequest.pathVariable("id");
        Flux<CharacterCard> cardFlux = characterCardRepository.findCardsByCharacterIdWithoutDuplicates(character_id)
                .flatMap(card -> Flux.just(card).repeat(Math.max(0, card.getUnits_in_deck() - 1)));
        return ServerResponse.ok().body(cardFlux, CharacterCard.class);
    }
}
