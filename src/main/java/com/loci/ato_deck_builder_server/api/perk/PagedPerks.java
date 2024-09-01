package com.loci.ato_deck_builder_server.api.perk;

import com.loci.ato_deck_builder_server.database.objects.Perks;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PagedPerks {
    private List<Perks> perks;
    private int pages;
}
