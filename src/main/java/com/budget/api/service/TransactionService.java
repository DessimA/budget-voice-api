package com.budget.api.service;

import com.budget.api.domain.Transaction;
import com.budget.api.domain.TransactionCategory;
import com.budget.api.domain.TransactionType;
import com.budget.api.dto.MonthlySummaryResponse;
import com.budget.api.dto.MonthlySummaryResponse.CategorySummary;
import com.budget.api.dto.TransactionResponse;
import com.budget.api.exception.BusinessException;
import com.budget.api.repository.TransactionRepository;
import com.budget.api.util.FormatUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public final class TransactionService {

    private final TransactionRepository repository;
    private final ValidationService validationService;

    public TransactionService(TransactionRepository repository, ValidationService validationService) {
        this.repository = repository;
        this.validationService = validationService;
    }

    public TransactionResponse createTransaction(String description, BigDecimal amount,
                                                  String type, String category, String date) {
        validationService.validateTransaction(description, amount, type, category, date);

        TransactionType transactionType = TransactionType.valueOf(type.toUpperCase());
        TransactionCategory transactionCategory = TransactionCategory.valueOf(category.toUpperCase());
        LocalDate transactionDate = LocalDate.parse(date, FormatUtils.DATE_STORAGE);

        LocalDateTime since = LocalDateTime.now().minusMinutes(1);
        if (repository.existsDuplicate(description, amount, transactionType, transactionCategory,
                                       transactionDate, since)) {
            throw new BusinessException("Transação duplicada detectada. Ignorando requisição repetida.");
        }

        Transaction transaction = new Transaction(description, amount, transactionType,
                                                  transactionCategory, transactionDate);
        Transaction saved = repository.save(transaction);
        return TransactionResponse.from(saved);
    }

    public Page<TransactionResponse> getAllTransactions(Pageable pageable) {
        return repository.findAllByOrderByTransactionDateDesc(pageable)
            .map(TransactionResponse::from);
    }

    public List<TransactionResponse> getTransactionsByPeriod(LocalDate start, LocalDate end) {
        return repository.findByTransactionDateBetweenOrderByTransactionDateDesc(start, end)
            .stream()
            .map(TransactionResponse::from)
            .toList();
    }

    public BigDecimal getCurrentBalance() {
        BigDecimal totalIncome = repository.sumAmountByType(TransactionType.INCOME).orElse(BigDecimal.ZERO);
        BigDecimal totalExpense = repository.sumAmountByType(TransactionType.EXPENSE).orElse(BigDecimal.ZERO);
        return totalIncome.subtract(totalExpense);
    }

    public MonthlySummaryResponse getMonthlySummary(int month, int year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        BigDecimal totalIncome = repository
            .sumAmountByTypeAndDateBetween(TransactionType.INCOME, start, end)
            .orElse(BigDecimal.ZERO);
        BigDecimal totalExpense = repository
            .sumAmountByTypeAndDateBetween(TransactionType.EXPENSE, start, end)
            .orElse(BigDecimal.ZERO);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        List<Object[]> categoryData = repository.sumAmountGroupedByCategoryBetween(start, end);
        List<CategorySummary> byCategory = new ArrayList<>();
        for (Object[] row : categoryData) {
            TransactionCategory cat = (TransactionCategory) row[0];
            BigDecimal total = (BigDecimal) row[1];
            Long count = (Long) row[2];
            byCategory.add(new CategorySummary(
                cat.name(),
                cat.getDescricao(),
                total.setScale(2, RoundingMode.HALF_EVEN),
                count
            ));
        }

        return new MonthlySummaryResponse(
            month, year,
            totalIncome.setScale(2, RoundingMode.HALF_EVEN),
            totalExpense.setScale(2, RoundingMode.HALF_EVEN),
            balance.setScale(2, RoundingMode.HALF_EVEN),
            byCategory
        );
    }

    public Map<String, BigDecimal> getBalanceByCategory() {
        List<Object[]> categoryData = repository.sumAmountGroupedByCategory();
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Object[] row : categoryData) {
            TransactionCategory cat = (TransactionCategory) row[0];
            BigDecimal total = (BigDecimal) row[1];
            result.put(cat.getDescricao(), total.setScale(2, RoundingMode.HALF_EVEN));
        }
        return result;
    }
}
