# Controller

## Endpoints

### POST `/api/voice/command`

Processa um comando de voz enviado como arquivo de áudio.

- **Request**: `multipart/form-data` com campo `audio`
- **Response 200**: `VoiceCommandResponse` com transcrição e resposta da IA
- **Response 422**: `VoiceCommandResponse` com status "error" em caso de falha

Exemplo:
```bash
curl -X POST -F "audio=@comando.mp3" http://localhost:8080/api/voice/command
```

### POST `/api/voice/command/audio`

Processa comando de voz e retorna resposta em áudio WAV.

- **Request**: `multipart/form-data` com campo `audio`
- **Response 200**: `audio/wav` com a resposta sintetizada
- **Response 503**: Texto informando que o TTS ainda está inicializando
- **Response 422**: `VoiceCommandResponse` com status "error"

```bash
curl -X POST -F "audio=@comando.mp3" http://localhost:8080/api/voice/command/audio --output resposta.wav
```

### GET `/api/voice/health`

Verifica se a API está rodando.

- **Response 200**: "Budget Voice API is running"

```bash
curl http://localhost:8080/api/voice/health
```

### GET `/api/transactions`

Lista todas as transações ordenadas por data descendente.

- **Response 200**: `List<TransactionResponse>`

```bash
curl http://localhost:8080/api/transactions
```

### GET `/api/transactions/balance`

Retorna o saldo atual (entradas - saídas).

- **Response 200**: `{"balance": 1234.56}`

```bash
curl http://localhost:8080/api/transactions/balance
```

### GET `/api/transactions/summary/{year}/{month}`

Resumo financeiro de um mês específico.

- **Response 200**: `MonthlySummaryResponse`

```bash
curl http://localhost:8080/api/transactions/summary/2025/6
```

## Observações

O endpoint `/api/voice/command/audio` pode retornar 503 durante os
primeiros minutos enquanto o Coqui TTS finaliza a inicialização.
O endpoint `/api/voice/command` não depende do TTS e funciona
imediatamente.
