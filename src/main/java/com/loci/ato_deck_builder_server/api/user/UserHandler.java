package com.loci.ato_deck_builder_server.api.user;

import com.loci.ato_deck_builder_server.services.KeycloakService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class UserHandler {

    private final KeycloakService keycloakService;

    public UserHandler() {
        this.keycloakService = new KeycloakService();
    }

    public Mono<ServerResponse> updateUser(ServerRequest serverRequest) {
        Mono<Authentication> authenticationMono = ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication);
        return authenticationMono.flatMap(authentication -> {
            String userId = authentication.getName();
            String username = serverRequest.queryParam("username").orElse(null);
            String email = serverRequest.queryParam("email").orElse(null);
            return keycloakService.updateUser(userId, username, email)
                    .flatMap(s -> ServerResponse.ok().build());
        });
    }

    public Mono<ServerResponse> deleteUser(ServerRequest ignore) {
        Mono<Authentication> authenticationMono = ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication);
        return authenticationMono.flatMap(authentication -> {
            String userId = authentication.getName();
            return keycloakService.deleteUser(userId)
                    .flatMap(s -> ServerResponse.ok().build());
        });
    }
}
