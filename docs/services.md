# Services

## VoiceCommandService

Usa o Spring AI `ChatClient` para enviar o texto transcrito ao
LLM (Groq Llama 4 Scout via OpenAI-compatible API). O `ChatClient`
é configurado por request com um system prompt dinâmico (gerado por
`AiConfig.buildSystemPrompt()`) e tem acesso às ferramentas `BudgetTools`.
O LLM decide qual ferramenta chamar com base no comando do usuário.

Trata retorno nulo do LLM com fallback para mensagem padrão.

## AudioTranscriptionService

Usa `RestClient` para chamar diretamente a API Whisper do Groq.
Não depende do auto-configure do Spring AI para áudio. A chamada
é um POST multipart/form-data para `https://api.groq.com/openai/v1/audio/transcriptions`
com o arquivo de áudio, modelo, idioma "pt" e response_format "text".

**Timeouts configurados:**
- connectTimeout: 10 segundos (tempo para estabelecer conexao com Groq)
- readTimeout: 60 segundos (transcricao de audios longos pode levar tempo)

Lança `AudioProcessingException` em caso de falha.

## TextToSpeechService

Usa `RestClient` para chamar o serviço gTTS rodando em Docker no
container `budget_tts`. A requisição é um GET para `/api/tts?text=...`
e o retorno são bytes WAV.

O serviço gTTS é um servidor Flask que usa a biblioteca `gTTS` para
chamar a API do Google Translate e converter texto em áudio MP3, depois
converte para WAV usando `pydub` + `ffmpeg`.

**Timeouts configurados:**
- connectTimeout: 5 segundos (serviço na rede local Docker)
- readTimeout: 30 segundos (síntese de textos longos)

Lança `ExternalServiceException` em caso de falha, retornando HTTP 503.

## ValidationService

Serviço separado com responsabilidade única de validar dados antes
da persistência. Valida descrição, valor, tipo, categoria e data.
Lança `BusinessException` para qualquer violação de regra.

## TransactionService

Service de negócio que gerencia transações financeiras. Depende de
`TransactionRepository` para persistência e `ValidationService` para
validação. Oferece métodos para criar, listar (paginado), consultar
saldo e gerar resumos mensais com agregação no banco.

Usa `FormatUtils` para formatação de datas em vez de definições locais.

## Acoplamento Fraco

Nenhum dos três services de IA conhece os outros:
- VoiceCommandService so depende de ChatClient e BudgetTools
- AudioTranscriptionService so depende de RestClient
- TextToSpeechService so depende de RestClient

Eles são orquestrados pelo Controller, que conhece todos os services.
