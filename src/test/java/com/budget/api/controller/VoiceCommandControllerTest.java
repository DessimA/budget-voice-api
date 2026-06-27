package com.budget.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.budget.api.service.AudioTranscriptionService;
import com.budget.api.service.TextToSpeechService;
import com.budget.api.service.VoiceCommandService;

@WebMvcTest(VoiceCommandController.class)
class VoiceCommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class MockConfig {
        @Bean
        public AudioTranscriptionService audioTranscriptionService() {
            return org.mockito.Mockito.mock(AudioTranscriptionService.class);
        }

        @Bean
        public VoiceCommandService voiceCommandService() {
            return org.mockito.Mockito.mock(VoiceCommandService.class);
        }

        @Bean
        public TextToSpeechService textToSpeechService() {
            return org.mockito.Mockito.mock(TextToSpeechService.class);
        }
    }

    @Test
    void postSemArquivoDeveRetornar400() throws Exception {
        mockMvc.perform(multipart("/api/voice/command"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void postComArquivoMaiorQue25MBDeveRetornar413() throws Exception {
        byte[] largeContent = new byte[26 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
            "audio", "teste.mp3", MediaType.APPLICATION_OCTET_STREAM_VALUE, largeContent);

        mockMvc.perform(multipart("/api/voice/command").file(file))
            .andExpect(status().isRequestEntityTooLarge());
    }

    @Test
    void getHealthDeveRetornar200() throws Exception {
        mockMvc.perform(get("/api/voice/health"))
            .andExpect(status().isOk());
    }
}
