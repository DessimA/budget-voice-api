package com.budget.api.controller;

import com.budget.api.dto.VoiceCommandResponse;
import com.budget.api.service.AudioTranscriptionService;
import com.budget.api.service.TextToSpeechService;
import com.budget.api.service.VoiceCommandService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/voice")
public final class VoiceCommandController {

    private final AudioTranscriptionService audioTranscriptionService;
    private final VoiceCommandService voiceCommandService;
    private final TextToSpeechService textToSpeechService;

    public VoiceCommandController(AudioTranscriptionService audioTranscriptionService,
                                  VoiceCommandService voiceCommandService,
                                  TextToSpeechService textToSpeechService) {
        this.audioTranscriptionService = audioTranscriptionService;
        this.voiceCommandService = voiceCommandService;
        this.textToSpeechService = textToSpeechService;
    }

    @PostMapping("/command")
    public ResponseEntity<VoiceCommandResponse> processVoiceCommand(
            @RequestParam("audio") MultipartFile audioFile) {
        try {
            String transcribedText = audioTranscriptionService.transcribe(audioFile);
            String aiResponse = voiceCommandService.processCommand(transcribedText);
            return ResponseEntity.ok(new VoiceCommandResponse(transcribedText, aiResponse, "success"));
        } catch (Exception e) {
            return ResponseEntity.unprocessableEntity()
                .body(new VoiceCommandResponse("", e.getMessage(), "error"));
        }
    }

    @PostMapping("/command/audio")
    public ResponseEntity<?> processVoiceCommandWithAudio(
            @RequestParam("audio") MultipartFile audioFile) {
        try {
            String transcribedText = audioTranscriptionService.transcribe(audioFile);
            String aiResponse = voiceCommandService.processCommand(transcribedText);

            byte[] audioBytes = textToSpeechService.synthesize(aiResponse);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/wav"));
            return ResponseEntity.ok().headers(headers).body(audioBytes);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("TTS")) {
                return ResponseEntity.status(503)
                    .body("Serviço TTS ainda inicializando. Aguarde alguns minutos e tente novamente.");
            }
            return ResponseEntity.unprocessableEntity()
                .body(new VoiceCommandResponse("", e.getMessage(), "error"));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Budget Voice API is running");
    }
}
