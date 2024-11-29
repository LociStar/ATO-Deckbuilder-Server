package com.loci.ato_deck_builder_server.dto;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerkNodeDTO {
    @Id
    @Column("id")
    private String id;

    @Column("column")
    private Integer column;

    @Column("cost")
    private String cost;

    @Column("locked_in_town")
    private Boolean lockedInTown;

    @Column("no_stack")
    private Boolean noStack;

    @Column("perk")
    private String perk;

    @Column("perk_required")
    private String perkRequired;

    @Column("perks_connected")
    private String[] perksConnected;

    @Column("row")
    private Integer row;

    @Column("sprite")
    private String sprite;

    @Column("type")
    private Integer type;

    @Column("value")
    private String description;

    @Column("icon_text_value")
    private String iconTextValue;

    @Column("max_health")
    private Integer maxHealth;

    @Column("additional_currency")
    private Integer additionalCurrency;

    @Column("additional_shards")
    private Integer additionalShards;

    @Column("speed_quantity")
    private Integer speedQuantity;

    @Column("heal_quantity")
    private Integer healQuantity;

    @Column("energy_begin")
    private Integer energyBegin;

    @Column("aura_curse_bonus")
    private String auraCurseBonus;

    @Column("resist_modified")
    private String resistModified;

    @Column("damage_flat_bonus")
    private String damageFlatBonus;
}
