package com.budget.api.config;

import java.util.Objects;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public final class AiConfig {

    private static final String SYSTEM_PROMPT = """
        Você é um assistente financeiro pessoal chamado "Orçamento AI".
        Você ajuda usuários a gerenciar suas finanças pessoais através de comandos de voz.
        Sempre use as ferramentas disponíveis para executar as ações solicitadas.
        Responda de forma concisa e amigável em português.
        Confirme as ações realizadas com os valores exatos.
        Quando não entender o comando, peça esclarecimento educadamente.
        Formate valores monetários no padrão brasileiro (R$ 1.234,56).
        """;

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(Objects.requireNonNull(chatModel, "ChatModel must not be null"))
            .defaultSystem(SYSTEM_PROMPT)
            .build();
    }
}
