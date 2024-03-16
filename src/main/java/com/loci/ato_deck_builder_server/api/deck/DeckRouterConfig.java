package com.loci.ato_deck_builder_server.api.deck;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class DeckRouterConfig {
    @Bean
    protected RouterFunction<ServerResponse> charRoutes(DeckHandler deckHandler) {
        return RouterFunctions.route()
                .GET("/deck", deckHandler::getDecks)
                .GET("/deck/{id}", deckHandler::getDeckDetails)
                .PUT("/deck/upload", deckHandler::uploadDeck)
                .build();
    }
}
