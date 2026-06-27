# FormatUtils

## Propósito

Classe utilitária que centraliza formatadores usados em múltiplos
serviços da aplicação. Evita duplicação de constantes e garante
consistência na formatação de datas e valores monetários.

## Constantes e Métodos

- `DATE_STORAGE`: `DateTimeFormatter` no padrão `yyyy-MM-dd`, usado
  para persistência e parsing de datas.
- `DATE_DISPLAY`: `DateTimeFormatter` no padrão `dd/MM/yyyy`, usado
  para exibição em respostas de ferramentas e logs.
- `brazilianCurrency()`: retorna uma nova instância de `NumberFormat`
  configurada para o locale `pt_BR` (R$). O método (não constante)
  garante thread-safety, já que `NumberFormat` não é thread-safe.

## Por que centralizar?

Antes da centralização, `ValidationService` e `BudgetTools` definiam
seus próprios `DateTimeFormatter` com o mesmo padrão. A centralização
garante que qualquer alteração de formato seja refletida em todos os
pontos de uso sem necessidade de buscar referências duplicadas.
