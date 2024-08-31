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
@Table("perks")
public class Perks {
    @Id
    @Column("id")
    private int id;
    @Column("data")
    private String data;
    @Column("uid")
    private String uid;
    @Column("title")
    private String title;
}
