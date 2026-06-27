# Configuração

## Flyway e Migrations

### Por que `ddl-auto: validate` em vez de `update`?

O modo `update` do Hibernate cria/altera tabelas automaticamente com
base nas entidades JPA. Isso é prático em desenvolvimento mas perigoso
em cenários onde o schema precisa ser versionado. Com `validate`, o
Hibernate apenas verifica se as tabelas existem e são compatíveis com
as entidades, sem alterá-las. O versionamento fica a cargo do Flyway.

### Por que Flyway?

Flyway aplica migrations SQL de forma ordenada e incremental. Cada
migration é um arquivo SQL com número sequencial (V1, V2, ...) que
só é executado uma vez. Isso garante que:

1. O schema é reproduzível em qualquer ambiente.
2. Alterações são rastreadas no Git.
3. Rollbacks podem ser feitos com scripts manuais.
4. Times diferentes trabalham com o mesmo schema.

### Índices Criados

- `idx_transactions_date`: acelera consultas por período (relatório
  mensal, listagem por data).
- `idx_transactions_type`: acelera consultas de saldo agregado por tipo.
- `idx_transactions_date_type`: índice composto para queries que
  filtram por tipo e período simultaneamente (usado em
  `sumAmountByTypeAndDateBetween`).

## Multipart Upload

O limite de 25MB para upload de arquivos corresponde ao limite máximo
suportado pela API Whisper do Groq. O `max-request-size` de 26MB é
ligeiramente superior para acomodar overhead do multipart form.
