# Repository

## Queries Implementadas

### `findByTransactionDateBetween`
Busca transações por período. Usada por serviços que precisam filtrar
por intervalo de datas.

### `findByTransactionDateBetweenOrderByTransactionDateDesc`
Mesmo filtro com ordenação descendente. Evita ordenação em memória.

### `sumAmountByTypeAndDateBetween`
Soma de valores por tipo e período via JPQL. Usa `SUM` nativo do
banco para agregação eficiente.

### `sumAmountByType`
Soma total de um tipo (INCOME ou EXPENSE) sem filtro de data.

### `findAllByOrderByTransactionDateDesc` (paginada)
Lista paginada ordenada por data. Evita carregar todas as transações.

### `sumAmountGroupedByCategory`
Agrupa saldo por categoria usando `CASE WHEN` no SQL. Substitui o
agrupamento em memória que exigia carregar todas as transações.

### `sumAmountGroupedByCategoryBetween`
Mesmo agrupamento com filtro de período. Usada pelo relatório mensal
para evitar carregar transações do mês inteiro em memória.

## Por que JPQL em vez de Java?

As queries agregadas (`SUM`, `GROUP BY`, `CASE WHEN`) transferem o
processamento para o banco de dados, que é otimizado para isso. A
abordagem anterior carregava todas as transações em memória e fazia
agrupamento com Streams, o que é ineficiente para grandes volumes.

## Impacto de Performance

- `getBalanceByCategory`: antes carregava todas as transações via
  `findAll()`; agora executa uma única query agregada.
- `getMonthlySummary`: antes carregava transações do mês e agrupava
  em Streams; agora usa query agregada com `GROUP BY`.
- `getAllTransactions`: agora é paginada, evitando carregar todas
  as transações em uma única listagem.
