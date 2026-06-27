package com.budget.api.repository;

import com.budget.api.domain.Transaction;
import com.budget.api.domain.TransactionCategory;
import com.budget.api.domain.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByTransactionDateBetween(LocalDate start, LocalDate end);

    List<Transaction> findByType(TransactionType type);

    List<Transaction> findByCategory(TransactionCategory category);

    List<Transaction> findByTypeAndTransactionDateBetween(
        TransactionType type, LocalDate start, LocalDate end);

    List<Transaction> findByCategoryAndTransactionDateBetween(
        TransactionCategory category, LocalDate start, LocalDate end);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.type = :type "
         + "AND t.transactionDate BETWEEN :start AND :end")
    Optional<BigDecimal> sumAmountByTypeAndDateBetween(
        TransactionType type, LocalDate start, LocalDate end);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.type = :type")
    Optional<BigDecimal> sumAmountByType(TransactionType type);

    List<Transaction> findAllByOrderByTransactionDateDesc();
}
