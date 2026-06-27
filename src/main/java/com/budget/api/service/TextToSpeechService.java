package com.budget.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public final class TextToSpeechService {

    private final RestClient restClient;

    public TextToSpeechService(@Value("${tts.coqui.url}") String ttsUrl) {
        this.restClient = RestClient.builder()
            .baseUrl(ttsUrl)
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
            throw new RuntimeException("Serviço TTS indisponível", e);
        }
    }
}
