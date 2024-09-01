package com.loci.ato_deck_builder_server.api.perk;

import com.loci.ato_deck_builder_server.api.deck.objects.DeckIdResponse;
import com.loci.ato_deck_builder_server.database.objects.Perks;
import com.loci.ato_deck_builder_server.database.repositories.PerksRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
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
public class PerksHandler {

    private final PerksRepository perksRepository;
    private final ConcurrentHashMap<String, Flux<DataBuffer>> imageCache = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(PerksHandler.class);

    public PerksHandler(PerksRepository perksRepository) {
        this.perksRepository = perksRepository;
        preloadImages();
    }

    private void preloadImages() {
        String imagePathEnv = System.getenv("IMAGE_PATH");
        Path dirPath = Paths.get(imagePathEnv + "/perk");
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

    public Mono<ServerResponse> uploadPerk(ServerRequest request) {
        Mono<Authentication> authenticationMono = ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication);
        return request.bodyToMono(WebPerk.class)
                .flatMap(webPerk -> {
                    Mono<Integer> savedWebPerk = authenticationMono.flatMap(authentication -> perksRepository.insertDeck(webPerk.getTitle(), webPerk.getPerks(), authentication.getName()));
                    return savedWebPerk.flatMap(id -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(new DeckIdResponse(id)));
                });
    }

    public Mono<ServerResponse> getAllPerks(ServerRequest request) {
        int page = Integer.parseInt(request.queryParam("page").orElse("0")) - 1;
        int size = Integer.parseInt(request.queryParam("size").orElse("10"));
        Pageable pageable = PageRequest.of(page, size);
        String searchQuery = "%" + request.queryParam("searchQuery").orElse("") + "%";
//        String charId = "%" + request.queryParam("charId").orElse("") + "%";
//        String ownedFilter = request.queryParam("ownedFilter").orElse("all").toLowerCase();
//        String userName = request.queryParam("userName").orElse("");
//        boolean sortByLikesFirst = Boolean.parseBoolean(request.queryParam("sortByLikesFirst").orElse("true"));

//        String key = searchQuery + "-" + charId + "-" + ownedFilter + "-" + userName + "-" + sortByLikesFirst + "-" + page + "-" + size;

        // Get the decks
        Flux<Perks> decks = perksRepository.findByTitle_likes(searchQuery, pageable.getPageSize(), pageable.getOffset());

        // Create a PagedWebDeck object
        Mono<PagedPerks> pagedPerksMono = decks.collectList().map(perksList -> {
            // Calculate the total number of pages
            int totalPages = (int) Math.ceil((double) (perksList.size() + 1) / size);
            return new PagedPerks(perksList, totalPages);
        });

        return pagedPerksMono.flatMap(pagedPerks -> {
            return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(pagedPerks);
        });
    }

    public Mono<ServerResponse> getPerks(ServerRequest request) {
        String id = request.pathVariable("id");
        return perksRepository.findById(id)
                .flatMap(perks -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(perks))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> getPerkImage(ServerRequest serverRequest) {
        String id = serverRequest.pathVariable("id");
        String imagePathEnv = System.getenv("IMAGE_PATH");
        Path imagePath = Paths.get(imagePathEnv + "/perk", id + ".webp");

        Flux<DataBuffer> imageFlux = imageCache.computeIfAbsent(id, key -> DataBufferUtils.readAsynchronousFileChannel(
                () -> AsynchronousFileChannel.open(imagePath, StandardOpenOption.READ),
                new DefaultDataBufferFactory(), 4096));

        return ServerResponse.ok().contentType(MediaType.valueOf("image/webp")).header("Cache-Control", "public, max-age=2592000").body(imageFlux, DataBuffer.class);
    }
}
