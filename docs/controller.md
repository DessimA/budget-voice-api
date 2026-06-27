# Controller

## VoiceCommandController

Gerencia os endpoints de comandos de voz em `/api/voice/**`.

### POST `/api/voice/command`

Processa um comando de voz enviado como arquivo de audio.

- **Request**: `multipart/form-data` com campo `audio`
- **Response 200**: `VoiceCommandResponse` com transcricao e resposta da IA
- **Response 400**: Arquivo nao enviado
- **Response 413**: Arquivo excede 25MB
- **Response 422**: Erro de negocio (formato invalido) ou falha de transcricao

Exemplo:
```bash
curl -X POST -F "audio=@comando.mp3" http://localhost:8080/api/voice/command
```

### POST `/api/voice/command/audio`

Processa comando de voz e retorna resposta em audio WAV.

- **Request**: `multipart/form-data` com campo `audio`
- **Response 200**: `audio/wav` com a resposta sintetizada
- **Response 400**: Arquivo nao enviado
- **Response 413**: Arquivo excede 25MB
- **Response 422**: Erro de negocio ou falha de transcricao
- **Response 503**: Servico TTS indisponivel

```bash
curl -X POST -F "audio=@comando.mp3" http://localhost:8080/api/voice/command/audio --output resposta.wav
```

### GET `/api/voice/health`

Verifica se a API esta rodando.

- **Response 200**: "Budget Voice API is running"

```bash
curl http://localhost:8080/api/voice/health
```

### Validacao de Upload

Antes de processar o audio, o controller valida:
- Arquivo nao pode ser nulo ou vazio
- Tamanho maximo de 25MB (limite da API Whisper)
- Extensoes permitidas: wav, mp3, m4a, ogg, flac, webm

Validacoes falhas lancam `BusinessException` tratado pelo
`GlobalExceptionHandler`.

## TransactionController

Gerencia os endpoints de transacoes em `/api/transactions/**`.

### GET `/api/transactions`

Lista transacoes com paginacao.

- **Parametros**: `page` (default 0), `size` (default 20, max 100)
- **Response 200**: `Page<TransactionResponse>`

```bash
curl "http://localhost:8080/api/transactions?page=0&size=10"
```

### GET `/api/transactions/balance`

Retorna o saldo atual (entradas - saidas).

- **Response 200**: `{"balance": 1234.56}`

```bash
curl http://localhost:8080/api/transactions/balance
```

### GET `/api/transactions/summary/{year}/{month}`

Resumo financeiro de um mes especifico.

- **Response 200**: `MonthlySummaryResponse`

```bash
curl http://localhost:8080/api/transactions/summary/2025/6
```

## Tratamento de Erros

O `GlobalExceptionHandler` centraliza o tratamento de excecoes:

| Excecao | Status | Body |
|---|---|---|
| `BusinessException` | 422 | `{"error": mensagem}` |
| `AudioProcessingException` | 422 | `{"error": mensagem}` |
| `ExternalServiceException` | 503 | `{"error": mensagem}` |
| `MethodArgumentNotValidException` | 400 | `[{campo: mensagem}]` |
| `MaxUploadSizeExceededException` | 413 | `{"error": "Arquivo excede o tamanho maximo de 25MB"}` |
| `Exception` (fallback) | 500 | `{"error": "Erro interno do servidor"}` |
