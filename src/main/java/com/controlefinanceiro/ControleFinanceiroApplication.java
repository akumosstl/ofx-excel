package com.controlefinanceiro;

import com.controlefinanceiro.excel.GeradorExcel;
import com.controlefinanceiro.model.ExtratoBancario;
import com.controlefinanceiro.parser.OfxParser;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.text.NumberFormat;
import java.util.Locale;

@SpringBootApplication
public class ControleFinanceiroApplication implements CommandLineRunner {

    private static final Locale LOCALE_BRASIL = new Locale("pt", "BR");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(LOCALE_BRASIL);

    private final ApplicationContext context;
    private final OfxParser ofxParser;
    private final GeradorExcel geradorExcel;

    public ControleFinanceiroApplication(ApplicationContext context, OfxParser ofxParser, GeradorExcel geradorExcel) {
        this.context = context;
        this.ofxParser = ofxParser;
        this.geradorExcel = geradorExcel;
    }

    public static void main(String[] args) {
        SpringApplication.run(ControleFinanceiroApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length == 0) {
            exibirAjuda();
            return;
        }

        String comando = args[0];

        switch (comando) {
            case "preencher" -> executarPreencher(args);
            case "--ajuda", "-h", "ajuda" -> exibirAjuda();
            default -> System.out.println("Comando desconhecido: " + comando);
        }
    }

    private void executarPreencher(String[] args) {
        if (args.length < 3) {
            System.out.println("Uso: preencher <caminho\\arquivo.ofx> <caminho\\arquivo.xlsx>");
            return;
        }

        String caminhoOfx = args[1];
        String caminhoXlsx = args[2];

        System.out.println("===============================================");
        System.out.println("       CONTROLE FINANCEIRO - PREENCHER");
        System.out.println("===============================================\n");
        System.out.println("Processando arquivo OFX: " + caminhoOfx);
        System.out.println("Gerando planilha: " + caminhoXlsx + "\n");

        try {
            validarArquivoOfx(caminhoOfx);

            ExtratoBancario extrato = ofxParser.parse(caminhoOfx);

            geradorExcel.gerarPlanilha(extrato, caminhoXlsx);

            System.out.println("Resumo:");
            System.out.println("- Transações encontradas: " + extrato.getTransacoes().size());
            System.out.println("- Créditos: " + CURRENCY_FORMAT.format(extrato.getTotalCreditos()));
            System.out.println("- Débitos: " + CURRENCY_FORMAT.format(extrato.getTotalDebitos()));
            System.out.println("- Saldo: " + CURRENCY_FORMAT.format(extrato.getSaldoFinal()));
            System.out.println("\nOperação concluída com sucesso!");

        } catch (IllegalArgumentException e) {
            System.err.println("Erro: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Erro inesperado: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void validarArquivoOfx(String caminho) {
        File arquivo = new File(caminho);
        if (!arquivo.exists()) {
            throw new IllegalArgumentException("Arquivo OFX não encontrado: " + caminho);
        }
        if (!arquivo.canRead()) {
            throw new IllegalArgumentException("Sem permissão para ler o arquivo: " + caminho);
        }
    }

    private void exibirAjuda() {
        System.out.println("""
            ===============================================
                   CONTROLE FINANCEIRO - AJUDA
            ===============================================

            Uso: java -jar controle-financeiro.jar <comando>

            Comandos disponíveis:

              preencher <ofx> <xlsx>
                  Preenche uma planilha Excel com dados de um arquivo OFX.

                  Exemplo:
                    java -jar controle-financeiro.jar preencher extrato.ofx planilha.xlsx

              ajuda, --ajuda, -h
                  Exibe esta mensagem de ajuda.

            """);
    }
}
