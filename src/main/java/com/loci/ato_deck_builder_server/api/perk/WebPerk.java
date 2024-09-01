package com.loci.ato_deck_builder_server.api.perk;

import lombok.*;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebPerk {
    private String title;
    private String perks;
}
