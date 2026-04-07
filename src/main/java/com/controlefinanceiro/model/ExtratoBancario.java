package com.controlefinanceiro.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ExtratoBancario {
    private String banco;
    private String conta;
    private LocalDate dataInicial;
    private LocalDate dataFinal;
    private BigDecimal saldoInicial;
    private BigDecimal saldoFinal;
    private List<Transacao> transacoes;

    public ExtratoBancario() {
        this.transacoes = new ArrayList<>();
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public String getConta() {
        return conta;
    }

    public void setConta(String conta) {
        this.conta = conta;
    }

    public LocalDate getDataInicial() {
        return dataInicial;
    }

    public void setDataInicial(LocalDate dataInicial) {
        this.dataInicial = dataInicial;
    }

    public LocalDate getDataFinal() {
        return dataFinal;
    }

    public void setDataFinal(LocalDate dataFinal) {
        this.dataFinal = dataFinal;
    }

    public BigDecimal getSaldoFinal() {
        return saldoFinal;
    }

    public void setSaldoFinal(BigDecimal saldoFinal) {
        this.saldoFinal = saldoFinal;
    }

    public BigDecimal getSaldoInicial() {
        return saldoInicial;
    }

    public void setSaldoInicial(BigDecimal saldoInicial) {
        this.saldoInicial = saldoInicial;
    }

    public void calcularSaldoInicial() {
        BigDecimal totalCreditos = getTotalCreditos();
        BigDecimal totalDebitos = getTotalDebitos();
        this.saldoInicial = saldoFinal.subtract(totalCreditos.subtract(totalDebitos));
    }

    public List<Transacao> getTransacoes() {
        return transacoes;
    }

    public void setTransacoes(List<Transacao> transacoes) {
        this.transacoes = transacoes;
    }

    public void adicionarTransacao(Transacao transacao) {
        this.transacoes.add(transacao);
    }

    public BigDecimal getTotalCreditos() {
        return transacoes.stream()
                .filter(t -> t.getTipo() == TipoTransacao.CREDIT)
                .map(Transacao::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalDebitos() {
        return transacoes.stream()
                .filter(t -> t.getTipo() == TipoTransacao.DEBIT)
                .map(Transacao::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .abs();
    }
}
