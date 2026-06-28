# Budget Voice API

API REST que aceita comandos de voz em portugues para gerenciar
financas pessoais. Usa Spring AI com Tool Calling para entender a
intencao do usuario e executar operacoes reais no banco de dados.
Stack 100% gratuita: Groq (LLM + transcricao) e gTTS (sintese
de voz via Google TTS em Docker).

## Pre-requisitos

- Docker 24+
- Docker Compose v2+
- Conta gratuita no [Groq Console](https://console.groq.com) (sem cartao de credito)

## Como Executar

```bash
git clone <url>
cd budget-voice-api

cp .env.example .env
# Editar .env e inserir GROQ_API_KEY=gsk_sua_chave

docker compose up --build
```

A API estara disponivel em `http://localhost:8080` assim que o
PostgreSQL e o Spring Boot subirem. O servico gTTS (Flask) sobe rapidamente
(segundos) pois nao ha download de modelos. O endpoint `/api/voice/command/audio`
requer conexao com internet para sintetizar voz via Google TTS.

## Como Testar

```bash
# Verificar saude da API
curl http://localhost:8080/api/voice/health

# Enviar arquivo de audio
curl -X POST -F "audio=@meuaudio.mp3" http://localhost:8080/api/voice/command

# Consultar transacoes (paginado)
curl "http://localhost:8080/api/transactions?page=0&size=10"

# Consultar saldo
curl http://localhost:8080/api/transactions/balance

# Resumo do mes
curl http://localhost:8080/api/transactions/summary/2025/6

# Resposta em audio
curl -X POST -F "audio=@meuaudio.mp3" http://localhost:8080/api/voice/command/audio --output resposta.wav
```

## Endpoints

| Metodo | Path | Parametros | Resposta | Status |
|---|---|---|---|---|
| GET | `/api/voice/health` | - | `"Budget Voice API is running"` | 200 |
| POST | `/api/voice/command` | `audio` (file) | `VoiceCommandResponse` | 200, 400, 413, 422 |
| POST | `/api/voice/command/audio` | `audio` (file) | `audio/wav` | 200, 400, 413, 422, 503 |
| GET | `/api/transactions` | `page`, `size` | `Page<TransactionResponse>` | 200 |
| GET | `/api/transactions/balance` | - | `{"balance": ...}` | 200 |
| GET | `/api/transactions/summary/{year}/{month}` | - | `MonthlySummaryResponse` | 200 |

## Tratamento de Erros

| Status | Causa |
|---|---|
| 400 | Parametro obrigatorio ausente ou invalido |
| 413 | Arquivo de audio excede 25MB |
| 422 | Erro de negocio (validacao, formato, transcricao) |
| 503 | Servico TTS indisponivel |
| 500 | Erro interno generico |

## Exemplos de Comandos de Voz

- "Gastei cinquenta reais em almoco hoje"
- "Recebi meu salario de cinco mil reais"
- "Qual e meu saldo atual?"
- "Como foram meus gastos esse mes?"
- "Mostre meus gastos dos ultimos 7 dias"
- "Quanto gastei em cada categoria?"

## Tecnologias

| Tecnologia | Versao | Papel |
|---|---|---|
| Java | 21 | Runtime |
| Spring Boot | 3.5.x | Framework |
| Spring AI | 1.0.0 | Integracao Groq LLM |
| PostgreSQL | 16-alpine | Banco de dados |
| Flyway | 10.x | Migracoes de banco |
| Groq Llama 4 Scout | meta-llama/llama-4-scout-17b-16e-instruct | LLM + Tool Calling |
| Groq Whisper | whisper-large-v3-turbo | Transcricao |
| gTTS + Flask | 2.x | Sintese de voz (Google TTS via Docker) |

## Custo de Operacao

| Recurso | Provedor | Custo |
|---|---|---|
| LLM com Tool Calling | Groq (Llama 4 Scout) | Gratuito |
| Transcricao de audio | Groq Whisper | Gratuito |
| Sintese de voz | gTTS + Flask (Docker) | Gratuito |
| Banco de dados | PostgreSQL (Docker) | Gratuito |

## Melhorias Implementadas

1. `TransactionCategory` enum com 9 categorias para classificacao
2. `ValidationService` desacoplado com regras de negocio
3. Tool `getMonthlySummary` para relatorios mensais por voz
4. Tool `getBalanceByCategory` para consulta por categoria
5. Campos de auditoria `createdAt` e `updatedAt` na entidade
6. `MonthlySummaryResponse` com breakdown completo por categoria
7. Hierarquia de excecoes customizadas (`BusinessException`, `AudioProcessingException`, `ExternalServiceException`)
8. `GlobalExceptionHandler` com `@RestControllerAdvice` e tratamento centralizado
9. Validacao de upload de audio (tamanho, formato, extensao)
10. Queries JPQL agregadas substituindo processamento em memoria
11. Paginacao em `GET /api/transactions`
12. Timeout configurado nos `RestClient` (Groq Whisper e gTTS)
13. `FormatUtils` centralizando formatadores de data e moeda
14. Flyway com `ddl-auto: validate` e migration versionada
15. Healthcheck do gTTS no Docker Compose
16. `GlobalExceptionHandler` substituindo try/catch nos controllers
17. Testes unitarios e de integracao (`ValidationService`, `TransactionService`, `VoiceCommandController`)

## O que Aprendi

- Tool Calling com Spring AI + Groq funciona perfeitamente usando
  o starter OpenAI com `base-url` apontando para o Groq.
- A separacao de responsabilidades entre services de IA e services
  de negocio mantem o codigo testavel e de facil manutencao.
- O RestClient do Spring e suficiente para integracoes HTTP diretas
  sem depender de auto-configuration para cada provedor.
- Docker Compose e suficiente para orquestrar servicos auxiliares
  como banco de dados e TTS.
- Validacoes desacopladas em um service proprio (SRP) evitam que
  regras de negocio fiquem espalhadas pela aplicacao.
- Excecoes especificas permitem tratamento HTTP diferenciado sem
  inspecionar mensagens de erro.
- Queries JPQL agregadas transferem processamento para o banco e
  eliminam a necessidade de `@SuppressWarnings` em Streams.
