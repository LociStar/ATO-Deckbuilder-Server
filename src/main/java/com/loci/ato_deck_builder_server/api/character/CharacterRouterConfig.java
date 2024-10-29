package com.loci.ato_deck_builder_server.api.character;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class CharacterRouterConfig {

    @Bean
    protected RouterFunction<ServerResponse> characterRoutes(CharacterHandler characterHandler) {
        return RouterFunctions.route()
                .GET("/character", characterHandler::getAllCharacters)
                .GET("/character/image/{id}", characterHandler::getCharacterImage)
                .GET("/character/default/{id}", characterHandler::getCharacterDeck)
                .build();
    }
}
