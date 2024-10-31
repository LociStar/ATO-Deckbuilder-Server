// KeycloakService.java
package com.loci.ato_deck_builder_server.services;

import org.json.JSONObject;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KeycloakService {

    private final WebClient webClient;
    private final String clientId = System.getenv("keycloak_clientId");
    private final String clientSecret = System.getenv("keycloak_clientSecret");
    private static String accessToken;
    private static Instant tokenExpiryTime;

    public KeycloakService() {
        String keycloakUrl = System.getenv("keycloak_base_url");
        this.webClient = WebClient.builder().baseUrl(keycloakUrl).build();
    }

    public Mono<String> getUserId(String username) {
        return getToken().flatMap(token -> webClient.get()
                        .uri("/admin/realms/ATO-Deckbuilder/users?username={username}", username)
                        .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                        }))
                .map(s -> {
                    if (s.isEmpty())
                        return "";
                    Map<String, Object> firstUser = s.get(0);
                    return (String) firstUser.get("id");
                });
    }

    public Mono<String> getUsername(String userId) {
        Mono<String> tokenMono = getToken();
        return getUsernameWithToken(userId, tokenMono);
    }

    private Mono<String> getUsernameWithToken(String userId, Mono<String> tokenMono) {
        return tokenMono.flatMap(token ->
                        webClient.get()
                                .uri("/admin/realms/ATO-Deckbuilder/users/{userId}", userId)
                                .headers(httpHeaders -> httpHeaders.setBearerAuth(token))
                                .retrieve()
                                .bodyToMono(String.class))
                .map(s -> new JSONObject(s).getString("username"));
    }

    private Mono<String> getToken() {
        // If the token is not null, and it's not expired, return it
        if (accessToken != null && Instant.now().isBefore(tokenExpiryTime)) {
            return Mono.just(accessToken);
        }

        // Otherwise, request a new token
        return webClient.post()
                .uri("/realms/ATO-Deckbuilder/protocol/openid-connect/token")
                .headers(httpHeaders -> httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED))
                .headers(httpHeaders -> httpHeaders.setBasicAuth(clientId, clientSecret))
                .bodyValue("grant_type=client_credentials")
                .retrieve()
                .bodyToMono(String.class)
                .map(s -> {
                    JSONObject json = new JSONObject(s);
                    accessToken = json.getString("access_token");
                    int expiresIn = json.getInt("expires_in"); // Get the expiry time in seconds
                    tokenExpiryTime = Instant.now().plusSeconds(expiresIn - 60); // Subtract 60 seconds to account for possible delays
                    return accessToken;
                });
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
