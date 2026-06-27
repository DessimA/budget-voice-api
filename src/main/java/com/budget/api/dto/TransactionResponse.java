package com.budget.api.dto;

import com.budget.api.domain.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
    UUID id,
    String description,
    BigDecimal amount,
    String type,
    String typeDescription,
    String category,
    String categoryDescription,
    LocalDate transactionDate,
    LocalDateTime createdAt
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
            transaction.getId(),
            transaction.getDescription(),
            transaction.getAmount(),
            transaction.getType().name(),
            transaction.getType().getDescricao(),
            transaction.getCategory().name(),
            transaction.getCategory().getDescricao(),
            transaction.getTransactionDate(),
            transaction.getCreatedAt()
        );
    }
}
