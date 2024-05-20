package com.loci.ato_deck_builder_server.api.card;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class CardRouterConfig {

    @Bean
    protected RouterFunction<ServerResponse> cardRoutes(CardHandler cardHandler) {
        return RouterFunctions.route()
                .GET("/card", cardHandler::getFilteredCards)
                .GET("/cardClass/{id}", cardHandler::getCardsByCardClass)
                //.GET("/card/{id}/details", cardHandler::getCardDetails)
                .GET("/card/image/{id}", cardHandler::getCardImageUrl)
                .GET("/card/sprite/{id}", cardHandler::getCardSpriteUrl)
                //.OPTIONS("/parseData", cardHandler::parseData)
                //.OPTIONS("/parseDetailsData", cardHandler::parseDetailsData)
                .build();
    }
}
