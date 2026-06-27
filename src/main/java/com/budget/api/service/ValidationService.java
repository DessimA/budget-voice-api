package com.budget.api.service;

import com.budget.api.domain.TransactionCategory;
import com.budget.api.domain.TransactionType;
import com.budget.api.exception.BusinessException;
import com.budget.api.util.FormatUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Service;

@Service
public final class ValidationService {

    public void validateTransaction(String description, BigDecimal amount,
                                    String type, String category, String date) {
        if (description == null || description.isBlank()) {
            throw new BusinessException("Descrição da transação é obrigatória");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Valor deve ser positivo e maior que zero");
        }
        parseType(type);
        parseCategory(category);
        if (date == null || date.isBlank()) {
            throw new BusinessException("Data é obrigatória");
        }
        LocalDate parsedDate;
        try {
            parsedDate = LocalDate.parse(date, FormatUtils.DATE_STORAGE);
        } catch (DateTimeParseException e) {
            throw new BusinessException("Formato de data inválido. Use yyyy-MM-dd");
        }
        if (parsedDate.isAfter(LocalDate.now().plusDays(1))) {
            throw new BusinessException("Data não pode ser futura além de amanhã");
        }
    }

    public TransactionCategory parseCategory(String category) {
        if (category == null || category.isBlank()) {
            return TransactionCategory.OUTROS;
        }
        try {
            return TransactionCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TransactionCategory.OUTROS;
        }
    }

    public TransactionType parseType(String type) {
        if (type == null || type.isBlank()) {
            throw new BusinessException("Tipo de transação é obrigatório");
        }
        try {
            return TransactionType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Tipo inválido. Use INCOME ou EXPENSE");
        }
    }
}
