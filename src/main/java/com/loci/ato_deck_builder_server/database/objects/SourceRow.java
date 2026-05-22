package com.loci.ato_deck_builder_server.database.objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceRow {
    @Column("name")
    private String name;

    @Column("card_class")
    private String cardClass;

    @Column("character_id")
    private String characterId;
}
