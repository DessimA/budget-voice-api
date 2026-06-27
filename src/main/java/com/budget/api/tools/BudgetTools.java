package com.budget.api.tools;

import com.budget.api.dto.TransactionResponse;
import com.budget.api.service.TransactionService;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public final class BudgetTools {

    private final TransactionService transactionService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public BudgetTools(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    private static NumberFormat currencyFormat() {
        return NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    }

    @Tool(description = "Registers a new expense transaction in the budget")
    public String registerExpense(
            @ToolParam(description = "Short description of the expense") String description,
            @ToolParam(description = "Amount spent, positive number") BigDecimal amount,
            @ToolParam(description = "Category: ALIMENTACAO, TRANSPORTE, MORADIA, SAUDE, LAZER, EDUCACAO, OUTROS") String category,
            @ToolParam(description = "Date in yyyy-MM-dd format, use today if not specified") String date) {
        try {
            if (date == null || date.isBlank()) {
                date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
            TransactionResponse response = transactionService.createTransaction(
                description, amount, "EXPENSE", category, date);
            return String.format("Despesa registrada com sucesso! %s - %s: %s em %s",
                response.description(),
                response.categoryDescription(),
                currencyFormat().format(response.amount()),
                response.transactionDate().format(DATE_FORMATTER));
        } catch (IllegalArgumentException e) {
            return "Erro ao registrar despesa: " + e.getMessage();
        } catch (Exception e) {
            return "Erro inesperado ao registrar despesa. Tente novamente.";
        }
    }

    @Tool(description = "Registers a new income transaction in the budget")
    public String registerIncome(
            @ToolParam(description = "Short description of the income") String description,
            @ToolParam(description = "Amount received, positive number") BigDecimal amount,
            @ToolParam(description = "Category: SALARIO, INVESTIMENTO, OUTROS") String category,
            @ToolParam(description = "Date in yyyy-MM-dd format, use today if not specified") String date) {
        try {
            if (date == null || date.isBlank()) {
                date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
            TransactionResponse response = transactionService.createTransaction(
                description, amount, "INCOME", category, date);
            return String.format("Entrada registrada com sucesso! %s - %s: %s em %s",
                response.description(),
                response.categoryDescription(),
                currencyFormat().format(response.amount()),
                response.transactionDate().format(DATE_FORMATTER));
        } catch (IllegalArgumentException e) {
            return "Erro ao registrar entrada: " + e.getMessage();
        } catch (Exception e) {
            return "Erro inesperado ao registrar entrada. Tente novamente.";
        }
    }

    @Tool(description = "Returns the current financial balance: total income minus total expenses")
    public String getCurrentBalance() {
        try {
            BigDecimal balance = transactionService.getCurrentBalance();
            return String.format("Seu saldo atual é de %s",
                currencyFormat().format(balance));
        } catch (Exception e) {
            return "Erro ao consultar saldo. Tente novamente.";
        }
    }

    @Tool(description = "Lists transactions from the last N days. Default 30 if not specified by user")
    public String listRecentTransactions(
            @ToolParam(description = "Number of days to look back") int days) {
        try {
            if (days <= 0) {
                days = 30;
            }
            LocalDate startDate = LocalDate.now().minusDays(days);
            List<TransactionResponse> transactions = transactionService.getTransactionsByPeriod(startDate, LocalDate.now());

            if (transactions.isEmpty()) {
                return "Nenhuma transação encontrada nos últimos " + days + " dias.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Transações dos últimos ").append(days).append(" dias:\n");
            int limit = Math.min(transactions.size(), 20);
            for (int i = 0; i < limit; i++) {
                TransactionResponse t = transactions.get(i);
                String signal = t.type().equals("INCOME") ? "+" : "-";
                sb.append(String.format("%s | %s | %s | %s %s%n",
                    t.transactionDate().format(DATE_FORMATTER),
                    t.description(),
                    t.categoryDescription(),
                    signal,
                    currencyFormat().format(t.amount())));
            }
            if (transactions.size() > 20) {
                sb.append("... e mais ").append(transactions.size() - 20).append(" transações.");
            }
            return sb.toString();
        } catch (Exception e) {
            return "Erro ao listar transações. Tente novamente.";
        }
    }

    @Tool(description = "Returns financial summary for a specific month and year. Use current if not specified")
    public String getMonthlySummary(
            @ToolParam(description = "Month number 1-12") int month,
            @ToolParam(description = "Full year like 2025") int year) {
        try {
            if (month <= 0 || month > 12) {
                YearMonth current = YearMonth.now();
                month = current.getMonthValue();
                year = current.getYear();
            }
            if (year <= 0) {
                year = YearMonth.now().getYear();
            }
            var summary = transactionService.getMonthlySummary(month, year);

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Resumo financeiro de %s/%d:%n",
                String.format("%02d", month), year));
            sb.append(String.format("Total de entradas: %s%n",
                currencyFormat().format(summary.totalIncome())));
            sb.append(String.format("Total de saídas: %s%n",
                currencyFormat().format(summary.totalExpense())));
            sb.append(String.format("Saldo: %s%n",
                currencyFormat().format(summary.balance())));
            sb.append("Por categoria:\n");
            for (var cat : summary.byCategory()) {
                sb.append(String.format("  %s: %s (%d transações)%n",
                    cat.categoryDescription(),
                    currencyFormat().format(cat.totalAmount()),
                    cat.transactionCount()));
            }
            return sb.toString();
        } catch (Exception e) {
            return "Erro ao gerar resumo mensal. Tente novamente.";
        }
    }

    @Tool(description = "Returns spending summary grouped by category")
    public String getBalanceByCategory() {
        try {
            Map<String, BigDecimal> byCategory = transactionService.getBalanceByCategory();
            if (byCategory.isEmpty()) {
                return "Nenhuma transação encontrada para análise por categoria.";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Saldo por categoria:\n");
            byCategory.forEach((cat, value) ->
                sb.append(String.format("  %s: %s%n", cat, currencyFormat().format(value))));
            return sb.toString();
        } catch (Exception e) {
            return "Erro ao consultar saldo por categoria. Tente novamente.";
        }
    }
}
