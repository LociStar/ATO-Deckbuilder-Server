package com.loci.ato_deck_builder_server.api.card;

import com.loci.ato_deck_builder_server.database.objects.Card;
import com.loci.ato_deck_builder_server.database.repositories.CardDetailRepository;
import com.loci.ato_deck_builder_server.database.repositories.CardRepository;
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
import reactor.util.Logger;
import reactor.util.Loggers;

import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Component
public class CardHandler {
    private final CardRepository cardRepository;
    private final CardDetailRepository cardDetailRepository;
    private static final ResolvableType TYPE = ResolvableType.forClass(String.class);
    private static final Logger log = Loggers.getLogger(CardHandler.class);

    public CardHandler(CardRepository cardRepository, CardDetailRepository cardDetailRepository) {
        this.cardRepository = cardRepository;
        this.cardDetailRepository = cardDetailRepository;
    }

    public Mono<ServerResponse> getAllCards(ServerRequest request) {
        int page = Integer.parseInt(request.queryParam("page").orElse("0"));
        int size = Integer.parseInt(request.queryParam("size").orElse("10"));
        String searchQuery = "%" + request.queryParam("searchQuery").orElse("") + "%";
        Pageable pageable = PageRequest.of(page, size);
        Flux<Card> cards = cardRepository.findByNameContaining(searchQuery, pageable.getPageSize(), pageable.getOffset());
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(cards.collectList(), Card.class);
    }

    public Mono<ServerResponse> getCardsByCardClass(ServerRequest ignored) {
        String id = ignored.pathVariable("id");
        Flux<Card> cards = cardRepository.findByCardClass(id);
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(cards.collectList(), Card.class);
    }

    public Mono<ServerResponse> getCardImageUrl(ServerRequest request) {
        String id = request.pathVariable("id");

        // Use an environment variable
        String imagePathEnv = System.getenv("IMAGE_PATH");
        // Or use a JVM argument
        // String imagePathArg = System.getProperty("imagePathArg");

        Path imagePath = Paths.get(imagePathEnv, id + ".png");

        Flux<DataBuffer> imageFlux = DataBufferUtils.readAsynchronousFileChannel(
                () -> AsynchronousFileChannel.open(imagePath, StandardOpenOption.READ),
                new DefaultDataBufferFactory(), 4096);

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
