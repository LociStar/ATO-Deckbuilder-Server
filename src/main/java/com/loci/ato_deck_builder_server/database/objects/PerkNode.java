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
@Table("perk_node")
public class PerkNode {
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
}
