package com.loci.ato_deck_builder_server.services;

import com.loci.ato_deck_builder_server.database.objects.Card;
import com.loci.ato_deck_builder_server.database.objects.WebCard;
import com.loci.ato_deck_builder_server.database.repositories.CardDetailRepository;
import com.loci.ato_deck_builder_server.database.repositories.CardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CardServiceImpl implements CardService {

    private static final Logger logger = LoggerFactory.getLogger(CardServiceImpl.class);

    private final CardRepository cardRepository;
    private final CardDetailRepository cardDetailRepository;
    private final ConcurrentHashMap<String, Flux<DataBuffer>> imageCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Flux<DataBuffer>> spriteCache = new ConcurrentHashMap<>();

    public CardServiceImpl(CardRepository cardRepository, CardDetailRepository cardDetailRepository) {
        this.cardRepository = cardRepository;
        this.cardDetailRepository = cardDetailRepository;
        preloadImages();
    }

    @Override
    public Mono<Iterable<WebCard>> getFilteredCards(int page, int size, String searchQuery, String charClass, String secondaryCharClass) {
        long offset = (long) page * size;
        Flux<WebCard> cards = cardRepository.findByNameContaining(searchQuery, size, offset, charClass, secondaryCharClass);
        return cards.collectList().map(list -> (Iterable<WebCard>) list);
    }

    @Override
    public Mono<Iterable<Card>> getCardsByCardClass(String cardClassId) {
        Flux<Card> cards = cardRepository.findByCardClass(cardClassId);
        return cards.collectList().map(list -> (Iterable<Card>) list);
    }

    @Override
    public Flux<DataBuffer> getCardImage(String cardId) {
        String imagePathEnv = System.getenv("IMAGE_PATH");
        Path imagePath = Paths.get(imagePathEnv, cardId + ".webp");

        return imageCache.computeIfAbsent(cardId, key -> {
            if (!Files.exists(imagePath)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found for card ID: " + cardId);
            }
            return DataBufferUtils.readAsynchronousFileChannel(
                    () -> AsynchronousFileChannel.open(imagePath, StandardOpenOption.READ),
                    new DefaultDataBufferFactory(), 4096);
        });
    }

    @Override
    public Flux<DataBuffer> getCardSprite(String cardId) {
        String imagePathEnv = System.getenv("IMAGE_PATH");

        Mono<String> spriteNameMono = cardDetailRepository.findSpriteById(cardId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Sprite not found for card ID: " + cardId)));

        Mono<Path> imagePathMono = spriteNameMono.map(spriteName -> Paths.get(imagePathEnv + "/sprite", spriteName + ".webp"));

        return imagePathMono.flatMapMany(imagePath -> spriteCache.computeIfAbsent(cardId, key -> {
            if (!Files.exists(imagePath)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sprite image not found for card ID: " + cardId);
            }
            return DataBufferUtils.readAsynchronousFileChannel(
                    () -> AsynchronousFileChannel.open(imagePath, StandardOpenOption.READ),
                    new DefaultDataBufferFactory(), 4096);
        }));
    }

    private void preloadImages() {
        String imagePathEnv = System.getenv("IMAGE_PATH");
        Path dirPath = Paths.get(imagePathEnv);
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

        // Convert the total size to MB and print it
        double totalSizeInMB = totalSize / (1024.0 * 1024.0);
        logger.info("Total CardsImages cache size: {} MB", totalSizeInMB);
    }
}

