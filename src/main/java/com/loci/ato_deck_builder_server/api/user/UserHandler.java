package com.loci.ato_deck_builder_server.api.user;

import com.loci.ato_deck_builder_server.services.UserService;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class UserHandler {

    private final UserService userService;

    public UserHandler(UserService userService) {
        this.userService = userService;
    }

    public Mono<ServerResponse> updateUser(ServerRequest serverRequest) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(authentication -> {
                    String userId = authentication.getName();
                    String username = serverRequest.queryParam("username").orElse(null);
                    String email = serverRequest.queryParam("email").orElse(null);
                    return userService.updateUser(userId, username, email)
                            .then(ServerResponse.ok().build());
                })
                .onErrorResume(e -> ServerResponse.status(500).bodyValue(e.getMessage()));
    }

    public Mono<ServerResponse> deleteUser(ServerRequest ignore) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(authentication -> {
                    String userId = authentication.getName();
                    return userService.deleteUser(userId)
                            .then(ServerResponse.ok().build());
                })
                .onErrorResume(e -> ServerResponse.status(500).bodyValue(e.getMessage()));
    }
}
