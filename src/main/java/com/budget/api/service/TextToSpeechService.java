package com.budget.api.service;

import com.budget.api.exception.ExternalServiceException;
import java.time.Duration;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public final class TextToSpeechService {

    private final RestClient restClient;

    public TextToSpeechService(@Value("${tts.coqui.url}") String ttsUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = RestClient.builder()
            .baseUrl(Objects.requireNonNull(ttsUrl, "TTS URL must not be null"))
            .requestFactory(factory)
            .build();
    }

    public byte[] synthesize(String text) {
        try {
            String uri = UriComponentsBuilder.fromPath("/api/tts")
                .queryParam("text", text)
                .build()
                .encode()
                .toUriString();

            return restClient.get()
                .uri(uri)
                .retrieve()
                .body(byte[].class);
        } catch (Exception e) {
            throw new ExternalServiceException("Serviço TTS indisponível", e);
        }
    }
}
