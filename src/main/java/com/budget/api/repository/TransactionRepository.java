package com.budget.api.repository;

import com.budget.api.domain.Transaction;
import com.budget.api.domain.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByTransactionDateBetween(LocalDate start, LocalDate end);

    List<Transaction> findByTransactionDateBetweenOrderByTransactionDateDesc(
        LocalDate start, LocalDate end);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.type = :type "
         + "AND t.transactionDate BETWEEN :start AND :end")
    Optional<BigDecimal> sumAmountByTypeAndDateBetween(
        TransactionType type, LocalDate start, LocalDate end);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.type = :type")
    Optional<BigDecimal> sumAmountByType(TransactionType type);

    Page<Transaction> findAllByOrderByTransactionDateDesc(Pageable pageable);

    @Query("""
        SELECT t.category, SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE -t.amount END)
        FROM Transaction t
        GROUP BY t.category
        ORDER BY t.category
        """)
    List<Object[]> sumAmountGroupedByCategory();

    @Query("""
        SELECT t.category, SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE -t.amount END),
               COUNT(t)
        FROM Transaction t
        WHERE t.transactionDate BETWEEN :start AND :end
        GROUP BY t.category
        ORDER BY t.category
        """)
    List<Object[]> sumAmountGroupedByCategoryBetween(
        @Param("start") LocalDate start, @Param("end") LocalDate end);
}
