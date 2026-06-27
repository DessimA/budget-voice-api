# Services

## VoiceCommandService

Usa o Spring AI `ChatClient` para enviar o texto transcrito ao
LLM (Groq Llama 3.3 70B via OpenAI-compatible API). O `ChatClient`
é configurado com um system prompt em português e tem acesso às
ferramentas `BudgetTools`. O LLM decide qual ferramenta chamar com
base no comando do usuário.

## AudioTranscriptionService

Usa `RestClient` para chamar diretamente a API Whisper do Groq.
Não depende do auto-configure do Spring AI para áudio. A chamada
é um POST multipart/form-data para `https://api.groq.com/openai/v1/audio/transcriptions`
com o arquivo de áudio, modelo, idioma "pt" e response_format "text".

## TextToSpeechService

Usa `RestClient` para chamar o Coqui TTS rodando em Docker no
container `budget_tts`. A requisição é um GET para `/api/tts?text=...`
e o retorno são bytes WAV.

## ValidationService

Serviço separado com responsabilidade única de validar dados antes
da persistência. Valida descrição, valor, tipo, categoria e data.

## TransactionService

Service de negócio que gerencia transações financeiras. Depende de
`TransactionRepository` para persistência e `ValidationService` para
validação. Oferece métodos para criar, listar, consultar saldo e
gerar resumos mensais.

## Acoplamento Fraco

Nenhum dos três services de IA conhece os outros:
- VoiceCommandService só depende de ChatClient e BudgetTools
- AudioTranscriptionService só depende de RestClient
- TextToSpeechService só depende de RestClient

Eles são orquestrados pelo Controller, que conhece todos os services.
