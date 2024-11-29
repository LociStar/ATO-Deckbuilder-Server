package com.loci.ato_deck_builder_server.services;

import com.loci.ato_deck_builder_server.api.perk.PagedPerks;
import com.loci.ato_deck_builder_server.api.perk.WebPerk;
import com.loci.ato_deck_builder_server.database.objects.PerkDetails;
import com.loci.ato_deck_builder_server.database.objects.PerkNode;
import com.loci.ato_deck_builder_server.database.objects.Perks;
import com.loci.ato_deck_builder_server.database.repositories.PerkDetailsRepository;
import com.loci.ato_deck_builder_server.database.repositories.PerkNodeRepository;
import com.loci.ato_deck_builder_server.database.repositories.PerksRepository;
import com.loci.ato_deck_builder_server.dto.PerkNodeDTO;
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
public class PerksServiceImpl implements PerksService {

    private static final Logger logger = LoggerFactory.getLogger(PerksServiceImpl.class);
    private final PerksRepository perksRepository;
    private final PerkDetailsRepository perkDetailsRepository;
    private final PerkNodeRepository perkNodeRepository;
    private final ConcurrentHashMap<String, Flux<DataBuffer>> imageCache = new ConcurrentHashMap<>();

    public PerksServiceImpl(PerksRepository perksRepository, PerkDetailsRepository perkDetailsRepository, PerkNodeRepository perkNodeRepository) {
        this.perksRepository = perksRepository;
        this.perkDetailsRepository = perkDetailsRepository;
        this.perkNodeRepository = perkNodeRepository;
        preloadImages();
    }

    @Override
    public Mono<Integer> uploadPerk(WebPerk webPerk, String username) {
        return perksRepository.insertDeck(webPerk.getTitle(), webPerk.getPerks(), username);
    }

    @Override
    public Mono<PagedPerks> getAllPerks(int page, int size, String searchQuery) {
        long offset = (long) page * size;
        Flux<Perks> decks = perksRepository.findByTitle_likes(searchQuery, size, offset);

        return decks.collectList()
                .map(perksList -> {
                    int totalPages = (int) Math.ceil((double) (perksList.size() + 1) / size);
                    return new PagedPerks(perksList, totalPages);
                });
    }

    @Override
    public Mono<Perks> getPerks(String id) {
        return perksRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Perks not found")));
    }

    @Override
    public Flux<DataBuffer> getPerkImage(String id) {
        String imagePathEnv = System.getenv("IMAGE_PATH");
        Path imagePath = Paths.get(imagePathEnv + "/perk", id + ".webp");

        return imageCache.computeIfAbsent(id, key -> {
            if (!Files.exists(imagePath)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found for perk ID: " + id);
            }
            return DataBufferUtils.readAsynchronousFileChannel(
                    () -> AsynchronousFileChannel.open(imagePath, StandardOpenOption.READ),
                    new DefaultDataBufferFactory(), 4096);
        });
    }

    @Override
    public Mono<PerkDetails> getPerkDetails(String id) {
        return perkDetailsRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Perk details not found")));
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

                totalSize += Files.size(imagePath);
            }
        } catch (IOException e) {
            logger.error("Error while preloading images", e);
        }

        double totalSizeInMB = totalSize / (1024.0 * 1024.0);
        logger.info("Total PerkImages cache size: {} MB", totalSizeInMB);
    }

    public Flux<PerkNodeDTO> getPerkNodes() {
        return perkNodeRepository.getAllPerkNodeData();
    }

    public Mono<Void> deletePerk(String id) {
        return perksRepository.deleteById(id);
    }

    public Mono<Void> updatePerk(String id, WebPerk updatedPerk) {
        return perksRepository.updatePerk(id, updatedPerk.getTitle(), updatedPerk.getPerks());
    }
}
