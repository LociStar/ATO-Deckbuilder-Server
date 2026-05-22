package com.loci.ato_deck_builder_server.services;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.loci.ato_deck_builder_server.api.card.objects.FacetCounts;
import com.loci.ato_deck_builder_server.api.card.objects.LibraryEntry;
import com.loci.ato_deck_builder_server.api.card.objects.LibraryStats;
import com.loci.ato_deck_builder_server.api.card.objects.LibraryTier;
import com.loci.ato_deck_builder_server.api.card.objects.PagedLibrary;
import com.loci.ato_deck_builder_server.database.objects.Card;
import com.loci.ato_deck_builder_server.database.objects.LibraryRow;
import com.loci.ato_deck_builder_server.database.objects.NamedCount;
import com.loci.ato_deck_builder_server.database.objects.SourceRow;
import com.loci.ato_deck_builder_server.database.objects.WebCard;
import com.loci.ato_deck_builder_server.database.repositories.CardDetailRepository;
import com.loci.ato_deck_builder_server.database.repositories.CardRepository;
import com.loci.ato_deck_builder_server.database.repositories.LibraryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CardServiceImpl implements CardService {

    private static final Logger logger = LoggerFactory.getLogger(CardServiceImpl.class);

    private static final Map<String, Integer> TIER_ORDER = Map.of(
            "Common", 1, "Uncommon", 2, "Rare", 3, "Epic", 4, "Mythic", 5);

    private static final Map<String, String> CLASS_TO_CATEGORY = Map.of(
            "Warrior", "hero", "Mage", "hero", "Healer", "hero", "Scout", "hero", "MagicKnight", "hero",
            "Monster", "monster", "Item", "item", "Boon", "boon", "Injury", "injury", "Special", "special");

    private final CardRepository cardRepository;
    private final CardDetailRepository cardDetailRepository;
    private final LibraryRepository libraryRepository;
    private final String gameVersion;
    private final ConcurrentHashMap<String, Flux<DataBuffer>> imageCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Flux<DataBuffer>> spriteCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PagedLibrary> libraryCache = new ConcurrentHashMap<>();
    private final AsyncLoadingCache<String, LibraryStats> statsCache;
    private final Mono<Map<String, List<String>>> sourcesMapMono;

    public CardServiceImpl(CardRepository cardRepository, CardDetailRepository cardDetailRepository,
                           LibraryRepository libraryRepository,
                           @Value("${app.game-version:}") String gameVersion) {
        this.cardRepository = cardRepository;
        this.cardDetailRepository = cardDetailRepository;
        this.libraryRepository = libraryRepository;
        this.gameVersion = (gameVersion == null || gameVersion.isBlank()) ? null : gameVersion.trim();
        this.statsCache = Caffeine.newBuilder()
                .maximumSize(4)
                .expireAfterWrite(1, TimeUnit.DAYS)
                .buildAsync((key, executor) -> loadStats().toFuture());
        this.sourcesMapMono = libraryRepository.findStarterDeckSources()
                .collectList()
                .map(this::buildSourcesMap)
                .cache();
        preloadImages();
    }

    private Map<String, List<String>> buildSourcesMap(List<SourceRow> rows) {
        Map<String, TreeSet<String>> grouped = new HashMap<>();
        for (SourceRow row : rows) {
            String key = row.getName() + "\0" + row.getCardClass();
            grouped.computeIfAbsent(key, k -> new TreeSet<>()).add(row.getCharacterId());
        }
        Map<String, List<String>> result = new HashMap<>(grouped.size());
        grouped.forEach((k, v) -> result.put(k, new ArrayList<>(v)));
        return result;
    }

    @Override
    public Mono<Iterable<WebCard>> getFilteredCards(int page, int size, String searchQuery, String charClass, String secondaryCharClass) {
        long offset = (long) page * size;
        Flux<WebCard> cards = cardRepository.findByNameContaining(searchQuery, size, offset, charClass, secondaryCharClass);
        return cards.collectList().map(list -> list);
    }

    @Override
    public Mono<Iterable<Card>> getCardsByCardClass(String cardClassId) {
        Flux<Card> cards = cardRepository.findByCardClass(cardClassId);
        return cards.collectList().map(list -> list);
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

    @Override
    public Mono<PagedLibrary> getLibrary(int page, int size, String sort, String searchQuery, String letter,
                                         String[] rarities, String[] classes, String[] types, String[] categories,
                                         int costMin, int costMax) {
        String cacheKey = String.join("|",
                String.valueOf(page), String.valueOf(size), sort, searchQuery, letter,
                String.join(",", rarities), String.join(",", classes), String.join(",", types), String.join(",", categories),
                String.valueOf(costMin), String.valueOf(costMax));

        PagedLibrary cached = libraryCache.get(cacheKey);
        if (cached != null) return Mono.just(cached);

        long offset = (long) page * size;

        Flux<LibraryRow> rowsFlux = switch (sort) {
            case "name_desc" -> libraryRepository.findLibraryRowsByNameDesc(searchQuery, letter, rarities, classes, types, categories, costMin, costMax, size, offset);
            case "cost_asc" -> libraryRepository.findLibraryRowsByCostAsc(searchQuery, letter, rarities, classes, types, categories, costMin, costMax, size, offset);
            case "cost_desc" -> libraryRepository.findLibraryRowsByCostDesc(searchQuery, letter, rarities, classes, types, categories, costMin, costMax, size, offset);
            default -> libraryRepository.findLibraryRowsByNameAsc(searchQuery, letter, rarities, classes, types, categories, costMin, costMax, size, offset);
        };

        Mono<List<LibraryRow>> rowsMono = rowsFlux.collectList();
        Mono<Long> totalMono = libraryRepository.countLibraryEntries(searchQuery, letter, rarities, classes, types, categories, costMin, costMax);
        Mono<Map<String, Long>> letterCountsMono = libraryRepository.findLetterCounts()
                .collectMap(NamedCount::getKey, NamedCount::getCount);
        Mono<Map<String, Long>> rarityFacetMono = libraryRepository.facetCountsRarity(searchQuery, letter, classes, types, categories, costMin, costMax)
                .collectMap(NamedCount::getKey, NamedCount::getCount);
        Mono<Map<String, Long>> classFacetMono = libraryRepository.facetCountsClass(searchQuery, letter, rarities, types, categories, costMin, costMax)
                .collectMap(NamedCount::getKey, NamedCount::getCount);
        Mono<Map<String, Long>> typeFacetMono = libraryRepository.facetCountsType(searchQuery, letter, rarities, classes, categories, costMin, costMax)
                .collectMap(NamedCount::getKey, NamedCount::getCount);
        Mono<Map<String, Long>> categoryFacetMono = libraryRepository.facetCountsCategory(searchQuery, letter, rarities, classes, types, costMin, costMax)
                .collectMap(NamedCount::getKey, NamedCount::getCount);

        return Mono.zip(rowsMono, totalMono, letterCountsMono, rarityFacetMono, classFacetMono, typeFacetMono, categoryFacetMono, sourcesMapMono)
                .map(tuple -> {
                    List<LibraryEntry> entries = groupRowsIntoEntries(tuple.getT1(), tuple.getT8());
                    long total = tuple.getT2();
                    int pages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
                    FacetCounts facetCounts = new FacetCounts(tuple.getT4(), tuple.getT5(), tuple.getT6(), tuple.getT7());
                    PagedLibrary result = new PagedLibrary(entries, page, pages, total, tuple.getT3(), facetCounts);
                    libraryCache.put(cacheKey, result);
                    return result;
                });
    }

    @Override
    public Mono<LibraryStats> getLibraryStats() {
        return Mono.fromFuture(statsCache.get("current"));
    }

    private Mono<LibraryStats> loadStats() {
        Mono<Map<String, Long>> countsMono = libraryRepository.statsCounts()
                .collectMap(NamedCount::getKey, NamedCount::getCount);
        Mono<List<String>> typesMono = libraryRepository.knownTypes().collectList();
        Mono<List<String>> classesMono = libraryRepository.knownClasses().collectList();

        return Mono.zip(countsMono, typesMono, classesMono).map(tuple -> {
            Map<String, Long> counts = tuple.getT1();
            return new LibraryStats(
                    counts.getOrDefault("total", 0L),
                    counts.getOrDefault("tiers", 0L),
                    counts.getOrDefault("heroes", 0L),
                    counts.getOrDefault("monsters", 0L),
                    counts.getOrDefault("items", 0L),
                    counts.getOrDefault("boons", 0L),
                    counts.getOrDefault("injuries", 0L),
                    counts.getOrDefault("specials", 0L),
                    gameVersion,
                    null,
                    tuple.getT2(),
                    tuple.getT3());
        });
    }

    private List<LibraryEntry> groupRowsIntoEntries(List<LibraryRow> rows, Map<String, List<String>> sourcesMap) {
        Map<String, LibraryEntry> byKey = new LinkedHashMap<>();
        for (LibraryRow row : rows) {
            String entryKey = row.getName() + "\0" + row.getCardClass();
            LibraryEntry entry = byKey.get(entryKey);
            if (entry == null) {
                entry = new LibraryEntry(
                        row.getName(),
                        row.getLetter(),
                        CLASS_TO_CATEGORY.getOrDefault(row.getCardClass(), "special"),
                        row.getCardClass(),
                        row.getType(),
                        composeTarget(row.getTargetSide(), row.getTargetType(), row.getTargetPosition()),
                        new ArrayList<>(),
                        List.of(),
                        null,
                        sourcesMap.getOrDefault(entryKey, List.of()));
                byKey.put(entryKey, entry);
            }
            entry.getTiers().add(new LibraryTier(row.getRarity(), row.getCost(), row.getCardId(), null));
        }
        for (LibraryEntry entry : byKey.values()) {
            entry.getTiers().sort(Comparator.comparingInt(t -> TIER_ORDER.getOrDefault(t.getRarity(), 99)));
        }
        return new ArrayList<>(byKey.values());
    }

    private static final String TARGET_SEP = " · ";

    private String composeTarget(String side, String type, String position) {
        return Arrays.stream(new String[]{side, type, position})
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(TARGET_SEP));
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

                totalSize += Files.size(imagePath);
            }
        } catch (IOException e) {
            logger.error("Error while preloading images", e);
        }

        double totalSizeInMB = totalSize / (1024.0 * 1024.0);
        logger.info("Total CardsImages cache size: {} MB", totalSizeInMB);
    }
}
