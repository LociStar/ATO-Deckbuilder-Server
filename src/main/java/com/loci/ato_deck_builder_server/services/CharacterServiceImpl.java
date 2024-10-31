package com.loci.ato_deck_builder_server.services;

import com.loci.ato_deck_builder_server.database.objects.Character;
import com.loci.ato_deck_builder_server.database.objects.CharacterCard;
import com.loci.ato_deck_builder_server.database.repositories.CharacterCardRepository;
import com.loci.ato_deck_builder_server.database.repositories.CharacterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CharacterServiceImpl implements CharacterService {
    private static final Logger logger = LoggerFactory.getLogger(CharacterServiceImpl.class);

    private final CharacterRepository characterRepository;
    private final CharacterCardRepository characterCardRepository;
    private final ConcurrentHashMap<String, Flux<DataBuffer>> imageCache = new ConcurrentHashMap<>();

    public CharacterServiceImpl(CharacterRepository characterRepository, CharacterCardRepository characterCardRepository) {
        this.characterRepository = characterRepository;
        this.characterCardRepository = characterCardRepository;
        preloadImages();
    }

    @Override
    public Mono<List<Character>> getAllCharacters() {
        return characterRepository.findAll().collectList();
    }

    @Override
    public Flux<DataBuffer> getCharacterImage(String characterId) {
        String imagePathEnv = System.getenv("IMAGE_PATH");
        Path imagePath = Paths.get(imagePathEnv + "/Chars", characterId + ".webp");

        return imageCache.computeIfAbsent(characterId, key -> DataBufferUtils.readAsynchronousFileChannel(
                () -> {
                    try {
                        return AsynchronousFileChannel.open(imagePath, StandardOpenOption.READ);
                    } catch (IOException e) {
                        logger.error("Error opening image file for character ID: {}", characterId, e);
                        return null;
                    }
                },
                new DefaultDataBufferFactory(), 4096));
    }

    @Override
    public Flux<CharacterCard> getCharacterDeck(String characterId) {
        return characterCardRepository.findCardsByCharacterIdWithoutDuplicates(characterId)
                .flatMap(card -> Flux.just(card).repeat(Math.max(0, card.getUnits_in_deck() - 1)));
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
}

