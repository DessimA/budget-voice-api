package com.budget.api.domain;

public enum TransactionType {
    INCOME("Entrada"),
    EXPENSE("Saída");

    private final String descricao;

    TransactionType(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
