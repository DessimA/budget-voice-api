# Services

## VoiceCommandService

Usa o Spring AI `ChatClient` para enviar o texto transcrito ao
LLM (Groq Llama 3.3 70B via OpenAI-compatible API). O `ChatClient`
é configurado com um system prompt em português e tem acesso às
ferramentas `BudgetTools`. O LLM decide qual ferramenta chamar com
base no comando do usuário.

Trata retorno nulo do LLM com fallback para mensagem padrão.

## AudioTranscriptionService

Usa `RestClient` para chamar diretamente a API Whisper do Groq.
Não depende do auto-configure do Spring AI para áudio. A chamada
é um POST multipart/form-data para `https://api.groq.com/openai/v1/audio/transcriptions`
com o arquivo de áudio, modelo, idioma "pt" e response_format "text".

**Timeouts configurados:**
- connectTimeout: 10 segundos (tempo para estabelecer conexao com Groq)
- readTimeout: 60 segundos (transcricao de audios longos pode levar tempo)

Lanca `AudioProcessingException` em caso de falha.

## TextToSpeechService

Usa `RestClient` para chamar o Coqui TTS rodando em Docker no
container `budget_tts`. A requisicao e um GET para `/api/tts?text=...`
e o retorno sao bytes WAV.

**Timeouts configurados:**
- connectTimeout: 5 segundos (Coqui esta na rede local Docker)
- readTimeout: 30 segundos (sintese de textos longos)

Lanca `ExternalServiceException` em caso de falha.

## ValidationService

Servico separado com responsabilidade unica de validar dados antes
da persistencia. Valida descricao, valor, tipo, categoria e data.
Lanca `BusinessException` para qualquer violacao de regra.

## TransactionService

Service de negocio que gerencia transacoes financeiras. Depende de
`TransactionRepository` para persistencia e `ValidationService` para
validacao. Oferece metodos para criar, listar (paginado), consultar
saldo e gerar resumos mensais com agregacao no banco.

Usa `FormatUtils` para formatacao de datas em vez de definicoes locais.

## Acoplamento Fraco

Nenhum dos tres services de IA conhece os outros:
- VoiceCommandService so depende de ChatClient e BudgetTools
- AudioTranscriptionService so depende de RestClient
- TextToSpeechService so depende de RestClient

Eles sao orquestrados pelo Controller, que conhece todos os services.
