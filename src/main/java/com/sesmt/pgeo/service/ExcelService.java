package com.sesmt.pgeo.service;

import com.sesmt.pgeo.model.Agendamento;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelService {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String[] COLUNAS = {
        "Nome", "Setor", "Função", "Tipo de Exame",
        "Data Clínico", "Horário", "Data Sangue",
        "ASO Enviado", "ASO Recebido", "Observações"
    };

    public byte[] gerarPlanilha(List<Agendamento> agendamentos) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Agendamentos");

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < COLUNAS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(COLUNAS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Agendamento a : agendamentos) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(nvl(a.getFuncionarioNome()));
                row.createCell(1).setCellValue(nvl(a.getFuncionarioSetor()));
                row.createCell(2).setCellValue(nvl(a.getFuncionarioFuncao()));
                row.createCell(3).setCellValue(a.getTipoExameDescricao());
                row.createCell(4).setCellValue(a.getDataClinico() != null ? a.getDataClinico().format(BR) : "");
                row.createCell(5).setCellValue(nvl(a.getHoraClinico()));
                row.createCell(6).setCellValue(a.getDataSangue() != null ? a.getDataSangue().format(BR) : "");
                row.createCell(7).setCellValue(a.isAsoEnviado() ? "Sim" : "Não");
                row.createCell(8).setCellValue(a.isAsoRecebido() ? "Sim" : "Não");
                row.createCell(9).setCellValue(nvl(a.getObservacoes()));
            }

            for (int i = 0; i < COLUNAS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Falha ao gerar planilha Excel", e);
        }
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }
}
