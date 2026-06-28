# Arquitetura

## Diagrama de Componentes

```mermaid
graph TB
    subgraph Docker["Docker Compose - Custo Zero"]
        subgraph API["budget-voice-api (Spring Boot)"]
            CTL_VOICE[VoiceCommandController]
            CTL_TX[TransactionController]
            ATS[AudioTranscriptionService\nRestClient -> Groq Whisper]
            VCS[VoiceCommandService\nSpring AI ChatClient]
            TTS_SVC[TextToSpeechService\nRestClient -> gTTS]
            VS[ValidationService]
            TS[TransactionService]
            BT[BudgetTools]
        end
        PG[(PostgreSQL 16)]
        COQUI[gTTS Service (Flask + Google TTS)\nPorta 5002]
    end

    subgraph Groq["Groq Cloud - Free Tier"]
        LLM[Llama 4 Scout 17B\nTool Calling]
        WHISPER[Whisper Large v3 Turbo\nTranscrição PT-BR]
    end

    CTL_VOICE --> ATS
    CTL_VOICE --> VCS
    CTL_VOICE --> TTS_SVC
    CTL_TX --> TS
    VCS --> BT
    BT --> VS
    BT --> TS
    TS --> PG
    ATS --> WHISPER
    VCS --> LLM
    TTS_SVC --> COQUI
```

## Camadas

1. **Controller**: Dois controllers separados por responsabilidade.
   `VoiceCommandController` gerencia `/api/voice/**` (áudio e IA).
   `TransactionController` gerencia `/api/transactions/**` (CRUD).
2. **Service**: Lógica de negócio e integração com IA.
3. **Tools**: Ferramentas chamadas pelo LLM via Tool Calling.
4. **Domain**: Entidades JPA e enums.
5. **Repository**: Acesso a dados via Spring Data JPA.

## Decisões Técnicas

### Por que AudioTranscriptionService usa RestClient diretamente?

O Spring AI possui auto-configure para módulos de áudio, mas optamos
por `RestClient` direto por duas razões:

1. **Maior controle**: A API Whisper do Groq retorna texto plano
   quando `response_format=text`, e o RestClient lida com isso de
   forma mais simples.
2. **Menos dependências**: Evita adicionar dependências específicas
   de áudio do Spring AI que poderiam conflitar ou exigir configuração
   adicional desnecessária.

### Por que Groq?

- **Custo zero**: Tier gratuito permanente sem cartão de crédito.
- **Compatibilidade OpenAI**: O Groq é compatível com a API da OpenAI,
  permitindo usar `spring-ai-starter-model-openai` apenas
  alterando `base-url` e `api-key` no `application.yml`.
- **Tool Calling**: Suporte completo a function calling, essencial
  para o fluxo de comandos de voz.
- **Whisper nativo**: Transcrição de áudio com o mesmo provedor.

### Por que gTTS em Docker separado?

- **Isolamento**: O serviço TTS roda em container independente (Flask + gTTS),
  não afetando a inicialização da API.
- **Sem GPU**: Nenhum modelo local é necessário. A síntese usa a API do Google
  Translate (gTTS), que é gratuita e não exige infraestrutura local.
- **Português**: gTTS com `lang='pt-BR'` tem suporte nativo a português brasileiro.
- **Sem cache de modelo**: Diferente de modelos locais, não há download de pesos.
  O container inicia em segundos.
- **Trade-off**: Requer conexão com internet para síntese. Em ambiente offline,
  o endpoint de texto (`/api/voice/command`) funciona normalmente; apenas o
  endpoint de áudio (`/api/voice/command/audio`) fica indisponível.
