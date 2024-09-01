package com.loci.ato_deck_builder_server.database.repositories;

import com.loci.ato_deck_builder_server.database.objects.PerkDetails;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface PerkDetailsRepository extends R2dbcRepository<PerkDetails, String> {
}
