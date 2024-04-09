package com.loci.ato_deck_builder_server.services;

import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

public class KeycloakService {

    private final WebClient webClient;
    private final String clientId = System.getenv("keycloak_clientId");
    private final String clientSecret = System.getenv("keycloak_clientSecret");

    public KeycloakService() {
        String keycloakUrl = System.getenv("keycloak_base_url");
        this.webClient = WebClient.builder().baseUrl(keycloakUrl).build();
    }

    public Mono<String> getUsername(String userId) {

        Mono<String> tokenMono = getToken();

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

    private Mono<String> getToken() {
        Mono<String> responseMono = webClient.post()
                .uri("/realms/ATO-Deckbuilder/protocol/openid-connect/token")
                .headers(httpHeaders -> httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED))
                //.header("Content-Type", "application/x-www-form-urlencoded")
                .headers(httpHeaders -> httpHeaders.setBasicAuth(clientId, clientSecret))
                .bodyValue("grant_type=client_credentials")
                .retrieve()
                .bodyToMono(String.class);

        return responseMono.map(s -> new JSONObject(s).getString("access_token"));
    }

    public Mono<String> updateUser(String userId, String username, String email) {
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("email", email);
        return getToken().flatMap(token -> webClient.put()
                .uri("/admin/realms/ATO-Deckbuilder/users/{userId}", userId)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class));
    }

    public Mono<String> deleteUser(String userId) {
        return getToken().flatMap(token -> webClient.delete()
                .uri("/admin/realms/ATO-Deckbuilder/users/{userId}", userId)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                .retrieve()
                .bodyToMono(String.class));
    }
}
