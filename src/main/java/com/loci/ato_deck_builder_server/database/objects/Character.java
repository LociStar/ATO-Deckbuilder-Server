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
@Table("character")
public class Character {
    @Id
    @Column("character_id")
    private String characterId;
}
