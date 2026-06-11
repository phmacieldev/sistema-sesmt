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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FuncionarioImportServiceTest {

    @Mock FuncionarioRepository funcionarioRepo;
    @Mock AuditService          auditService;

    @InjectMocks FuncionarioImportService service;

    // ── CSV ───────────────────────────────────────────────────────────

    @Test
    void importarCsv_novoFuncionario_criaComStatusAtivo() throws Exception {
        when(funcionarioRepo.findByMatricula("12345")).thenReturn(Optional.empty());
        when(funcionarioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resultado = service.importar(csv("12345;João Silva;UTI;Técnico;joao@h.com;;;HEAA"));

        assertThat(resultado.criados()).isEqualTo(1);
        assertThat(resultado.atualizados()).isZero();
        assertThat(resultado.erros()).isEmpty();

        verify(funcionarioRepo).save(argThat(f ->
            "João Silva".equals(f.getNome()) &&
            StatusFuncionario.ATIVO == f.getStatus() &&
            "12345".equals(f.getMatricula())
        ));
    }

    @Test
    void importarCsv_funcionarioExistente_atualizaDados() throws Exception {
        Funcionario existente = new Funcionario();
        existente.setId(1L);
        existente.setMatricula("99999");
        existente.setNome("Nome Antigo");
        existente.setStatus(StatusFuncionario.ATIVO);

        when(funcionarioRepo.findByMatricula("99999")).thenReturn(Optional.of(existente));
        when(funcionarioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resultado = service.importar(csv("99999;Nome Novo;RH;Assistente;;;;FMC"));

        assertThat(resultado.atualizados()).isEqualTo(1);
        assertThat(resultado.criados()).isZero();

        verify(funcionarioRepo).save(argThat(f -> "Nome Novo".equals(f.getNome())));
    }

    @Test
    void importarCsv_semMatricula_criaComoPreAdmissional() throws Exception {
        when(funcionarioRepo.count()).thenReturn(5L);
        when(funcionarioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resultado = service.importar(csv(";Maria Pré;;Aux;;;;"));

        assertThat(resultado.criados()).isEqualTo(1);
        verify(funcionarioRepo).save(argThat(f ->
            StatusFuncionario.PRE_ADMISSIONAL == f.getStatus()
        ));
    }

    @Test
    void importarCsv_linhaComNomeVazio_ignora() throws Exception {
        var resultado = service.importar(csv("12345;;UTI;Técnico;;;;")); // nome em branco

        assertThat(resultado.ignorados()).isEqualTo(1);
        assertThat(resultado.criados()).isZero();
        verify(funcionarioRepo, never()).save(any());
    }

    @Test
    void importarCsv_arquivoVazio_retornaErro() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
            "arquivo", "vazio.csv", "text/csv", new byte[0]);

        var resultado = service.importar(arquivo);

        assertThat(resultado.erros()).isNotEmpty();
        verify(funcionarioRepo, never()).save(any());
    }

    @Test
    void importarCsv_multipasLinhas_contaCorretamente() throws Exception {
        when(funcionarioRepo.findByMatricula("001")).thenReturn(Optional.empty());
        when(funcionarioRepo.findByMatricula("002")).thenReturn(Optional.empty());
        when(funcionarioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String conteudo = "matricula;nome;setor;funcao;email;aso;exige_sangue;estabelecimento\n"
                        + "001;Func Um;UTI;Técnico;;;;\n"
                        + "002;Func Dois;RH;Aux;;;;";

        MockMultipartFile arquivo = new MockMultipartFile(
            "arquivo", "func.csv", "text/csv",
            conteudo.getBytes(StandardCharsets.UTF_8));

        var resultado = service.importar(arquivo);

        assertThat(resultado.criados()).isEqualTo(2);
        assertThat(resultado.erros()).isEmpty();
    }

    @Test
    void importarCsv_erroEmUmaLinha_continuaProcessandoRestante() throws Exception {
        when(funcionarioRepo.findByMatricula("001")).thenReturn(Optional.empty());
        when(funcionarioRepo.findByMatricula("002")).thenReturn(Optional.empty());
        when(funcionarioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // data de ASO com formato inválido na linha 1
        String conteudo = "matricula;nome;setor;funcao;email;aso;exige_sangue;estabelecimento\n"
                        + "001;Func Um;UTI;Técnico;;data-invalida;;\n"
                        + "002;Func Dois;RH;Aux;;;;";

        MockMultipartFile arquivo = new MockMultipartFile(
            "arquivo", "func.csv", "text/csv",
            conteudo.getBytes(StandardCharsets.UTF_8));

        var resultado = service.importar(arquivo);

        assertThat(resultado.criados()).isEqualTo(1);
        assertThat(resultado.ignorados()).isEqualTo(1);
        assertThat(resultado.erros()).hasSize(1);
    }

    @Test
    void importarCsv_exigeSangueVariasFormas_parseCorretamente() throws Exception {
        when(funcionarioRepo.findByMatricula(any())).thenReturn(Optional.empty());
        when(funcionarioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.importar(csv("001;Func A;UTI;Tec;;; sim;"));
        service.importar(csv("002;Func B;UTI;Tec;;;true;"));
        service.importar(csv("003;Func C;UTI;Tec;;;1;"));

        verify(funcionarioRepo, times(3)).save(argThat(Funcionario::isExigeSangue));
    }

    @Test
    void importar_registraAuditoria() throws Exception {
        when(funcionarioRepo.findByMatricula(any())).thenReturn(Optional.empty());
        when(funcionarioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.importar(csv("001;Func;UTI;Tec;;;;"));

        verify(auditService).registrar(eq("IMPORTACAO"), eq("Funcionario"), isNull(), anyString());
    }

    // ── helper ────────────────────────────────────────────────────────

    private MockMultipartFile csv(String linha) {
        String conteudo = "matricula;nome;setor;funcao;email;aso;exige_sangue;estabelecimento\n" + linha;
        return new MockMultipartFile("arquivo", "teste.csv", "text/csv",
            conteudo.getBytes(StandardCharsets.UTF_8));
    }
}
