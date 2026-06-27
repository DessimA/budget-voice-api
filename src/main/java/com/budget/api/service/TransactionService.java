package com.budget.api.service;

import com.budget.api.domain.Transaction;
import com.budget.api.domain.TransactionCategory;
import com.budget.api.domain.TransactionType;
import com.budget.api.dto.MonthlySummaryResponse;
import com.budget.api.dto.MonthlySummaryResponse.CategorySummary;
import com.budget.api.dto.TransactionResponse;
import com.budget.api.repository.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

        TransactionType transactionType = validationService.parseType(type);
        TransactionCategory transactionCategory = validationService.parseCategory(category);
        LocalDate transactionDate = LocalDate.parse(date, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Transaction transaction = new Transaction(description, amount, transactionType,
                                                  transactionCategory, transactionDate);
        Transaction saved = repository.save(transaction);
        return TransactionResponse.from(saved);
    }

    public List<TransactionResponse> getAllTransactions() {
        return repository.findAllByOrderByTransactionDateDesc()
            .stream()
            .map(TransactionResponse::from)
            .toList();
    }

    public List<TransactionResponse> getTransactionsByPeriod(LocalDate start, LocalDate end) {
        return repository.findByTransactionDateBetween(start, end)
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

        List<Transaction> transactions = repository.findByTransactionDateBetween(start, end);
        Map<TransactionCategory, List<Transaction>> grouped = transactions.stream()
            .collect(Collectors.groupingBy(Transaction::getCategory));

        List<CategorySummary> byCategory = grouped.entrySet().stream()
            .map(entry -> {
                TransactionCategory cat = entry.getKey();
                List<Transaction> txns = entry.getValue();
                BigDecimal total = txns.stream()
                    .map(t -> t.getType() == TransactionType.EXPENSE
                        ? t.getAmount().negate() : t.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                return new CategorySummary(
                    cat.name(),
                    cat.getDescricao(),
                    total.setScale(2, RoundingMode.HALF_EVEN),
                    txns.size()
                );
            })
            .sorted(Comparator.comparing(CategorySummary::categoryDescription))
            .toList();

        return new MonthlySummaryResponse(
            month, year,
            totalIncome.setScale(2, RoundingMode.HALF_EVEN),
            totalExpense.setScale(2, RoundingMode.HALF_EVEN),
            balance.setScale(2, RoundingMode.HALF_EVEN),
            byCategory
        );
    }

    public Map<String, BigDecimal> getBalanceByCategory() {
        List<Transaction> all = repository.findAll();
        return all.stream()
            .collect(Collectors.groupingBy(
                t -> t.getCategory().getDescricao(),
                LinkedHashMap::new,
                Collectors.reducing(
                    BigDecimal.ZERO,
                    t -> t.getType() == TransactionType.EXPENSE
                        ? t.getAmount().negate() : t.getAmount(),
                    BigDecimal::add
                )
            ));
    }
}
