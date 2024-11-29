package com.loci.ato_deck_builder_server.database.repositories;

import com.loci.ato_deck_builder_server.database.objects.PerkNode;
import com.loci.ato_deck_builder_server.dto.PerkNodeDTO;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PerkNodeRepository extends R2dbcRepository<PerkNode, String> {

    @Query("""
            SELECT pn.id, pn.column, pn.cost, pn.locked_in_town, pn.no_stack, pn.perk, pn.perk_required, pn.perks_connected, pn.row, pn.sprite, pn.type, d.value, pd.icon_text_value,
            pd.max_health, pd.additional_currency, pd.additional_shards, pd.speed_quantity, pd.heal_quantity, pd.energy_begin, pd.aura_curse_bonus, pd.resist_modified, pd.damage_flat_bonus
            FROM perk_node as pn
            LEFT JOIN perk_details AS pd ON pn.perk = pd.id
            LEFT JOIN description AS d ON pd.custom_description = d.id
            """)
    Flux<PerkNodeDTO> getAllPerkNodeData();
}
