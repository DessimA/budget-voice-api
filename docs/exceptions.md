# Hierarquia de Exceções

## Estrutura

```
RuntimeException
├── BusinessException        — Erros de regra de negócio (validação)
├── AudioProcessingException — Falhas de transcrição ou síntese de áudio
└── ExternalServiceException — Falhas de comunicação com serviços externos
```

## Quando usar cada tipo

- `BusinessException`: validações de campos, valores inválidos, tipos
  inválidos, datas inválidas. Retorna HTTP 422.
- `AudioProcessingException`: falhas na transcrição de áudio (Groq Whisper)
  ou síntese de voz (Coqui TTS). Retorna HTTP 422.
- `ExternalServiceException`: falhas de comunicação com Groq API, Coqui TTS,
  ou qualquer serviço externo. Retorna HTTP 503.

## Por que não usar exceções genéricas

Exceções genéricas (`RuntimeException`, `IllegalArgumentException`) não
carregam semântica sobre a natureza do erro. O `GlobalExceptionHandler`
precisa do tipo da exceção para determinar o status HTTP correto e a
mensagem apropriada. Exceções específicas permitem tratamento diferenciado
sem inspecionar mensagens ou usar `instanceof` excessivo.
