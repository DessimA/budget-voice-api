package com.budget.api.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

class AiConfigTest {

    private static final DateTimeFormatter DATE_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Test
    void buildSystemPromptDeveConterDataAtual() {
        String prompt = AiConfig.buildSystemPrompt();
        String today = LocalDate.now().format(DATE_ISO);
        assertTrue(prompt.contains(today),
            "System prompt deve conter a data atual: " + today);
    }

    @Test
    void buildSystemPromptDeveConterMesAtual() {
        String prompt = AiConfig.buildSystemPrompt();
        int currentMonth = LocalDate.now().getMonthValue();
        assertTrue(prompt.contains(String.valueOf(currentMonth)),
            "System prompt deve conter o mês atual: " + currentMonth);
    }

    @Test
    void buildSystemPromptNaoDeveConterAnoPredefinido() {
        String prompt = AiConfig.buildSystemPrompt();
        assertFalse(prompt.contains("2025-01-01"),
            "System prompt nao deve conter datas hardcodadas");
    }

    @Test
    void buildSystemPromptDeveConterOntem() {
        String prompt = AiConfig.buildSystemPrompt();
        String yesterday = LocalDate.now().minusDays(1).format(DATE_ISO);
        assertTrue(prompt.contains(yesterday),
            "System prompt deve conter a data de ontem: " + yesterday);
    }

    @Test
    void buildSystemPromptDeveSempreGerarConteudoDiferente() {
        String prompt1 = AiConfig.buildSystemPrompt();
        String prompt2 = AiConfig.buildSystemPrompt();
        assertTrue(prompt1.equals(prompt2),
            "Duas chamadas no mesmo dia devem gerar o mesmo prompt");
    }
}
