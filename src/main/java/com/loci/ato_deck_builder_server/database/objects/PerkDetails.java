package com.loci.ato_deck_builder_server.database.objects;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("perk_details")
public class PerkDetails {
    @Id
    private String id;
    @Column("additional_currency")
    private int additionalCurrency;
    @Column("additional_shards")
    private int additionalShards;
    @Column("aura_curse_bonus")
    private String auraCurseBonus;
    @Column("aura_curse_bonus_value")
    private int auraCurseBonusValue;
    @Column("card_class")
    private String cardClass;
    @Column("custom_description")
    private String customDescription;
    @Column("damage_flat_bonus")
    private String damageFlatBonus;
    @Column("damage_flat_bonus_value")
    private int damageFlatBonusValue;
    @Column("energy_begin")
    private int energyBegin;
    @Column("heal_quantity")
    private int healQuantity;
    @Column("icon")
    private String icon;
    @Column("icon_text_value")
    private String iconTextValue;
    @Column("level")
    private int level;
    @Column("main_perk")
    private boolean mainPerk;
    @Column("max_health")
    private int maxHealth;
    @Column("obelisk_perk")
    private boolean obeliskPerk;
    @Column("resist_modified")
    private String resistModified;
    @Column("resist_modified_value")
    private int resistModifiedValue;
    @Column("row")
    private int row;
    @Column("speed_quantity")
    private int speedQuantity;
}
