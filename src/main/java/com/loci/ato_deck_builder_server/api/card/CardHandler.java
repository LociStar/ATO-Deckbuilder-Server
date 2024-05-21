package com.loci.ato_deck_builder_server.api.card;

import com.loci.ato_deck_builder_server.database.objects.Card;
import com.loci.ato_deck_builder_server.database.objects.WebCard;
import com.loci.ato_deck_builder_server.database.repositories.CardDetailRepository;
import com.loci.ato_deck_builder_server.database.repositories.CardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
public class CardHandler {
    private static final ResolvableType TYPE = ResolvableType.forClass(String.class);
    private static final Logger logger = LoggerFactory.getLogger(CardHandler.class);
    private final CardRepository cardRepository;
    private final CardDetailRepository cardDetailRepository;
    private final ConcurrentHashMap<String, Flux<DataBuffer>> imageCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Flux<DataBuffer>> spriteCache = new ConcurrentHashMap<>();

    public CardHandler(CardRepository cardRepository, CardDetailRepository cardDetailRepository) {
        this.cardRepository = cardRepository;
        this.cardDetailRepository = cardDetailRepository;
        preloadImages();
    }

    public Mono<ServerResponse> getFilteredCards(ServerRequest request) {
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("10"));
        String searchQuery = "%" + request.queryParam("searchQuery").orElse("") + "%";
        String charClass = "%" + request.queryParam("charClass").orElse("") + "%";
        String secondaryCharClass = request.queryParam("secondaryCharClass").orElse("");
        Pageable pageable = PageRequest.of(page, size);
        Flux<WebCard> cards = cardRepository.findByNameContaining(searchQuery, pageable.getPageSize(), pageable.getOffset(), charClass, secondaryCharClass);
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(cards.collectList(), Card.class);
    }

    public Mono<ServerResponse> getCardsByCardClass(ServerRequest ignored) {
        String id = ignored.pathVariable("id");
        Flux<Card> cards = cardRepository.findByCardClass(id);
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(cards.collectList(), Card.class);
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

    public Mono<ServerResponse> getCardImageUrl(ServerRequest request) {
        String id = request.pathVariable("id");
        String imagePathEnv = System.getenv("IMAGE_PATH");
        Path imagePath = Paths.get(imagePathEnv, id + ".webp");

        Flux<DataBuffer> imageFlux = imageCache.computeIfAbsent(id, key -> DataBufferUtils.readAsynchronousFileChannel(
                () -> AsynchronousFileChannel.open(imagePath, StandardOpenOption.READ),
                new DefaultDataBufferFactory(), 4096));

        return ServerResponse.ok().contentType(MediaType.IMAGE_PNG).body(imageFlux, DataBuffer.class);
    }

    public Mono<ServerResponse> getCardSpriteUrl(ServerRequest request) {
        String id = request.pathVariable("id");
        String imagePathEnv = System.getenv("IMAGE_PATH");
        Mono<String> spriteNameMono = cardDetailRepository.findSpriteById(id);
        Mono<Path> imagePathMono = spriteNameMono.map(spriteName -> Paths.get(imagePathEnv + "/sprite", spriteName + ".webp"));

        Flux<DataBuffer> imageFlux = imagePathMono.flatMapMany(imagePath -> spriteCache.computeIfAbsent(id, key -> DataBufferUtils.readAsynchronousFileChannel(
                () -> AsynchronousFileChannel.open(imagePath, StandardOpenOption.READ),
                new DefaultDataBufferFactory(), 4096)));

        return ServerResponse.ok().contentType(MediaType.IMAGE_PNG).body(imageFlux, DataBuffer.class);
    }

    public Mono<ServerResponse> getCardDetails(ServerRequest serverRequest) {
        String id = serverRequest.pathVariable("id");
        return cardDetailRepository.findById(id).flatMap(cardDetails -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(cardDetails)).switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> parseData(ServerRequest ignored) {

        StringDecoder stringDecoder = StringDecoder.textPlainOnly();

        return DataBufferUtils.readAsynchronousFileChannel(() -> AsynchronousFileChannel.open(Path.of("src/main/resources/cardlist_txt.csv"), StandardOpenOption.READ), DefaultDataBufferFactory.sharedInstance, 4096).transform(dataBufferFlux -> stringDecoder.decode(dataBufferFlux, TYPE, null, null)).flatMap(s -> {
            String[] split = s.split("\t");
            if (split[0].equals("id")) {
                return Mono.empty();
            }
            return cardRepository.insert(split[0], split[1], split[2], split[3]);
        }).then(ServerResponse.ok().build());
    }

}
