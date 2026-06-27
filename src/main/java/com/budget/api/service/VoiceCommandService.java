package com.budget.api.service;

import java.util.Objects;
import com.budget.api.tools.BudgetTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public final class VoiceCommandService {

    private final ChatClient chatClient;
    private final BudgetTools budgetTools;

    public VoiceCommandService(ChatClient chatClient, BudgetTools budgetTools) {
        this.chatClient = chatClient;
        this.budgetTools = budgetTools;
    }

    public String processCommand(String transcribedText) {
        String content = chatClient.prompt()
            .user(transcribedText)
            .tools(budgetTools)
            .call()
            .content();
        return Objects.requireNonNullElse(content, "Não foi possível processar o comando. Tente novamente.");
    }
}
