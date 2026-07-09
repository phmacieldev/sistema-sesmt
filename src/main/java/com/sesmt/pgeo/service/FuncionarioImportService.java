/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.service;

import com.sesmt.pgeo.audit.AuditService;
import com.sesmt.pgeo.model.Funcionario;
import com.sesmt.pgeo.model.enums.StatusFuncionario;
import com.sesmt.pgeo.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FuncionarioImportService {

    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final FuncionarioRepository funcionarioRepo;
    private final AuditService          auditService;

    public record ImportacaoResultado(int criados, int atualizados, int ignorados, List<String> erros) {}

    public record ConflitoCampo(String campo, String valorAtual, String valorArquivo) {}

    public record LinhaPreview(int linha, String matricula, String nome, boolean novo,
                               List<ConflitoCampo> conflitos) {}

    public record PreviewResultado(List<LinhaPreview> linhas, int novos, int conflitantes,
                                    int semConflito, List<String> erros) {}

    @Transactional
    public ImportacaoResultado importar(MultipartFile arquivo) throws Exception {
        String nome = arquivo.getOriginalFilename() != null
            ? arquivo.getOriginalFilename().toLowerCase() : "";

        if (nome.endsWith(".xlsx") || nome.endsWith(".xls")) {
            return importarExcel(arquivo);
        }
        return importarCsv(arquivo);
    }

    // ── CSV (separador ;) ─────────────────────────────────────────────

    private ImportacaoResultado importarCsv(MultipartFile arquivo) throws Exception {
        int criados = 0, atualizados = 0, ignorados = 0;
        List<String> erros = new ArrayList<>();
        int linha = 0;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(arquivo.getInputStream(), StandardCharsets.UTF_8))) {

            String cabecalho = br.readLine(); // descarta cabeçalho
            if (cabecalho == null) return new ImportacaoResultado(0, 0, 0, List.of("Arquivo vazio."));

            String row;
            while ((row = br.readLine()) != null) {
                linha++;
                // Remove BOM se presente
                row = row.replace("", "").strip();
                if (row.isBlank()) continue;

                String[] cols = row.split(";", -1);
                try {
                    var resultado = processarLinha(
                        get(cols, 0), get(cols, 1), get(cols, 2), get(cols, 3),
                        get(cols, 4), get(cols, 5), get(cols, 6), get(cols, 7));
                    if (resultado == 1)      criados++;
                    else if (resultado == 2) atualizados++;
                    else                     ignorados++;
                } catch (Exception e) {
                    erros.add("Linha " + linha + ": " + e.getMessage());
                    ignorados++;
                }
            }
        }

        registrarAudit(criados, atualizados, ignorados);
        return new ImportacaoResultado(criados, atualizados, ignorados, erros);
    }

    // ── Excel (.xlsx) ─────────────────────────────────────────────────

    private ImportacaoResultado importarExcel(MultipartFile arquivo) throws Exception {
        int criados = 0, atualizados = 0, ignorados = 0;
        List<String> erros = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(arquivo.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            boolean primeiraLinha = true;

            for (Row row : sheet) {
                if (primeiraLinha) { primeiraLinha = false; continue; } // cabeçalho
                if (isRowVazia(row)) continue;

                int linhaNum = row.getRowNum() + 1;
                try {
                    var resultado = processarLinha(
                        celula(row, 0), celula(row, 1), celula(row, 2), celula(row, 3),
                        celula(row, 4), celula(row, 5), celula(row, 6), celula(row, 7));
                    if (resultado == 1)      criados++;
                    else if (resultado == 2) atualizados++;
                    else                     ignorados++;
                } catch (Exception e) {
                    erros.add("Linha " + linhaNum + ": " + e.getMessage());
                    ignorados++;
                }
            }
        }

        registrarAudit(criados, atualizados, ignorados);
        return new ImportacaoResultado(criados, atualizados, ignorados, erros);
    }

    // ── Preview: analisa arquivo sem salvar ─────────────────────────────

    public PreviewResultado preview(MultipartFile arquivo) throws Exception {
        List<String[]> linhas = parsearArquivo(arquivo);
        List<LinhaPreview> previews = new ArrayList<>();
        List<String> erros = new ArrayList<>();
        int novos = 0, conflitantes = 0, semConflito = 0;

        for (int i = 0; i < linhas.size(); i++) {
            String[] cols = linhas.get(i);
            try {
                LinhaPreview lp = analisarLinha(i + 2, cols);
                if (lp == null) continue;
                previews.add(lp);
                if (lp.novo()) novos++;
                else if (!lp.conflitos().isEmpty()) conflitantes++;
                else semConflito++;
            } catch (Exception e) {
                erros.add("Linha " + (i + 2) + ": " + e.getMessage());
            }
        }

        return new PreviewResultado(previews, novos, conflitantes, semConflito, erros);
    }

    private LinhaPreview analisarLinha(int numLinha, String[] cols) {
        String nome = get(cols, 1);
        if (nome == null || nome.isBlank()) return null;

        String mat = get(cols, 0);
        mat = (mat != null && !mat.isBlank()) ? mat.strip() : null;

        Funcionario func = (mat != null) ? funcionarioRepo.findByMatricula(mat).orElse(null) : null;

        if (func == null) {
            return new LinhaPreview(numLinha, mat != null ? mat : "(nova)", nome.strip(), true, List.of());
        }

        List<ConflitoCampo> conflitos = new ArrayList<>();
        compararCampo(conflitos, "Nome",            func.getNome(),              nome);
        compararCampo(conflitos, "Setor",           func.getSetor(),             get(cols, 2));
        compararCampo(conflitos, "Função",          func.getFuncao(),            get(cols, 3));
        compararCampo(conflitos, "E-mail",          func.getEmail(),             get(cols, 4));
        compararCampo(conflitos, "ASO",             func.getAso() != null ? func.getAso().format(FMT_DATA) : "",
                                                    get(cols, 5));
        compararCampo(conflitos, "Exige Sangue",    func.isExigeSangue() ? "sim" : "nao", get(cols, 6));
        compararCampo(conflitos, "Estabelecimento", func.getEstabelecimento(),   get(cols, 7));

        return new LinhaPreview(numLinha, mat, func.getNome(), false, conflitos);
    }

    private void compararCampo(List<ConflitoCampo> lista, String campo, String atual, String arquivo) {
        if (arquivo == null || arquivo.isBlank()) return;
        String atualNorm = (atual != null) ? atual.strip() : "";
        String arquivoNorm = arquivo.strip();
        if (!atualNorm.equalsIgnoreCase(arquivoNorm)) {
            lista.add(new ConflitoCampo(campo, atualNorm.isEmpty() ? "(vazio)" : atualNorm, arquivoNorm));
        }
    }

    // ── Aplicação seletiva (após preview) ────────────────────────────

    @Transactional
    public ImportacaoResultado aplicarComSelecao(byte[] bytes, String nomeArquivo,
                                                 java.util.Set<String> conflitosAceitos) throws Exception {
        List<String[]> linhas = parsearBytes(bytes, nomeArquivo);
        int criados = 0, atualizados = 0, ignorados = 0;
        List<String> erros = new ArrayList<>();

        for (int i = 0; i < linhas.size(); i++) {
            String[] cols = linhas.get(i);
            try {
                int res = processarLinhaComSelecao(i + 2, cols, conflitosAceitos);
                if (res == 1)      criados++;
                else if (res == 2) atualizados++;
                else               ignorados++;
            } catch (Exception e) {
                erros.add("Linha " + (i + 2) + ": " + e.getMessage());
                ignorados++;
            }
        }

        registrarAudit(criados, atualizados, ignorados);
        return new ImportacaoResultado(criados, atualizados, ignorados, erros);
    }

    private int processarLinhaComSelecao(int numLinha, String[] cols,
                                          java.util.Set<String> aceitos) {
        String nome = get(cols, 1);
        if (nome == null || nome.isBlank()) return 0;

        String mat = get(cols, 0);
        mat = (mat != null && !mat.isBlank()) ? mat.strip() : null;

        Funcionario func = (mat != null) ? funcionarioRepo.findByMatricula(mat).orElse(null) : null;
        boolean criando = false;

        if (func == null) {
            func = new Funcionario();
            func.setMatricula(mat != null ? mat : gerarMatriculaAdm());
            func.setStatus(mat != null ? StatusFuncionario.ATIVO : StatusFuncionario.PRE_ADMISSIONAL);
            func.setAtivo(true);
            criando = true;
        }

        // Para novos, aplica tudo. Para existentes, só aplica campos aceitos.
        aplicarCampoSeletivo(func, "Nome", nome, criando, numLinha, aceitos,
            (f, v) -> f.setNome(v.strip()));
        aplicarCampoSeletivo(func, "Setor", get(cols, 2), criando, numLinha, aceitos,
            (f, v) -> f.setSetor(v.strip()));
        aplicarCampoSeletivo(func, "Função", get(cols, 3), criando, numLinha, aceitos,
            (f, v) -> f.setFuncao(v.strip()));
        aplicarCampoSeletivo(func, "E-mail", get(cols, 4), criando, numLinha, aceitos,
            (f, v) -> f.setEmail(v.strip()));
        aplicarCampoSeletivo(func, "Estabelecimento", get(cols, 7), criando, numLinha, aceitos,
            (f, v) -> f.setEstabelecimento(v.strip().toUpperCase()));

        String asoStr = get(cols, 5);
        if (asoStr != null && !asoStr.isBlank()) {
            if (criando || aceitos.contains(numLinha + ":ASO")) {
                func.setAso(LocalDate.parse(asoStr.strip(), FMT_DATA));
            }
        }
        String exigeSangueStr = get(cols, 6);
        if (exigeSangueStr != null && !exigeSangueStr.isBlank()) {
            if (criando || aceitos.contains(numLinha + ":Exige Sangue")) {
                func.setExigeSangue(parseBoolean(exigeSangueStr));
            }
        }

        funcionarioRepo.save(func);
        return criando ? 1 : 2;
    }

    private void aplicarCampoSeletivo(Funcionario func, String campo, String valor,
                                       boolean criando, int numLinha,
                                       java.util.Set<String> aceitos,
                                       java.util.function.BiConsumer<Funcionario, String> setter) {
        if (valor == null || valor.isBlank()) return;
        if (criando || aceitos.contains(numLinha + ":" + campo)) {
            setter.accept(func, valor);
        }
    }

    // ── Parse do arquivo (reutilizado por preview e importação) ──────

    private List<String[]> parsearArquivo(MultipartFile arquivo) throws Exception {
        return parsearBytes(arquivo.getBytes(),
            arquivo.getOriginalFilename() != null ? arquivo.getOriginalFilename() : "");
    }

    private List<String[]> parsearBytes(byte[] bytes, String nomeArquivo) throws Exception {
        String nome = nomeArquivo != null ? nomeArquivo.toLowerCase() : "";
        java.io.InputStream is = new java.io.ByteArrayInputStream(bytes);
        if (nome.endsWith(".xlsx") || nome.endsWith(".xls")) {
            return parsearExcelStream(is);
        }
        return parsearCsvStream(is);
    }

    private List<String[]> parsearCsvStream(java.io.InputStream is) throws Exception {
        List<String[]> linhas = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            br.readLine(); // cabeçalho
            String row;
            while ((row = br.readLine()) != null) {
                row = row.replace("﻿", "").strip();
                if (row.isBlank()) continue;
                linhas.add(row.split(";", -1));
            }
        }
        return linhas;
    }

    private List<String[]> parsearExcelStream(java.io.InputStream is) throws Exception {
        List<String[]> linhas = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            boolean primeiraLinha = true;
            for (Row row : sheet) {
                if (primeiraLinha) { primeiraLinha = false; continue; }
                if (isRowVazia(row)) continue;
                String[] cols = new String[8];
                for (int i = 0; i < 8; i++) cols[i] = celula(row, i);
                linhas.add(cols);
            }
        }
        return linhas;
    }

    // ── Lógica de upsert por linha (importação direta, sem revisão) ──
    // Retorna: 1 = criado, 2 = atualizado, 0 = ignorado

    private int processarLinha(String matricula, String nome, String setor, String funcao,
                                String email, String asoStr, String exigeSangueStr,
                                String estabelecimento) {
        if (nome == null || nome.isBlank()) return 0; // sem nome → ignora

        String mat = (matricula != null && !matricula.isBlank()) ? matricula.strip() : null;

        Funcionario func = null;
        boolean criando = false;

        if (mat != null) {
            func = funcionarioRepo.findByMatricula(mat).orElse(null);
        }

        if (func == null) {
            func = new Funcionario();
            func.setMatricula(mat != null ? mat : gerarMatriculaAdm());
            func.setStatus(mat != null ? StatusFuncionario.ATIVO : StatusFuncionario.PRE_ADMISSIONAL);
            func.setAtivo(true);
            criando = true;
        }

        // Aplica campos — campos em branco no CSV não sobrescrevem o existente
        func.setNome(nome.strip());
        if (setor != null && !setor.isBlank())         func.setSetor(setor.strip());
        if (funcao != null && !funcao.isBlank())        func.setFuncao(funcao.strip());
        if (email != null && !email.isBlank())          func.setEmail(email.strip());
        if (estabelecimento != null && !estabelecimento.isBlank())
            func.setEstabelecimento(estabelecimento.strip().toUpperCase());

        if (asoStr != null && !asoStr.isBlank()) {
            func.setAso(LocalDate.parse(asoStr.strip(), FMT_DATA));
        }
        if (exigeSangueStr != null && !exigeSangueStr.isBlank()) {
            func.setExigeSangue(parseBoolean(exigeSangueStr));
        }

        funcionarioRepo.save(func);
        return criando ? 1 : 2;
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private static String get(String[] arr, int idx) {
        return (idx < arr.length) ? arr[idx].strip() : "";
    }

    private static String celula(Row row, int idx) {
        Cell c = row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (c == null) return "";
        return switch (c.getCellType()) {
            case STRING  -> c.getStringCellValue().strip();
            case NUMERIC -> DateUtil.isCellDateFormatted(c)
                ? c.getLocalDateTimeCellValue().toLocalDate().format(FMT_DATA)
                : String.valueOf((long) c.getNumericCellValue());
            case BOOLEAN -> c.getBooleanCellValue() ? "sim" : "nao";
            default      -> "";
        };
    }

    private static boolean isRowVazia(Row row) {
        for (Cell c : row) {
            if (c.getCellType() != CellType.BLANK && !c.toString().isBlank()) return false;
        }
        return true;
    }

    private static boolean parseBoolean(String s) {
        return s.equalsIgnoreCase("sim") || s.equalsIgnoreCase("s")
            || s.equalsIgnoreCase("true") || s.equals("1");
    }

    private String gerarMatriculaAdm() {
        // count() é só ponto de partida: pode colidir com matrículas existentes
        // após exclusões, então incrementa até achar uma livre
        int ano = Year.now().getValue();
        long proximo = funcionarioRepo.count() + 1;
        String matricula;
        do {
            matricula = String.format("ADM%d%04d", ano, proximo++);
        } while (funcionarioRepo.existsByMatricula(matricula));
        return matricula;
    }

    private void registrarAudit(int criados, int atualizados, int ignorados) {
        auditService.registrar("IMPORTACAO", "Funcionario", null,
            String.format("Importação CSV/Excel: %d criado(s), %d atualizado(s), %d ignorado(s).",
                criados, atualizados, ignorados));
    }
}
