package com.controlefinanceiro.excel;

import com.controlefinanceiro.model.ExtratoBancario;
import com.controlefinanceiro.model.TipoTransacao;
import com.controlefinanceiro.model.Transacao;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class GeradorExcel {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale LOCALE_BRASIL = new Locale("pt", "BR");
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getCurrencyInstance(LOCALE_BRASIL);

    public void gerarPlanilha(ExtratoBancario extrato, String caminhoXlsx) throws IOException {
        Workbook workbook;
        Sheet sheetEntradas;
        Sheet sheetSaidas;
        Sheet sheetResumo;

        Set<String> idsEntradasExistentes = new HashSet<>();
        Set<String> idsSaidasExistentes = new HashSet<>();

        if (java.nio.file.Files.exists(java.nio.file.Paths.get(caminhoXlsx))) {
            try (FileInputStream fis = new FileInputStream(caminhoXlsx)) {
                workbook = new XSSFWorkbook(fis);
                sheetEntradas = workbook.getSheet("Entradas");
                if (sheetEntradas != null) {
                    idsEntradasExistentes = coletarIdsExistentes(sheetEntradas);
                }
                sheetSaidas = workbook.getSheet("Saidas");
                if (sheetSaidas != null) {
                    idsSaidasExistentes = coletarIdsExistentes(sheetSaidas);
                }
                sheetResumo = workbook.getSheet("Resumo");
            }
        } else {
            workbook = new XSSFWorkbook();
            sheetEntradas = criarAbaTransacoes(workbook, "Entradas");
            sheetSaidas = criarAbaTransacoes(workbook, "Saidas");
            sheetResumo = criarAbaResumo(workbook);
        }

        if (sheetEntradas == null) {
            sheetEntradas = criarAbaTransacoes(workbook, "Entradas");
        }
        if (sheetSaidas == null) {
            sheetSaidas = criarAbaTransacoes(workbook, "Saidas");
        }
        if (sheetResumo == null) {
            sheetResumo = criarAbaResumo(workbook);
        }

        CellStyle estiloVerde = criarEstiloValor(workbook, IndexedColors.GREEN);
        CellStyle estiloVermelho = criarEstiloValor(workbook, IndexedColors.RED);

        int ultimaLinhaEntradas = sheetEntradas.getLastRowNum();
        if (ultimaLinhaEntradas < 1) {
            ultimaLinhaEntradas = 1;
        }

        int ultimaLinhaSaidas = sheetSaidas.getLastRowNum();
        if (ultimaLinhaSaidas < 1) {
            ultimaLinhaSaidas = 1;
        }

        for (Transacao transacao : extrato.getTransacoes()) {
            if (transacao.getTipo() == TipoTransacao.CREDIT) {
                if (idsEntradasExistentes.contains(transacao.getIdUnico())) {
                    continue;
                }
                Row row = sheetEntradas.createRow(++ultimaLinhaEntradas);
                preencherLinhaTransacao(row, transacao, estiloVerde);
            } else {
                if (idsSaidasExistentes.contains(transacao.getIdUnico())) {
                    continue;
                }
                Row row = sheetSaidas.createRow(++ultimaLinhaSaidas);
                preencherLinhaTransacao(row, transacao, estiloVermelho);
            }
        }

        preencherResumo(sheetResumo, extrato);

        sheetEntradas.setAutoFilter(new CellRangeAddress(1, ultimaLinhaEntradas, 0, 3));
        sheetSaidas.setAutoFilter(new CellRangeAddress(1, ultimaLinhaSaidas, 0, 3));

        for (int i = 0; i < 4; i++) {
            sheetEntradas.autoSizeColumn(i);
            sheetSaidas.autoSizeColumn(i);
        }

        try (FileOutputStream fos = new FileOutputStream(caminhoXlsx)) {
            workbook.write(fos);
        }
        workbook.close();
    }

    private void preencherLinhaTransacao(Row row, Transacao transacao, CellStyle estiloCor) {
        Cell cellData = row.createCell(0);
        cellData.setCellValue(transacao.getData().format(DATE_FORMATTER));

        Cell cellDesc = row.createCell(1);
        cellDesc.setCellValue(transacao.getDescricao());

        Cell cellValor = row.createCell(2);
        cellValor.setCellValue(transacao.getValorAbsoluto().doubleValue());
        cellValor.setCellStyle(estiloCor);

        Cell cellId = row.createCell(3);
        cellId.setCellValue(transacao.getIdUnico());
    }

    private Set<String> coletarIdsExistentes(Sheet sheet) {
        Set<String> ids = new HashSet<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                Cell cell = row.getCell(3);
                if (cell != null && cell.getCellType() == CellType.STRING) {
                    String id = cell.getStringCellValue();
                    if (id != null && !id.isEmpty()) {
                        ids.add(id);
                    }
                }
            }
        }
        return ids;
    }

    private Sheet criarAbaTransacoes(Workbook workbook, String nomeAba) {
        Sheet sheet = workbook.createSheet(nomeAba);

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row headerRow = sheet.createRow(0);
        String[] headers = {"Data", "Descricao", "Valor (R$)", "ID Unico"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        return sheet;
    }

    private Sheet criarAbaResumo(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Resumo");

        CellStyle labelStyle = workbook.createCellStyle();
        Font labelFont = workbook.createFont();
        labelFont.setBold(true);
        labelStyle.setFont(labelFont);

        CellStyle valueStyle = workbook.createCellStyle();

        Row rowBanco = sheet.createRow(0);
        criarLinhaResumo(rowBanco, 0, "Banco:", "", labelStyle, valueStyle);

        Row rowConta = sheet.createRow(1);
        criarLinhaResumo(rowConta, 0, "Conta:", "", labelStyle, valueStyle);

        Row rowPeriodo = sheet.createRow(2);
        criarLinhaResumo(rowPeriodo, 0, "Periodo:", "", labelStyle, valueStyle);

        Row rowSaldoInicial = sheet.createRow(3);
        criarLinhaResumo(rowSaldoInicial, 0, "Saldo Inicial:", "", labelStyle, valueStyle);

        Row rowCreditos = sheet.createRow(4);
        criarLinhaResumo(rowCreditos, 0, "Total Entradas:", "", labelStyle, valueStyle);

        Row rowDebitos = sheet.createRow(5);
        criarLinhaResumo(rowDebitos, 0, "Total Saidas:", "", labelStyle, valueStyle);

        Row rowSaldo = sheet.createRow(6);
        criarLinhaResumo(rowSaldo, 0, "Saldo Final:", "", labelStyle, valueStyle);

        sheet.setColumnWidth(0, 3000);
        sheet.setColumnWidth(1, 4000);

        return sheet;
    }

    private void criarLinhaResumo(Row row, int colLabel, String label, String value,
                                   CellStyle labelStyle, CellStyle valueStyle) {
        Cell cellLabel = row.createCell(colLabel);
        cellLabel.setCellValue(label);
        cellLabel.setCellStyle(labelStyle);

        Cell cellValue = row.createCell(colLabel + 1);
        cellValue.setCellValue(value);
        cellValue.setCellStyle(valueStyle);
    }

    private void preencherResumo(Sheet sheet, ExtratoBancario extrato) {
        String periodo = extrato.getDataInicial().format(DATE_FORMATTER) + " a " +
                        extrato.getDataFinal().format(DATE_FORMATTER);

        atualizarCelula(sheet, 0, 1, extrato.getBanco());
        atualizarCelula(sheet, 1, 1, extrato.getConta());
        atualizarCelula(sheet, 2, 1, periodo);
        atualizarCelula(sheet, 3, 1, NUMBER_FORMAT.format(extrato.getSaldoInicial()));
        atualizarCelula(sheet, 4, 1, NUMBER_FORMAT.format(extrato.getTotalCreditos()));
        atualizarCelula(sheet, 5, 1, NUMBER_FORMAT.format(extrato.getTotalDebitos()));
        atualizarCelula(sheet, 6, 1, NUMBER_FORMAT.format(extrato.getSaldoFinal()));
    }

    private void atualizarCelula(Sheet sheet, int row, int col, String value) {
        Row sheetRow = sheet.getRow(row);
        if (sheetRow == null) {
            sheetRow = sheet.createRow(row);
        }
        Cell cell = sheetRow.getCell(col);
        if (cell == null) {
            cell = sheetRow.createCell(col);
        }
        cell.setCellValue(value);
    }

    private CellStyle criarEstiloValor(Workbook workbook, IndexedColors color) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setColor(color.getIndex());
        style.setFont(font);
        return style;
    }
}
