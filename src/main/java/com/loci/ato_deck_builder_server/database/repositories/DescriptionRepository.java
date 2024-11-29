package com.loci.ato_deck_builder_server.database.repositories;

import com.loci.ato_deck_builder_server.database.objects.Description;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface DescriptionRepository extends R2dbcRepository<Description, String> {
}
