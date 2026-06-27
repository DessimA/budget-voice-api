package com.budget.api.service;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestClient;

@Service
public final class AudioTranscriptionService {

    private static final MediaType MULTIPART_FORM_DATA = MediaType.MULTIPART_FORM_DATA;

    private final RestClient restClient;
    private final String model;

    public AudioTranscriptionService(
            @Value("${groq.whisper.url}") String whisperUrl,
            @Value("${groq.whisper.model}") String model,
            @Value("${spring.ai.openai.api-key}") String apiKey) {
        this.model = model;
        this.restClient = RestClient.builder()
            .baseUrl(Objects.requireNonNull(whisperUrl, "Whisper URL must not be null"))
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .build();
    }

    public String transcribe(MultipartFile audioFile) {
        try {
            LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(audioFile.getBytes()) {
                @Override
                public String getFilename() {
                    return Objects.requireNonNullElse(
                        audioFile.getOriginalFilename(), "audio.wav");
                }
            });
            body.add("model", model);
            body.add("language", "pt");
            body.add("response_format", "text");

            return restClient.post()
                .contentType(MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(String.class);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao transcrever áudio: " + e.getMessage(), e);
        }
    }
}
