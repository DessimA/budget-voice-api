package com.budget.api.tools;

import com.budget.api.dto.TransactionResponse;
import com.budget.api.service.TransactionService;
import com.budget.api.util.FormatUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public final class BudgetTools {

    private final TransactionService transactionService;

    public BudgetTools(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    private String fixDateYear(String date) {
        if (date == null || date.isBlank()) return date;
        LocalDate parsed = LocalDate.parse(date, FormatUtils.DATE_STORAGE);
        int currentYear = LocalDate.now().getYear();
        if (parsed.getYear() != currentYear) {
            return parsed.withYear(currentYear).format(FormatUtils.DATE_STORAGE);
        }
        return date;
    }

    private BigDecimal parseAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            throw new IllegalArgumentException("Valor não informado");
        }
        return new BigDecimal(amount.trim().replace(",", "."));
    }

    @Tool(description = "Registers a new expense transaction in the budget")
    public String registerExpense(
            @ToolParam(description = "Short description of the expense") String description,
            @ToolParam(description = "Amount spent, positive number") String amount,
            @ToolParam(description = "Category: ALIMENTACAO, TRANSPORTE, MORADIA, SAUDE, LAZER, EDUCACAO, OUTROS") String category,
            @ToolParam(required = false, description = "Date in yyyy-MM-dd format, use today if not specified") String date) {
        try {
            date = fixDateYear(date);
            if (date == null || date.isBlank()) {
                date = LocalDate.now().format(FormatUtils.DATE_STORAGE);
            }
            BigDecimal parsedAmount = parseAmount(amount);
            TransactionResponse response = transactionService.createTransaction(
                description, parsedAmount, "EXPENSE", category, date);
            return String.format("Despesa registrada com sucesso! %s - %s: %s em %s",
                response.description(),
                response.categoryDescription(),
                FormatUtils.brazilianCurrency().format(response.amount()),
                response.transactionDate().format(FormatUtils.DATE_DISPLAY));
        } catch (IllegalArgumentException e) {
            return "Erro ao registrar despesa: " + e.getMessage();
        } catch (Exception e) {
            return "Erro inesperado ao registrar despesa. Tente novamente.";
        }
    }

    @Tool(description = "Registers a new income transaction in the budget")
    public String registerIncome(
            @ToolParam(description = "Short description of the income") String description,
            @ToolParam(description = "Amount received, positive number") String amount,
            @ToolParam(description = "Category: SALARIO, INVESTIMENTO, OUTROS") String category,
            @ToolParam(required = false, description = "Date in yyyy-MM-dd format, use today if not specified") String date) {
        try {
            date = fixDateYear(date);
            if (date == null || date.isBlank()) {
                date = LocalDate.now().format(FormatUtils.DATE_STORAGE);
            }
            BigDecimal parsedAmount = parseAmount(amount);
            TransactionResponse response = transactionService.createTransaction(
                description, parsedAmount, "INCOME", category, date);
            return String.format("Entrada registrada com sucesso! %s - %s: %s em %s",
                response.description(),
                response.categoryDescription(),
                FormatUtils.brazilianCurrency().format(response.amount()),
                response.transactionDate().format(FormatUtils.DATE_DISPLAY));
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
                FormatUtils.brazilianCurrency().format(balance));
        } catch (Exception e) {
            return "Erro ao consultar saldo. Tente novamente.";
        }
    }

    @Tool(description = "Lists transactions from the last N days. Default 30 if not specified by user")
    public String listRecentTransactions(
            @ToolParam(description = "Number of days to look back") String days) {
        try {
            int daysInt;
            try {
                daysInt = Integer.parseInt(days.trim());
            } catch (Exception e) {
                daysInt = 30;
            }
            if (daysInt <= 0) {
                daysInt = 30;
            }
            LocalDate startDate = LocalDate.now().minusDays(daysInt);
            List<TransactionResponse> transactions = transactionService.getTransactionsByPeriod(startDate, LocalDate.now());

            if (transactions.isEmpty()) {
                return "Nenhuma transação encontrada nos últimos " + daysInt + " dias.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Transações dos últimos ").append(daysInt).append(" dias:\n");
            int limit = Math.min(transactions.size(), 20);
            for (int i = 0; i < limit; i++) {
                TransactionResponse t = transactions.get(i);
                String signal = t.type().equals("INCOME") ? "+" : "-";
                sb.append(String.format("%s | %s | %s | %s %s%n",
                    t.transactionDate().format(FormatUtils.DATE_DISPLAY),
                    t.description(),
                    t.categoryDescription(),
                    signal,
                    FormatUtils.brazilianCurrency().format(t.amount())));
            }
            if (transactions.size() > 20) {
                sb.append("... e mais ").append(transactions.size() - 20).append(" transações.");
            }
            return sb.toString();
        } catch (Exception e) {
            return "Erro ao listar transações. Tente novamente.";
        }
    }

    @Tool(description = "Returns financial summary for a specific month and year: total income, total expenses, balance, and breakdown by category with amounts and transaction counts. Use current month if not specified.")
    public String getMonthlySummary(
            @ToolParam(description = "Month number 1-12") String month,
            @ToolParam(description = "Full year like 2025") String year) {
        try {
            int monthInt;
            int yearInt;
            try {
                monthInt = Integer.parseInt(month.trim());
            } catch (Exception e) {
                monthInt = 0;
            }
            try {
                yearInt = Integer.parseInt(year.trim());
            } catch (Exception e) {
                yearInt = 0;
            }
            if (monthInt <= 0 || monthInt > 12) {
                YearMonth current = YearMonth.now();
                monthInt = current.getMonthValue();
                yearInt = current.getYear();
            }
            if (yearInt <= 0) {
                yearInt = YearMonth.now().getYear();
            }
            var summary = transactionService.getMonthlySummary(monthInt, yearInt);

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Resumo financeiro de %s/%d:%n",
                String.format("%02d", monthInt), yearInt));
            sb.append(String.format("Total de entradas: %s%n",
                FormatUtils.brazilianCurrency().format(summary.totalIncome())));
            sb.append(String.format("Total de saídas: %s%n",
                FormatUtils.brazilianCurrency().format(summary.totalExpense())));
            sb.append(String.format("Saldo: %s%n",
                FormatUtils.brazilianCurrency().format(summary.balance())));
            sb.append("Por categoria:\n");
            for (var cat : summary.byCategory()) {
                sb.append(String.format("  %s: %s (%d transações)%n",
                    cat.categoryDescription(),
                    FormatUtils.brazilianCurrency().format(cat.totalAmount()),
                    cat.transactionCount()));
            }
            return sb.toString();
        } catch (Exception e) {
            return "Erro ao gerar resumo mensal. Tente novamente.";
        }
    }

    @Tool(description = "Returns net balance (income minus expenses) grouped by category")
    public String getBalanceByCategory() {
        try {
            Map<String, BigDecimal> byCategory = transactionService.getBalanceByCategory();
            if (byCategory.isEmpty()) {
                return "Nenhuma transação encontrada para análise por categoria.";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Saldo por categoria:\n");
            byCategory.forEach((cat, value) ->
                sb.append(String.format("  %s: %s%n", cat, FormatUtils.brazilianCurrency().format(value))));
            return sb.toString();
        } catch (Exception e) {
            return "Erro ao consultar saldo por categoria. Tente novamente.";
        }
    }
}
