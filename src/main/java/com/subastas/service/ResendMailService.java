package com.subastas.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ResendMailService {

    private final WebClient webClient;

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${app.mail.from}")
    private String from;

    public ResendMailService(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("https://api.resend.com").build();
    }

    public void send(String to, String subject, String text) {
        Map<String, Object> body = Map.of(
            "from", from,
            "to", List.of(to),
            "subject", subject,
            "text", text
        );

        webClient.post()
            .uri("/emails")
            .header("Authorization", "Bearer " + apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(String.class)
            .block();

        log.info("Email enviado a {} | asunto: {}", to, subject);
    }
}
