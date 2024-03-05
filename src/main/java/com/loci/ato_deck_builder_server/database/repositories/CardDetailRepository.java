package com.loci.ato_deck_builder_server.database.repositories;

import com.loci.ato_deck_builder_server.database.objects.CardDetail;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface CardDetailRepository extends R2dbcRepository<CardDetail, String> {

}
