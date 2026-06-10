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
                row = row.replace("﻿", "").strip();
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

    // ── Lógica de upsert por linha ────────────────────────────────────
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
        return String.format("ADM%d%04d", Year.now().getValue(), funcionarioRepo.count() + 1);
    }

    private void registrarAudit(int criados, int atualizados, int ignorados) {
        auditService.registrar("IMPORTACAO", "Funcionario", null,
            String.format("Importação CSV/Excel: %d criado(s), %d atualizado(s), %d ignorado(s).",
                criados, atualizados, ignorados));
    }
}
