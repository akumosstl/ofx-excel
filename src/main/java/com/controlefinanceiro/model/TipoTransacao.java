package com.controlefinanceiro.model;

public enum TipoTransacao {
    CREDIT("Crédito"),
    DEBIT("Débito");

    private final String descricao;

    TipoTransacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
