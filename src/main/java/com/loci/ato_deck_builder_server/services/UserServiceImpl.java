package com.loci.ato_deck_builder_server.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserServiceImpl implements UserService {

    private final KeycloakService keycloakService;

    @Autowired
    public UserServiceImpl(KeycloakService keycloakService) {
        this.keycloakService = keycloakService;
    }

    @Override
    public Mono<Void> updateUser(String userId, String username, String email) {
        return keycloakService.updateUser(userId, username, email).then();
    }

    @Override
    public Mono<Void> deleteUser(String userId) {
        return keycloakService.deleteUser(userId).then();
    }
}
