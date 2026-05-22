package com.loci.ato_deck_builder_server.database.repositories;

import com.loci.ato_deck_builder_server.database.objects.Card;
import com.loci.ato_deck_builder_server.database.objects.LibraryRow;
import com.loci.ato_deck_builder_server.database.objects.NamedCount;
import com.loci.ato_deck_builder_server.database.objects.SourceRow;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LibraryRepository extends R2dbcRepository<Card, String> {

    String CATEGORY_CASE =
            "CASE c.card_class " +
            "WHEN 'Warrior' THEN 'hero' " +
            "WHEN 'Mage' THEN 'hero' " +
            "WHEN 'Healer' THEN 'hero' " +
            "WHEN 'Scout' THEN 'hero' " +
            "WHEN 'MagicKnight' THEN 'hero' " +
            "WHEN 'Monster' THEN 'monster' " +
            "WHEN 'Item' THEN 'item' " +
            "WHEN 'Boon' THEN 'boon' " +
            "WHEN 'Injury' THEN 'injury' " +
            "WHEN 'Special' THEN 'special' " +
            "ELSE 'special' END";

    String RANKED_CTE =
            "WITH ranked AS ( " +
            "  SELECT " +
            "    c.name, " +
            "    UPPER(LEFT(c.name, 1)) AS letter, " +
            "    c.card_class, " +
            "    " + CATEGORY_CASE + " AS category, " +
            "    cd.card_type AS type, " +
            "    cd.target_side, " +
            "    cd.target_type, " +
            "    cd.target_position, " +
            "    cd.card_rarity AS rarity, " +
            "    cd.energy_cost AS cost, " +
            "    c.card_id, " +
            "    ROW_NUMBER() OVER ( " +
            "      PARTITION BY c.name, c.card_class, cd.card_rarity " +
            "      ORDER BY " +
            "        CASE WHEN COALESCE(NULLIF(TRIM(c.version), ''), 'No') = 'No' THEN 0 ELSE 1 END, " +
            "        c.card_id " +
            "    ) AS rn " +
            "  FROM card c " +
            "  JOIN card_detail cd ON cd.card_id = c.card_id " +
            "  WHERE " +
            "    c.name ILIKE '%' || :searchQuery || '%' " +
            "    AND (:letter = '' OR UPPER(LEFT(c.name, 1)) = UPPER(:letter)) " +
            "    AND (cardinality(:rarities) = 0 OR cd.card_rarity = ANY(:rarities)) " +
            "    AND (cardinality(:classes) = 0 OR c.card_class = ANY(:classes)) " +
            "    AND (cardinality(:types) = 0 OR cd.card_type = ANY(:types)) " +
            "    AND (cardinality(:categories) = 0 OR (" + CATEGORY_CASE + ") = ANY(:categories)) " +
            "    AND cd.energy_cost >= :costMin " +
            "    AND cd.energy_cost <= :costMax " +
            "), " +
            "filtered AS (SELECT * FROM ranked WHERE rn = 1) ";

    String TIER_ORDER =
            "CASE f.rarity " +
            "WHEN 'Common' THEN 1 " +
            "WHEN 'Uncommon' THEN 2 " +
            "WHEN 'Rare' THEN 3 " +
            "WHEN 'Epic' THEN 4 " +
            "WHEN 'Mythic' THEN 5 " +
            "ELSE 6 END";

    String SELECT_ROWS =
            "SELECT f.name, f.letter, f.category, f.card_class, f.type, " +
            "       f.target_side, f.target_type, f.target_position, " +
            "       f.rarity, f.cost, f.card_id " +
            "FROM filtered f " +
            "JOIN paged p ON p.name = f.name AND p.card_class = f.card_class ";

    @Query(RANKED_CTE +
            ", paged AS ( " +
            "  SELECT name, card_class FROM filtered " +
            "  GROUP BY name, card_class " +
            "  ORDER BY name ASC, card_class ASC " +
            "  LIMIT :size OFFSET :offset " +
            ") " +
            SELECT_ROWS +
            "ORDER BY f.name ASC, f.card_class ASC, " + TIER_ORDER)
    Flux<LibraryRow> findLibraryRowsByNameAsc(String searchQuery, String letter,
                                              String[] rarities, String[] classes, String[] types, String[] categories,
                                              int costMin, int costMax, int size, long offset);

    @Query(RANKED_CTE +
            ", paged AS ( " +
            "  SELECT name, card_class FROM filtered " +
            "  GROUP BY name, card_class " +
            "  ORDER BY name DESC, card_class ASC " +
            "  LIMIT :size OFFSET :offset " +
            ") " +
            SELECT_ROWS +
            "ORDER BY f.name DESC, f.card_class ASC, " + TIER_ORDER)
    Flux<LibraryRow> findLibraryRowsByNameDesc(String searchQuery, String letter,
                                               String[] rarities, String[] classes, String[] types, String[] categories,
                                               int costMin, int costMax, int size, long offset);

    @Query(RANKED_CTE +
            ", paged AS ( " +
            "  SELECT name, card_class, MIN(cost) AS sort_cost FROM filtered " +
            "  GROUP BY name, card_class " +
            "  ORDER BY sort_cost ASC, name ASC, card_class ASC " +
            "  LIMIT :size OFFSET :offset " +
            ") " +
            SELECT_ROWS +
            "ORDER BY p.sort_cost ASC, f.name ASC, f.card_class ASC, " + TIER_ORDER)
    Flux<LibraryRow> findLibraryRowsByCostAsc(String searchQuery, String letter,
                                              String[] rarities, String[] classes, String[] types, String[] categories,
                                              int costMin, int costMax, int size, long offset);

    @Query(RANKED_CTE +
            ", paged AS ( " +
            "  SELECT name, card_class, MAX(cost) AS sort_cost FROM filtered " +
            "  GROUP BY name, card_class " +
            "  ORDER BY sort_cost DESC, name ASC, card_class ASC " +
            "  LIMIT :size OFFSET :offset " +
            ") " +
            SELECT_ROWS +
            "ORDER BY p.sort_cost DESC, f.name ASC, f.card_class ASC, " + TIER_ORDER)
    Flux<LibraryRow> findLibraryRowsByCostDesc(String searchQuery, String letter,
                                               String[] rarities, String[] classes, String[] types, String[] categories,
                                               int costMin, int costMax, int size, long offset);

    @Query(RANKED_CTE +
            "SELECT COUNT(*) FROM (SELECT name, card_class FROM filtered GROUP BY name, card_class) sub")
    Mono<Long> countLibraryEntries(String searchQuery, String letter,
                                   String[] rarities, String[] classes, String[] types, String[] categories,
                                   int costMin, int costMax);

    @Query("SELECT UPPER(LEFT(name, 1)) AS k, COUNT(DISTINCT (name, card_class)) AS n " +
            "FROM card GROUP BY UPPER(LEFT(name, 1)) ORDER BY 1")
    Flux<NamedCount> findLetterCounts();

    @Query("SELECT cd.card_rarity AS k, COUNT(DISTINCT (c.name, c.card_class)) AS n " +
            "FROM card c JOIN card_detail cd ON cd.card_id = c.card_id " +
            "WHERE c.name ILIKE '%' || :searchQuery || '%' " +
            "  AND (:letter = '' OR UPPER(LEFT(c.name, 1)) = UPPER(:letter)) " +
            "  AND (cardinality(:classes) = 0 OR c.card_class = ANY(:classes)) " +
            "  AND (cardinality(:types) = 0 OR cd.card_type = ANY(:types)) " +
            "  AND (cardinality(:categories) = 0 OR (" + CATEGORY_CASE + ") = ANY(:categories)) " +
            "  AND cd.energy_cost >= :costMin " +
            "  AND cd.energy_cost <= :costMax " +
            "GROUP BY cd.card_rarity")
    Flux<NamedCount> facetCountsRarity(String searchQuery, String letter,
                                       String[] classes, String[] types, String[] categories,
                                       int costMin, int costMax);

    @Query("SELECT c.card_class AS k, COUNT(DISTINCT (c.name, c.card_class)) AS n " +
            "FROM card c JOIN card_detail cd ON cd.card_id = c.card_id " +
            "WHERE c.name ILIKE '%' || :searchQuery || '%' " +
            "  AND (:letter = '' OR UPPER(LEFT(c.name, 1)) = UPPER(:letter)) " +
            "  AND (cardinality(:rarities) = 0 OR cd.card_rarity = ANY(:rarities)) " +
            "  AND (cardinality(:types) = 0 OR cd.card_type = ANY(:types)) " +
            "  AND (cardinality(:categories) = 0 OR (" + CATEGORY_CASE + ") = ANY(:categories)) " +
            "  AND cd.energy_cost >= :costMin " +
            "  AND cd.energy_cost <= :costMax " +
            "GROUP BY c.card_class")
    Flux<NamedCount> facetCountsClass(String searchQuery, String letter,
                                      String[] rarities, String[] types, String[] categories,
                                      int costMin, int costMax);

    @Query("SELECT cd.card_type AS k, COUNT(DISTINCT (c.name, c.card_class)) AS n " +
            "FROM card c JOIN card_detail cd ON cd.card_id = c.card_id " +
            "WHERE c.name ILIKE '%' || :searchQuery || '%' " +
            "  AND (:letter = '' OR UPPER(LEFT(c.name, 1)) = UPPER(:letter)) " +
            "  AND (cardinality(:rarities) = 0 OR cd.card_rarity = ANY(:rarities)) " +
            "  AND (cardinality(:classes) = 0 OR c.card_class = ANY(:classes)) " +
            "  AND (cardinality(:categories) = 0 OR (" + CATEGORY_CASE + ") = ANY(:categories)) " +
            "  AND cd.energy_cost >= :costMin " +
            "  AND cd.energy_cost <= :costMax " +
            "GROUP BY cd.card_type")
    Flux<NamedCount> facetCountsType(String searchQuery, String letter,
                                     String[] rarities, String[] classes, String[] categories,
                                     int costMin, int costMax);

    @Query("SELECT (" + CATEGORY_CASE + ") AS k, COUNT(DISTINCT (c.name, c.card_class)) AS n " +
            "FROM card c JOIN card_detail cd ON cd.card_id = c.card_id " +
            "WHERE c.name ILIKE '%' || :searchQuery || '%' " +
            "  AND (:letter = '' OR UPPER(LEFT(c.name, 1)) = UPPER(:letter)) " +
            "  AND (cardinality(:rarities) = 0 OR cd.card_rarity = ANY(:rarities)) " +
            "  AND (cardinality(:classes) = 0 OR c.card_class = ANY(:classes)) " +
            "  AND (cardinality(:types) = 0 OR cd.card_type = ANY(:types)) " +
            "  AND cd.energy_cost >= :costMin " +
            "  AND cd.energy_cost <= :costMax " +
            "GROUP BY 1")
    Flux<NamedCount> facetCountsCategory(String searchQuery, String letter,
                                         String[] rarities, String[] classes, String[] types,
                                         int costMin, int costMax);

    @Query("SELECT 'total' AS k, COUNT(DISTINCT (name, card_class)) AS n FROM card " +
            "UNION ALL SELECT 'tiers', COUNT(*) FROM card " +
            "UNION ALL SELECT 'heroes', COUNT(DISTINCT name) FROM card WHERE card_class IN ('Warrior','Mage','Healer','Scout','MagicKnight') " +
            "UNION ALL SELECT 'monsters', COUNT(DISTINCT name) FROM card WHERE card_class = 'Monster' " +
            "UNION ALL SELECT 'items', COUNT(DISTINCT name) FROM card WHERE card_class = 'Item' " +
            "UNION ALL SELECT 'boons', COUNT(DISTINCT name) FROM card WHERE card_class = 'Boon' " +
            "UNION ALL SELECT 'injuries', COUNT(DISTINCT name) FROM card WHERE card_class = 'Injury' " +
            "UNION ALL SELECT 'specials', COUNT(DISTINCT name) FROM card WHERE card_class = 'Special'")
    Flux<NamedCount> statsCounts();

    @Query("SELECT DISTINCT card_type FROM card_detail WHERE card_type IS NOT NULL AND card_type <> '' ORDER BY 1")
    Flux<String> knownTypes();

    @Query("SELECT DISTINCT card_class FROM card WHERE card_class IS NOT NULL AND card_class <> '' ORDER BY 1")
    Flux<String> knownClasses();

    @Query("SELECT c.name AS name, c.card_class AS card_class, cc.character_id AS character_id " +
            "FROM character_card cc JOIN card c ON c.card_id = cc.card_id")
    Flux<SourceRow> findStarterDeckSources();
}
