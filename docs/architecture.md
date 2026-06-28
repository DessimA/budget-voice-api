# Arquitetura

## Diagrama de Componentes

```mermaid
graph TB
    subgraph Docker["Docker Compose"]
        subgraph Frontend["Frontend (Navegador)"]
            UI[index.html + app.js]
        end

        subgraph API["budget-voice-api (Spring Boot)"]
            CTL_VOICE[VoiceCommandController]
            CTL_TX[TransactionController]
            ATS[AudioTranscriptionService<br/>RestClient -> Groq Whisper]
            VCS[VoiceCommandService<br/>Spring AI ChatClient]
            TTS_SVC[TextToSpeechService<br/>RestClient -> gTTS]
            VS[ValidationService]
            TS[TransactionService]
            BT[BudgetTools]
            GHE[GlobalExceptionHandler]
        end
        PG[(PostgreSQL 16)]
        GTTS[gTTS Service<br/>Flask + Google TTS<br/>Porta 5002]
    end

    subgraph Groq["Groq Cloud - Free Tier"]
        LLM[Llama 4 Scout 17B<br/>Tool Calling]
        WHISPER[Whisper Large v3 Turbo<br/>Transcrição PT-BR]
    end

    subgraph Google["Google Cloud"]
        GTTS_API[Google Translate TTS]
    end

    UI --> CTL_VOICE
    UI --> CTL_TX
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
    TTS_SVC --> GTTS
    GTTS --> GTTS_API
    CTL_VOICE --> GHE
    CTL_TX --> GHE
```

## Camadas

```mermaid
graph LR
    subgraph Presentation["Apresentação"]
        Ctrl[Controllers<br/>REST endpoints]
    end
    subgraph Service["Serviços"]
        Svc[Services<br/>Regras + IA]
        Tools["BudgetTools<br/>@Tool"]
    end
    subgraph Persistence["Persistência"]
        Repo[Repositories<br/>Spring Data JPA]
        DB[(PostgreSQL)]
    end

    Ctrl --> Svc
    Ctrl --> Tools
    Svc --> Repo
    Repo --> DB
```

1. **Controller**: `VoiceCommandController` (`/api/voice/**`) e `TransactionController` (`/api/transactions/**`)
2. **Service**: Lógica de negócio (`TransactionService`, `ValidationService`) e integração com IA (`VoiceCommandService`, `AudioTranscriptionService`, `TextToSpeechService`)
3. **Tools**: Ferramentas anotadas com `@Tool` chamadas pelo LLM via Tool Calling
4. **Domain**: Entidades JPA, enums e DTOs
5. **Repository**: Acesso a dados via Spring Data JPA

## Decisões Técnicas

### Por que AudioTranscriptionService usa RestClient diretamente?

```mermaid
sequenceDiagram
    participant VC as VoiceCommandController
    participant ATS as AudioTranscriptionService
    participant Groq as Groq Whisper API
    participant VCS as VoiceCommandService

    VC->>ATS: transcribe(file)
    ATS->>Groq: POST multipart (audio + model + lang=pt)
    Groq-->>ATS: texto transcrito
    ATS-->>VC: String
    VC->>VCS: processCommand(text)
```

O Spring AI possui auto-configure para módulos de áudio, mas optamos por `RestClient` direto por:

1. **Maior controle**: A API Whisper do Groq retorna texto plano com `response_format=text`
2. **Menos dependências**: Evita dependências específicas de áudio do Spring AI

### Por que Groq?

- **Custo zero**: Tier gratuito permanente sem cartão de crédito
- **Compatibilidade OpenAI**: API compatível com OpenAI, permitindo usar `spring-ai-starter-model-openai` apenas alterando `base-url` e `api-key`
- **Tool Calling**: Suporte completo a function calling
- **Whisper nativo**: Transcrição de áudio com o mesmo provedor

### Por que gTTS em Docker separado?

- **Isolamento**: Serviço independente (Flask + gTTS), não afeta a API
- **Sem GPU**: Usa API do Google Translate, não exige hardware local
- **Português**: gTTS com `lang='pt-BR'` tem suporte nativo
- **Setup rápido**: Sem download de modelos, container inicia em segundos
- **Trade-off**: Requer internet para síntese; endpoint de texto (`/api/voice/command`) funciona offline
