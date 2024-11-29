package com.loci.ato_deck_builder_server.api.description;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class DescriptionRouterConfig {
    @Bean
    protected RouterFunction<ServerResponse> descriptionRoutes(DescriptionHandler descriptionsHandler) {
        return RouterFunctions.route()
                .GET("/description/{id}", descriptionsHandler::getDescription)
                .build();
    }
}
