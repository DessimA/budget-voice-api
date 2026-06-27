package com.budget.api.domain;

public enum TransactionCategory {
    ALIMENTACAO("Alimentação"),
    TRANSPORTE("Transporte"),
    MORADIA("Moradia"),
    SAUDE("Saúde"),
    LAZER("Lazer"),
    EDUCACAO("Educação"),
    SALARIO("Salário"),
    INVESTIMENTO("Investimento"),
    OUTROS("Outros");

    private final String descricao;

    TransactionCategory(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
