package com.budget.api.controller;

import com.budget.api.dto.VoiceCommandResponse;
import com.budget.api.exception.BusinessException;
import com.budget.api.service.AudioTranscriptionService;
import com.budget.api.service.TextToSpeechService;
import com.budget.api.service.VoiceCommandService;
import java.util.List;
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

    private static final long MAX_FILE_SIZE = 25L * 1024 * 1024;
    private static final List<String> ALLOWED_EXTENSIONS = List.of("wav", "mp3", "m4a", "ogg", "flac", "webm");

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
        validateAudioFile(audioFile);
        String transcribedText = audioTranscriptionService.transcribe(audioFile);
        String aiResponse = voiceCommandService.processCommand(transcribedText);
        return ResponseEntity.ok(new VoiceCommandResponse(transcribedText, aiResponse, "success"));
    }

    @PostMapping("/command/audio")
    public ResponseEntity<byte[]> processVoiceCommandWithAudio(
            @RequestParam("audio") MultipartFile audioFile) {
        validateAudioFile(audioFile);
        String transcribedText = audioTranscriptionService.transcribe(audioFile);
        String aiResponse = voiceCommandService.processCommand(transcribedText);

        byte[] audioBytes = textToSpeechService.synthesize(aiResponse);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/wav"));
        return ResponseEntity.ok().headers(headers).body(audioBytes);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Budget Voice API is running");
    }

    private void validateAudioFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Arquivo de áudio vazio ou não enviado");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("Arquivo excede o tamanho máximo de 25MB");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            throw new BusinessException("Formato de arquivo não reconhecido");
        }
        String extension = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(
                "Formato de arquivo não suportado: " + extension + ". Permitidos: " + ALLOWED_EXTENSIONS);
        }
    }
}
