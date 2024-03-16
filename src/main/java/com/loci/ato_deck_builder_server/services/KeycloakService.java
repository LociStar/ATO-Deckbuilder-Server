package com.loci.ato_deck_builder_server.services;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

public class KeycloakService {

    private final WebClient webClient;
    @Value("${keycloak.clientId}")
    private String clientId;
    @Value("${keycloak.clientSecret}")
    private String clientSecret;
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String keycloakUrl;

    public KeycloakService() {
        this.webClient = WebClient.builder().baseUrl("https://keycloak.organizer-bot.com").build();
    }

    public Mono<String> getUsername(String userId) {

        Mono<String> responseMono = webClient.post()
                .uri("/realms/ATO-Deckbuilder/protocol/openid-connect/token")
                .headers(httpHeaders -> httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED))
                //.header("Content-Type", "application/x-www-form-urlencoded")
                .headers(httpHeaders -> httpHeaders.setBasicAuth("server", "lrFObreEi4AaLWSr34Gg6vNj9AJrm8Az"))
                .bodyValue("grant_type=client_credentials")
                .retrieve()
                .bodyToMono(String.class);

        Mono<String> tokenMono = responseMono.map(s -> new JSONObject(s).getString("access_token"));

        // Use the access token to fetch the username
        return getUsernameWithToken(userId, tokenMono);
    }

    private Mono<String> getUsernameWithToken(String userId, Mono<String> tokenMono) {

        Mono<String> responseMono = tokenMono.flatMap(token ->
                webClient.get()
                .uri("/admin/realms/ATO-Deckbuilder/users/{userId}", userId)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                .retrieve()
                .bodyToMono(String.class));

        return responseMono.map(s -> new JSONObject(s).getString("username"));
    }
}
