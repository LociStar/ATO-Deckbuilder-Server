package com.loci.ato_deck_builder_server.database.repositories;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface CharacterRepository extends R2dbcRepository<Character, String>{
}
