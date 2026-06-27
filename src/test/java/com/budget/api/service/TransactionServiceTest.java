package com.budget.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.budget.api.domain.Transaction;
import com.budget.api.domain.TransactionCategory;
import com.budget.api.domain.TransactionType;
import com.budget.api.dto.MonthlySummaryResponse;
import com.budget.api.dto.TransactionResponse;
import com.budget.api.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository repository;

    @Mock
    private ValidationService validationService;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(repository, validationService);
    }

    @Test
    void createTransactionDeveSalvarERetornarResponse() {
        Transaction saved = new Transaction("Salário", new BigDecimal("5000"),
            TransactionType.INCOME, TransactionCategory.SALARIO, LocalDate.of(2026, 6, 27));
        saved.setId(UUID.randomUUID());

        when(repository.save(any(Transaction.class))).thenReturn(saved);

        TransactionResponse response = transactionService.createTransaction(
            "Salário", new BigDecimal("5000"), "INCOME", "SALARIO", "2026-06-27");

        assertEquals("Salário", response.description());
        assertEquals(new BigDecimal("5000"), response.amount());
        assertEquals("INCOME", response.type());
        verify(repository).save(any(Transaction.class));
    }

    @Test
    void getCurrentBalanceDeveSubtrairDespesasDeReceitas() {
        when(repository.sumAmountByType(TransactionType.INCOME))
            .thenReturn(Optional.of(new BigDecimal("10000")));
        when(repository.sumAmountByType(TransactionType.EXPENSE))
            .thenReturn(Optional.of(new BigDecimal("3000")));

        BigDecimal balance = transactionService.getCurrentBalance();

        assertEquals(new BigDecimal("7000"), balance);
    }

    @Test
    void getMonthlySummaryDeveRetornarTotaisCorretos() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);

        when(repository.sumAmountByTypeAndDateBetween(TransactionType.INCOME, start, end))
            .thenReturn(Optional.of(new BigDecimal("5000")));
        when(repository.sumAmountByTypeAndDateBetween(TransactionType.EXPENSE, start, end))
            .thenReturn(Optional.of(new BigDecimal("2000")));
        when(repository.sumAmountGroupedByCategoryBetween(start, end))
            .thenReturn(java.util.List.of());

        MonthlySummaryResponse summary = transactionService.getMonthlySummary(6, 2026);

        assertEquals(6, summary.month());
        assertEquals(2026, summary.year());
        assertEquals(new BigDecimal("5000"), summary.totalIncome());
        assertEquals(new BigDecimal("2000"), summary.totalExpense());
        assertEquals(new BigDecimal("3000"), summary.balance());
    }
}
