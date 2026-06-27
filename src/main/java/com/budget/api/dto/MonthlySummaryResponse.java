package com.budget.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record MonthlySummaryResponse(
    int month,
    int year,
    BigDecimal totalIncome,
    BigDecimal totalExpense,
    BigDecimal balance,
    List<CategorySummary> byCategory
) {
    public record CategorySummary(
        String category,
        String categoryDescription,
        BigDecimal totalAmount,
        long transactionCount
    ) {
    }
}
