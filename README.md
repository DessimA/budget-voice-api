# Budget Voice API

API REST que aceita comandos de voz em português para gerenciar
finanças pessoais. Usa Spring AI com Tool Calling para entender a
intenção do usuário e executar operações reais no banco de dados.
Stack 100% gratuita: Groq (LLM + transcrição) e Coqui TTS (síntese
de voz open source em Docker).

## Pré-requisitos

- Docker 24+
- Docker Compose v2+
- Conta gratuita no [Groq Console](https://console.groq.com) (sem cartão de crédito)

## Como Executar

```bash
git clone <url>
cd budget-voice-api

cp .env.example .env
# Editar .env e inserir GROQ_API_KEY=gsk_sua_chave

docker compose up --build
```

A API estará disponível em `http://localhost:8080` assim que o
PostgreSQL e o Spring Boot subirem. O Coqui TTS sobe em paralelo e
pode levar 2 a 5 minutos no primeiro uso para baixar o modelo de voz
em português. Nas execuções seguintes o modelo já está em cache no
volume Docker e a inicialização é quase imediata.

## Como Testar

```bash
# Verificar saúde da API
curl http://localhost:8080/api/voice/health

# Enviar arquivo de áudio
curl -X POST -F "audio=@meuaudio.mp3" http://localhost:8080/api/voice/command

# Consultar transações
curl http://localhost:8080/api/transactions

# Consultar saldo
curl http://localhost:8080/api/transactions/balance

# Resumo do mês
curl http://localhost:8080/api/transactions/summary/2025/6

# Resposta em áudio (após Coqui inicializar)
curl -X POST -F "audio=@meuaudio.mp3" http://localhost:8080/api/voice/command/audio --output resposta.wav
```

## Exemplos de Comandos de Voz

- "Gastei cinquenta reais em almoço hoje"
- "Recebi meu salário de cinco mil reais"
- "Qual é meu saldo atual?"
- "Como foram meus gastos esse mês?"
- "Mostre meus gastos dos últimos 7 dias"
- "Quanto gastei em cada categoria?"

## Tecnologias

| Tecnologia | Versão | Papel |
|---|---|---|
| Java | 21 | Runtime |
| Spring Boot | 3.3.x | Framework |
| Spring AI | 1.0.0 | Integração Groq LLM |
| PostgreSQL | 16-alpine | Banco de dados |
| Groq Llama 3.3 70B | llama-3.3-70b-versatile | LLM + Tool Calling |
| Groq Whisper | whisper-large-v3-turbo | Transcrição |
| Coqui TTS | ghcr.io/coqui-ai/tts-cpu | Síntese de voz |

## Custo de Operação

| Recurso | Provedor | Custo |
|---|---|---|
| LLM com Tool Calling | Groq (Llama 3.3 70B) | Gratuito |
| Transcrição de áudio | Groq Whisper | Gratuito |
| Síntese de voz | Coqui TTS (Docker) | Gratuito |
| Banco de dados | PostgreSQL (Docker) | Gratuito |

## Melhorias Implementadas

1. `TransactionCategory` enum com 9 categorias para classificação
2. `ValidationService` desacoplado com regras de negócio
3. Tool `getMonthlySummary` para relatórios mensais por voz
4. Tool `getBalanceByCategory` para consulta por categoria
5. Campos de auditoria `createdAt` e `updatedAt` na entidade
6. `MonthlySummaryResponse` com breakdown completo por categoria

## O que Aprendi

- Tool Calling com Spring AI + Groq funciona perfeitamente usando
  o starter OpenAI com `base-url` apontando para o Groq.
- A separação de responsabilidades entre services de IA e services
  de negócio mantém o código testável e de fácil manutenção.
- O RestClient do Spring é suficiente para integrações HTTP diretas
  sem depender de auto-configuration para cada provedor.
- Docker Compose com volumes de cache acelera significativamente
  inicializações de modelos de ML como o Coqui TTS.
- Validações desacopladas em um service próprio (SRP) evitam que
  regras de negócio fiquem espalhadas pela aplicação.
