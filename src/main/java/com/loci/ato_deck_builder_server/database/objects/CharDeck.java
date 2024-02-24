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
@Table("char_deck")
public class CharDeck {
    @Id
    @Column("id")
    private String id;

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

    @Column("card_ids")
    private List<String> cardIds;
}
