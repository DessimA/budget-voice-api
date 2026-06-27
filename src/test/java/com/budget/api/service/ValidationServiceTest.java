package com.budget.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.budget.api.domain.TransactionCategory;
import com.budget.api.exception.BusinessException;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidationServiceTest {

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService();
    }

    @Test
    void deveLancarExcecaoParaDescricaoNula() {
        assertThrows(BusinessException.class, () ->
            validationService.validateTransaction(null, BigDecimal.TEN, "INCOME", "SALARIO", "2026-06-27"));
    }

    @Test
    void deveLancarExcecaoParaDescricaoEmBranco() {
        assertThrows(BusinessException.class, () ->
            validationService.validateTransaction("", BigDecimal.TEN, "INCOME", "SALARIO", "2026-06-27"));
    }

    @Test
    void deveLancarExcecaoParaValorNulo() {
        assertThrows(BusinessException.class, () ->
            validationService.validateTransaction("Teste", null, "INCOME", "SALARIO", "2026-06-27"));
    }

    @Test
    void deveLancarExcecaoParaValorZero() {
        assertThrows(BusinessException.class, () ->
            validationService.validateTransaction("Teste", BigDecimal.ZERO, "INCOME", "SALARIO", "2026-06-27"));
    }

    @Test
    void deveLancarExcecaoParaValorNegativo() {
        assertThrows(BusinessException.class, () ->
            validationService.validateTransaction("Teste", new BigDecimal("-50"), "INCOME", "SALARIO", "2026-06-27"));
    }

    @Test
    void deveLancarExcecaoParaTipoInvalido() {
        assertThrows(BusinessException.class, () ->
            validationService.validateTransaction("Teste", BigDecimal.TEN, "INVALIDO", "SALARIO", "2026-06-27"));
    }

    @Test
    void deveRetornarOutrosParaCategoriaInvalida() {
        assertEquals(TransactionCategory.OUTROS,
            validationService.parseCategory("CATEGORIA_INEXISTENTE"));
    }

    @Test
    void deveLancarExcecaoParaDataFuturaAlemDeAmanha() {
        assertThrows(BusinessException.class, () ->
            validationService.validateTransaction("Teste", BigDecimal.TEN, "INCOME", "SALARIO", "2099-01-01"));
    }

    @Test
    void deveLancarExcecaoParaDataComFormatoInvalido() {
        assertThrows(BusinessException.class, () ->
            validationService.validateTransaction("Teste", BigDecimal.TEN, "INCOME", "SALARIO", "27/06/2026"));
    }
}
