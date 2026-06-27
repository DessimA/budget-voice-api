# Testes

## Estratégia

### Unitários (Mockito)
- `ValidationServiceTest`: testa regras de validação sem dependências
  externas. Cada cenário de erro é um teste isolado.
- `TransactionServiceTest`: testa lógica de negócio com repositório
  mockado. Verifica integração entre service e validação, cálculo de
  saldo e resumo mensal.

### Web (MockMvc)
- `VoiceCommandControllerTest`: testa camada web com serviços mockados.
  Verifica validação de upload, código de erro para arquivos grandes e
  health check.

## O que é testado em cada camada

- **ValidationService**: todas as regras de validação (descrição, valor,
  tipo, categoria, data). Comportamento de fallback para categoria inválida.
- **TransactionService**: fluxo completo de criação, cálculo de saldo,
  geração de resumo mensal.
- **VoiceCommandController**: validação de entrada (arquivo ausente,
  tamanho excessivo), endpoint de health check.

## Por que esta abordagem?

Testes unitários são rápidos e não exigem infraestrutura (banco, Docker).
Testes `@WebMvcTest` validam a camada web sem iniciar o contexto completo
do Spring. A combinação cobre a maior parte dos cenários de erro sem
depender de serviços externos (Groq, Whisper, Coqui).
