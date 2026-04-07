package com.controlefinanceiro.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class Transacao {
    private LocalDate data;
    private String descricao;
    private TipoTransacao tipo;
    private BigDecimal valor;
    private String idUnico;

    public Transacao() {}

    public Transacao(LocalDate data, String descricao, TipoTransacao tipo, BigDecimal valor, String idUnico) {
        this.data = data;
        this.descricao = descricao;
        this.tipo = tipo;
        this.valor = valor;
        this.idUnico = idUnico;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoTransacao tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getIdUnico() {
        return idUnico;
    }

    public void setIdUnico(String idUnico) {
        this.idUnico = idUnico;
    }

    public BigDecimal getValorAbsoluto() {
        return valor.abs();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transacao that = (Transacao) o;
        return Objects.equals(idUnico, that.idUnico);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idUnico);
    }

    @Override
    public String toString() {
        return "Transacao{" +
                "data=" + data +
                ", descricao='" + descricao + '\'' +
                ", tipo=" + tipo +
                ", valor=" + valor +
                ", idUnico='" + idUnico + '\'' +
                '}';
    }
}
