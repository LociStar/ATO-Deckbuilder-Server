package com.loci.ato_deck_builder_server.database.objects;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.List;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("deck")
public class Deck {
    @Id
    @Column("deck_id")
    private int id;

    @Column("title")
    private String title;

    @Column("description")
    private String description;

    @Column("likes")
    private int likes;

    @Column("char_id")
    private String characterId;

    @Column("user_id")
    private String userId;
}
