package com.loci.ato_deck_builder_server.api.user;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class UserRouterConfig {
    @Bean
    protected RouterFunction<ServerResponse> userRouter(UserHandler userHandler) {
        return RouterFunctions.route()
                .PUT("/user/update", userHandler::updateUser)
                .DELETE("/user/delete", userHandler::deleteUser)
                .build();
    }
}
