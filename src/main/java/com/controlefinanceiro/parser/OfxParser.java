package com.controlefinanceiro.parser;

import com.controlefinanceiro.model.ExtratoBancario;
import com.controlefinanceiro.model.TipoTransacao;
import com.controlefinanceiro.model.Transacao;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OfxParser {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public ExtratoBancario parse(String caminhoArquivo) throws IOException {
        ExtratoBancario extrato = new ExtratoBancario();
        StringBuilder conteudo = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(caminhoArquivo))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                conteudo.append(linha);
            }
        }

        String xml = conteudo.toString();

        String moeda = extrairValor(xml, "CURDEF");
        if (!"BRL".equals(moeda)) {
            throw new IllegalArgumentException("Extrato deve estar em Reais (BRL). Moeda encontrada: " + moeda);
        }

        extrato.setBanco(extrairValor(xml, "ORG"));
        extrato.setConta(extrairValor(xml, "ACCTID"));
        extrato.setDataInicial(parseData(extrairValor(xml, "DTSTART")));
        extrato.setDataFinal(parseData(extrairValor(xml, "DTEND")));
        extrato.setSaldoFinal(new BigDecimal(extrairValor(xml, "BALAMT")));

        List<Transacao> transacoes = extrairTransacoes(xml);
        transacoes.forEach(extrato::adicionarTransacao);

        extrato.calcularSaldoInicial();

        if (extrato.getTransacoes().isEmpty()) {
            throw new IllegalArgumentException("Nenhuma transação encontrada no extrato");
        }

        return extrato;
    }

    private String extrairValor(String xml, String tag) {
        Pattern pattern = Pattern.compile("<" + tag + ">([^<\\n]+)");
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private LocalDate parseData(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            String dataFormatada = data.length() >= 8 ? data.substring(0, 8) : data;
            return LocalDate.parse(dataFormatada, DATE_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }

    private List<Transacao> extrairTransacoes(String xml) {
        List<Transacao> transacoes = new ArrayList<>();
        Pattern pattern = Pattern.compile("<STMTTRN>(.*?)</STMTTRN>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(xml);

        while (matcher.find()) {
            String transacaoBloco = matcher.group(1);
            Transacao transacao = new Transacao();

            String tipoStr = extrairValor(transacaoBloco, "TRNTYPE");
            transacao.setTipo("CREDIT".equals(tipoStr) ? TipoTransacao.CREDIT : TipoTransacao.DEBIT);

            transacao.setData(parseData(extrairValor(transacaoBloco, "DTPOSTED")));
            transacao.setValor(new BigDecimal(extrairValor(transacaoBloco, "TRNAMT")));
            transacao.setIdUnico(extrairValor(transacaoBloco, "FITID"));
            transacao.setDescricao(extrairValor(transacaoBloco, "MEMO").trim());

            transacoes.add(transacao);
        }

        transacoes.sort((t1, t2) -> t1.getData().compareTo(t2.getData()));
        return transacoes;
    }
}
