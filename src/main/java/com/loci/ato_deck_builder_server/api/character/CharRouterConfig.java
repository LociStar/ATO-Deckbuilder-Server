package com.loci.ato_deck_builder_server.api.character;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class CharRouterConfig {
    @Bean
    protected RouterFunction<ServerResponse> charRoutes(CharHandler charHandler) {
        return RouterFunctions.route()
                .GET("/char", charHandler::getCharBuilds)
                .build();
    }
}
