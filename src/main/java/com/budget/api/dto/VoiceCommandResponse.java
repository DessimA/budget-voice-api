package com.budget.api.dto;

public record VoiceCommandResponse(
    String transcribedText,
    String aiResponse,
    String status
) {
}
