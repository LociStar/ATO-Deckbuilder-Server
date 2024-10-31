package com.loci.ato_deck_builder_server.services;

import reactor.core.publisher.Mono;

public interface UserService {
    Mono<Void> updateUser(String userId, String username, String email);

    Mono<Void> deleteUser(String userId);
}
