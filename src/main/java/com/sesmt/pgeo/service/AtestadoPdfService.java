/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.sesmt.pgeo.model.MedicalLeave;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AtestadoPdfService {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BaseColor HEADER_BG  = new BaseColor(30, 58, 138);   // azul escuro
    private static final BaseColor STRIP_BG   = new BaseColor(241, 245, 249); // cinza claro

    public byte[] gerarRelatorio(List<MedicalLeave> atestados,
                                  java.time.LocalDate inicio, java.time.LocalDate fim,
                                  Map<String, Integer> porSetor, Map<String, Long> porTipo) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 28, 28, 36, 28);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font fTitulo  = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.WHITE);
            Font fSub     = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, BaseColor.WHITE);
            Font fHead    = new Font(Font.FontFamily.HELVETICA, 8,  Font.BOLD,   BaseColor.WHITE);
            Font fCell    = new Font(Font.FontFamily.HELVETICA, 8,  Font.NORMAL);
            Font fSection = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   BaseColor.WHITE);

            // ── Cabeçalho ──
            PdfPTable header = new PdfPTable(1);
            header.setWidthPercentage(100);
            PdfPCell hCell = new PdfPCell();
            hCell.setBackgroundColor(HEADER_BG);
            hCell.setPadding(12);
            hCell.setBorder(Rectangle.NO_BORDER);
            Paragraph hPar = new Paragraph("RELATÓRIO DE AFASTAMENTO SEMANAL — ATESTADOS", fTitulo);
            hPar.add(Chunk.NEWLINE);
            hPar.add(new Chunk("SESMT — PGEO  |  Período: " +
                inicio.format(BR) + " a " + fim.format(BR), fSub));
            hCell.addElement(hPar);
            header.addCell(hCell);
            doc.add(header);
            doc.add(Chunk.NEWLINE);

            // ── Tabela principal ──
            if (atestados.isEmpty()) {
                doc.add(new Paragraph("Nenhum atestado lançado neste período.", fSub));
            } else {
                float[] widths = {5f, 16f, 12f, 14f, 5f, 8f, 10f, 6f, 14f, 8f};
                PdfPTable tabela = new PdfPTable(widths.length);
                tabela.setWidthPercentage(100);
                tabela.setWidths(widths);

                String[] cols = {"Matríc.", "Nome", "Função", "Setor", "Dias", "Data Atest.", "Tipo", "CID", "Médico", "CRM"};
                for (String c : cols) {
                    PdfPCell cell = new PdfPCell(new Phrase(c, fHead));
                    cell.setBackgroundColor(HEADER_BG);
                    cell.setPadding(5);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(cell);
                }

                boolean alt = false;
                for (MedicalLeave ml : atestados) {
                    BaseColor bg = alt ? STRIP_BG : BaseColor.WHITE;
                    alt = !alt;
                    var f = ml.getFuncionario();
                    addCell(tabela, f.getMatricula() != null ? f.getMatricula() : "—", fCell, bg, Element.ALIGN_CENTER);
                    addCell(tabela, f.getNome(), fCell, bg, Element.ALIGN_LEFT);
                    addCell(tabela, nvl(f.getFuncao()), fCell, bg, Element.ALIGN_LEFT);
                    addCell(tabela, nvl(f.getSetor()), fCell, bg, Element.ALIGN_LEFT);
                    addCell(tabela, String.valueOf(ml.getDiasAfastamento()), fCell, bg, Element.ALIGN_CENTER);
                    addCell(tabela, ml.getDataAfastamento().format(BR), fCell, bg, Element.ALIGN_CENTER);
                    addCell(tabela, ml.getTipo() != null ? ml.getTipo().getDescricao() : "—", fCell, bg, Element.ALIGN_LEFT);
                    addCell(tabela, nvl(ml.getCid()), fCell, bg, Element.ALIGN_CENTER);
                    addCell(tabela, nvl(ml.getMedicoNome()), fCell, bg, Element.ALIGN_LEFT);
                    addCell(tabela, nvl(ml.getMedicoCrm()), fCell, bg, Element.ALIGN_CENTER);
                }

                doc.add(tabela);
            }

            doc.add(Chunk.NEWLINE);

            // ── Totais por setor e tipo lado a lado ──
            PdfPTable resumos = new PdfPTable(2);
            resumos.setWidthPercentage(60);
            resumos.setWidths(new float[]{1f, 1f});
            resumos.setHorizontalAlignment(Element.ALIGN_LEFT);

            PdfPCell sTitle = new PdfPCell(new Phrase("Total por Setor", fSection));
            sTitle.setColspan(1); sTitle.setPadding(6); sTitle.setBorder(Rectangle.NO_BORDER);
            sTitle.setBackgroundColor(HEADER_BG);
            resumos.addCell(sTitle);
            PdfPCell tTitle = new PdfPCell(new Phrase("Total por Tipo", fSection));
            tTitle.setColspan(1); tTitle.setPadding(6); tTitle.setBorder(Rectangle.NO_BORDER);
            tTitle.setBackgroundColor(HEADER_BG);
            resumos.addCell(tTitle);

            StringBuilder setorTxt = new StringBuilder();
            porSetor.forEach((s, d) -> setorTxt.append(s).append(": ").append(d).append(" dias\n"));
            PdfPCell setorCell = new PdfPCell(new Phrase(setorTxt.toString(), fCell));
            setorCell.setPadding(6);
            resumos.addCell(setorCell);

            StringBuilder tipoTxt = new StringBuilder();
            porTipo.forEach((t, c) -> tipoTxt.append(t).append(": ").append(c).append(" atestado(s)\n"));
            PdfPCell tipoCell = new PdfPCell(new Phrase(tipoTxt.toString(), fCell));
            tipoCell.setPadding(6);
            resumos.addCell(tipoCell);

            doc.add(resumos);

            doc.add(Chunk.NEWLINE);
            Font fRodape = new Font(Font.FontFamily.HELVETICA, 7, Font.ITALIC, BaseColor.GRAY);
            doc.add(new Paragraph("Gerado pelo PGEO — SESMT  |  " +
                java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), fRodape));

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Erro ao gerar PDF de atestados", e);
            throw new RuntimeException("Erro ao gerar PDF", e);
        }
    }

    private void addCell(PdfPTable t, String text, Font f, BaseColor bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(bg);
        c.setPadding(5);
        c.setHorizontalAlignment(align);
        t.addCell(c);
    }

    private String nvl(String s) { return s != null ? s : "—"; }
}
