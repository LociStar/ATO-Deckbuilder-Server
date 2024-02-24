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
@Table("card")
public class Card {
    @Id
    @Column("id")
    private String id;

    @Column("name")
    private String name;

    @Column("class")
    private String cardClass;

    @Column("version")
    private String version;
}
