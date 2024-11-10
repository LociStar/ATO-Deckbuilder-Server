package com.loci.ato_deck_builder_server.api.perk;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class PerksRouterConfig {

    @Bean
    protected RouterFunction<ServerResponse> perksRoutes(PerksHandler perksHandler) {
        return RouterFunctions.route()
                .GET("/perks", perksHandler::getAllPerks)
                .GET("/perks/{id}", perksHandler::getPerks)
                .PUT("/perks/upload", perksHandler::uploadPerk)
                .GET("/perks/image/{id}", perksHandler::getPerkImage)
                .GET("/perks/details/{id}", perksHandler::getPerkDetails)
                .GET("/perks/main/all", perksHandler::getAllMainPerks)
                .build();
    }
}

