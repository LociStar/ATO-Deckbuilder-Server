package com.loci.ato_deck_builder_server.database.repositories;

import com.loci.ato_deck_builder_server.database.objects.CardDetails;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface CardDetailsRepository extends R2dbcRepository<CardDetails, String> {

}
