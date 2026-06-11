/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import com.sesmt.pgeo.model.Agendamento;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class PdfService {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] gerarGuia(Agendamento ag) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titulo = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
            Font label  = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
            Font normal = new Font(Font.FontFamily.HELVETICA, 11);

            doc.add(new Paragraph("GUIA DE ATENDIMENTO MÉDICO", titulo));
            doc.add(new Paragraph("SESMT — Sistema de Exames Ocupacionais", normal));
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("─".repeat(60), normal));
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph("DADOS DO FUNCIONÁRIO", label));
            doc.add(Chunk.NEWLINE);
            addLinha(doc, "Funcionário:", ag.getFuncionarioNome(), label, normal);
            addLinha(doc, "Matrícula:",   ag.getFuncionarioMatricula(), label, normal);
            addLinha(doc, "Setor:",       ag.getFuncionarioSetor(), label, normal);
            addLinha(doc, "Função:",      ag.getFuncionarioFuncao(), label, normal);
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph("─".repeat(60), normal));
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("INFORMAÇÕES DO EXAME", label));
            doc.add(Chunk.NEWLINE);

            // Fix: getTipoExameDescricao() retorna String — correto para addLinha
            addLinha(doc, "Tipo de Exame:",    ag.getTipoExameDescricao(), label, normal);
            addLinha(doc, "Exame de Sangue:",  ag.getDataSangue()  != null ? ag.getDataSangue().format(BR)  : "Não aplicável", label, normal);
            addLinha(doc, "Consulta Clínica:", ag.getDataClinico() != null ? ag.getDataClinico().format(BR) : "—", label, normal);
            addLinha(doc, "Horário:",          ag.getHoraClinico(), label, normal);

            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("─".repeat(60), normal));
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("Documento gerado pelo PGEO — SESMT", normal));

            doc.close();
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Erro ao gerar PDF da guia", e);
            throw new RuntimeException("Erro ao gerar PDF", e);
        }
    }

    public byte[] gerarGuiaSangue(Agendamento ag) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titulo = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
            Font label  = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
            Font normal = new Font(Font.FontFamily.HELVETICA, 11);

            doc.add(new Paragraph("GUIA DE EXAME DE SANGUE", titulo));
            doc.add(new Paragraph("SESMT — Sistema de Exames Ocupacionais", normal));
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("─".repeat(60), normal));
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph("DADOS DO FUNCIONÁRIO", label));
            doc.add(Chunk.NEWLINE);
            addLinha(doc, "Funcionário:", ag.getFuncionarioNome(), label, normal);
            addLinha(doc, "Matrícula:",   ag.getFuncionarioMatricula(), label, normal);
            addLinha(doc, "Setor:",       ag.getFuncionarioSetor(), label, normal);
            addLinha(doc, "Função:",      ag.getFuncionarioFuncao(), label, normal);
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph("─".repeat(60), normal));
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("INFORMAÇÕES DO EXAME DE SANGUE", label));
            doc.add(Chunk.NEWLINE);
            addLinha(doc, "Tipo de Exame:",   ag.getTipoExameDescricao(), label, normal);
            addLinha(doc, "Data do Sangue:",  ag.getDataSangue() != null ? ag.getDataSangue().format(BR) : "—", label, normal);
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph("─".repeat(60), normal));
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("Documento gerado pelo PGEO — SESMT", normal));
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Erro ao gerar guia de sangue", e);
            throw new RuntimeException("Erro ao gerar PDF", e);
        }
    }

    public byte[] gerarGuiaClinico(Agendamento ag) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titulo = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
            Font label  = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
            Font normal = new Font(Font.FontFamily.HELVETICA, 11);

            doc.add(new Paragraph("GUIA DE EXAME CLÍNICO — PROTEUS", titulo));
            doc.add(new Paragraph("SESMT — Sistema de Exames Ocupacionais", normal));
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("─".repeat(60), normal));
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph("DADOS DO FUNCIONÁRIO", label));
            doc.add(Chunk.NEWLINE);
            addLinha(doc, "Funcionário:", ag.getFuncionarioNome(), label, normal);
            addLinha(doc, "Matrícula:",   ag.getFuncionarioMatricula(), label, normal);
            addLinha(doc, "Setor:",       ag.getFuncionarioSetor(), label, normal);
            addLinha(doc, "Função:",      ag.getFuncionarioFuncao(), label, normal);
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph("─".repeat(60), normal));
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("INFORMAÇÕES DO EXAME CLÍNICO", label));
            doc.add(Chunk.NEWLINE);
            addLinha(doc, "Tipo de Exame:",    ag.getTipoExameDescricao(), label, normal);
            addLinha(doc, "Data da Consulta:", ag.getDataClinico() != null ? ag.getDataClinico().format(BR) : "—", label, normal);
            addLinha(doc, "Horário:",          ag.getHoraClinico(), label, normal);
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph("─".repeat(60), normal));
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("Documento gerado pelo PGEO — SESMT", normal));
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Erro ao gerar guia clínico", e);
            throw new RuntimeException("Erro ao gerar PDF", e);
        }
    }

    private void addLinha(Document doc, String chave, String valor,
                          Font fLabel, Font fNormal) throws DocumentException {
        Paragraph p = new Paragraph();
        p.add(new Chunk(chave + " ", fLabel));
        p.add(new Chunk(valor != null ? valor : "—", fNormal));
        doc.add(p);
    }
}
