package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GroqService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    private final WebClient webClient = WebClient.create();

    public Mono<String> askGroq(String prompt) {

        Map<String, Object> body = Map.of(
                "model", "qwen/qwen3-32b",
                "messages", new Object[]{
                        Map.of("role", "user", "content", prompt)
                }
        );

        return webClient.post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> {
                    var choices = (java.util.List<Map<String, Object>>) resp.get("choices");
                    Map<String, Object> msg = (Map<String, Object>) ((Map<?, ?>) choices.get(0)).get("message");
                    return (String) msg.get("content");
                });
    }
}
