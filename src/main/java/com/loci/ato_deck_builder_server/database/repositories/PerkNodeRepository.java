package com.loci.ato_deck_builder_server.database.repositories;

import com.loci.ato_deck_builder_server.database.objects.PerkNode;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface PerkNodeRepository extends R2dbcRepository<PerkNode, String> {
}
