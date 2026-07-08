/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.service;

import com.sesmt.pgeo.audit.AuditService;
import com.sesmt.pgeo.exception.RegraDeNegocioException;
import com.sesmt.pgeo.exception.RecursoNaoEncontradoException;
import com.sesmt.pgeo.model.Agendamento;
import com.sesmt.pgeo.model.Funcionario;
import com.sesmt.pgeo.model.enums.StatusFuncionario;
import com.sesmt.pgeo.model.enums.TipoExame;
import com.sesmt.pgeo.repository.AgendamentoRepository;
import com.sesmt.pgeo.repository.FuncionarioRepository;
import com.sesmt.pgeo.util.AppConstants;
import com.sesmt.pgeo.util.SecurityUtils;
import com.sesmt.pgeo.websocket.NotificacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Fix: FuncionarioService REMOVIDO desta classe para eliminar o risco de
 * circular dependency no contexto do Spring.
 *
 * A lógica de criar pré-admissional agora fica aqui diretamente,
 * usando o FuncionarioRepository — sem precisar de outro Service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private static final int LIMITE_SANGUE_DIA = AppConstants.LIMITE_SANGUE_DIA;

    private static final List<String> HORARIOS = List.of(
        "08:00","08:30","09:00","09:30",
        "10:00","10:30","11:00",
        "13:30","14:00","14:30","15:00","15:30"
    );

    private final AgendamentoRepository agendamentoRepo;
    private final FuncionarioRepository  funcionarioRepo;
    private final AuditService           auditService;
    private final NotificacaoService     notificacaoService;

    public List<String> getHorariosDisponiveis() { return HORARIOS; }

    public List<String> getHorariosDisponiveisPorData(LocalDate data) {
        return HORARIOS; // sem limite por horário no clínico
    }

    public long countSanguePorData(LocalDate data) {
        return agendamentoRepo.countByDataSangue(data);
    }

    public boolean sangunePodeAgendar(LocalDate dataSangue) {
        if (dataSangue == null) return true;
        return agendamentoRepo.countByDataSangue(dataSangue) < LIMITE_SANGUE_DIA;
    }

    @Transactional
    public Agendamento criar(
            String matricula, String nome, String setor, String funcao,
            TipoExame tipoExame, LocalDate dataClinico, String hora,
            LocalDate dataSangue, String observacoes, String examesSangue) {

        validarDiaUtil(dataClinico, "exame clínico");
        if (dataSangue != null) validarDiaUtil(dataSangue, "exame de sangue");

        if (dataSangue != null && !sangunePodeAgendar(dataSangue)) {
            long atual = agendamentoRepo.countByDataSangue(dataSangue);
            throw new RegraDeNegocioException(
                "Limite de " + LIMITE_SANGUE_DIA + " exames de sangue atingido para "
                + dataSangue + " (" + atual + "/" + LIMITE_SANGUE_DIA + ").");
        }

        int anoAtual = Year.now().getValue();
        if (matricula != null && !matricula.isBlank()) {
            agendamentoRepo.findByMatriculaEAno(matricula.strip(), anoAtual)
                .ifPresent(ag -> {
                    throw new RegraDeNegocioException(
                        "DUPLICADO:" + ag.getId()
                        + " — " + ag.getFuncionarioNome()
                        + " já possui agendamento em "
                        + ag.getDataClinico() + " às " + ag.getHoraClinico());
                });
        }

        if ((matricula == null || matricula.isBlank()) && tipoExame != TipoExame.ADMISSIONAL) {
            throw new RegraDeNegocioException("Matrícula é obrigatória para este tipo de exame.");
        }

        // Gera matrícula temporária para admissional sem matrícula
        if (matricula == null || matricula.isBlank()) {
            matricula = gerarMatriculaAdmissional();
        }

        String mat = matricula.strip();

        // Busca funcionário existente ou cria novo (pré-admissional ou novo cadastro)
        Funcionario funcionario = funcionarioRepo.findByMatricula(mat)
            .orElseGet(() -> criarFuncionario(mat, nome, setor, funcao, tipoExame));

        Agendamento ag = new Agendamento();
        ag.setFuncionario(funcionario);
        ag.setTipoExame(tipoExame);
        ag.setDataClinico(dataClinico);
        ag.setHoraClinico(hora);
        ag.setDataSangue(dataSangue);
        ag.setAsoEnviado(false);
        ag.setAsoRecebido(false);
        ag.setObservacoes(observacoes);
        ag.setExamesSangue(examesSangue);
        ag.setCriadoPor(getUsuarioAtual());
        ag.syncCacheDoFuncionario();

        Agendamento salvo = agendamentoRepo.save(ag);
        String usuario = getUsuarioAtual();
        auditService.registrarCriacao("Agendamento", salvo.getId(),
            "Agendamento de " + funcionario.getNome() + " para " + dataClinico);
        notificacaoService.broadcastCriacao(salvo, usuario);
        return salvo;
    }

    /**
     * Cria funcionário diretamente — sem depender do FuncionarioService.
     * Admissional → PRE_ADMISSIONAL. Qualquer outro tipo → ATIVO (cadastro manual).
     */
    private Funcionario criarFuncionario(String mat, String nome, String setor,
                                         String funcao, TipoExame tipo) {
        Funcionario f = new Funcionario();
        f.setMatricula(mat);
        f.setNome(nome != null ? nome.strip() : "");
        f.setSetor(setor);
        f.setFuncao(funcao);
        f.setExigeSangue(true);
        f.setStatus(tipo == TipoExame.ADMISSIONAL
            ? StatusFuncionario.PRE_ADMISSIONAL
            : StatusFuncionario.ATIVO);
        f.setAtivo(true);
        Funcionario salvo = funcionarioRepo.save(f);
        auditService.registrarCriacao("Funcionario", salvo.getId(),
            (tipo == TipoExame.ADMISSIONAL ? "Pré-admissional" : "Funcionário") + " criado: " + nome);
        return salvo;
    }

    @Transactional
    public Agendamento editar(Long id, String nome, String setor, String funcao,
                              TipoExame tipoExame, LocalDate dataClinico,
                              LocalDate dataSangue, String hora, String observacoes,
                              String examesSangue) {

        Agendamento ag = agendamentoRepo.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", id));

        validarDiaUtil(dataClinico, "exame clínico");
        if (dataSangue != null) validarDiaUtil(dataSangue, "exame de sangue");

        boolean mudouSangue = dataSangue != null && !dataSangue.equals(ag.getDataSangue());
        if (mudouSangue && !sangunePodeAgendar(dataSangue)) {
            throw new RegraDeNegocioException(
                "Limite de " + LIMITE_SANGUE_DIA + " exames de sangue atingido para " + dataSangue + ".");
        }

        String descAntes = ag.getTipoExameDescricao() + " | " + ag.getDataClinico() + " " + ag.getHoraClinico();

        atualizarDadosFuncionario(ag, nome, setor, funcao);

        ag.setTipoExame(tipoExame);
        ag.setDataClinico(dataClinico);
        ag.setDataSangue(dataSangue);
        ag.setHoraClinico(hora);
        ag.setObservacoes(observacoes);
        ag.setExamesSangue(examesSangue);
        ag.setAtualizadoPor(getUsuarioAtual());

        Agendamento salvo = agendamentoRepo.save(ag);
        String descDepois = salvo.getTipoExameDescricao() + " | " + salvo.getDataClinico() + " " + salvo.getHoraClinico();
        auditService.registrarEdicao("Agendamento", id,
            "Edição de " + ag.getFuncionarioNome(), descAntes, descDepois);
        notificacaoService.broadcastEdicao(salvo, getUsuarioAtual());
        return salvo;
    }

    /**
     * funcionarioNome/Setor/Funcao no Agendamento são só cache: o
     * @PreUpdate (Agendamento.syncCacheDoFuncionario) os resincroniza a
     * partir do Funcionario vinculado antes de cada flush. Setar esses
     * campos direto no Agendamento seria descartado silenciosamente no
     * mesmo save — a correção precisa ir para o Funcionario de fato.
     */
    private void atualizarDadosFuncionario(Agendamento ag, String nome, String setor, String funcao) {
        Funcionario f = ag.getFuncionario();
        if (f == null) return;

        String nomeNovo = nome != null ? nome.strip() : null;
        boolean mudou = !Objects.equals(f.getNome(), nomeNovo)
            || !Objects.equals(f.getSetor(), setor)
            || !Objects.equals(f.getFuncao(), funcao);
        if (!mudou) return;

        String antes = f.getNome() + " | " + f.getSetor() + " | " + f.getFuncao();
        f.setNome(nomeNovo);
        f.setSetor(setor);
        f.setFuncao(funcao);
        funcionarioRepo.save(f);
        auditService.registrarEdicao("Funcionario", f.getId(),
            "Dados corrigidos via edição de agendamento",
            antes, nomeNovo + " | " + setor + " | " + funcao);
    }

    @Transactional
    public void excluir(Long id) {
        Agendamento ag = agendamentoRepo.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", id));
        String nome = ag.getFuncionarioNome();
        String usuario = getUsuarioAtual();
        agendamentoRepo.delete(ag);
        auditService.registrarExclusao("Agendamento", id,
            "Exclusão de " + nome, ag.getDataClinico() + " " + ag.getHoraClinico());
        notificacaoService.broadcastExclusao(id, nome, usuario);
    }

    @Transactional
    public Agendamento mover(Long id, LocalDate novaData, String novaHora) {
        Agendamento ag = agendamentoRepo.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", id));
        validarDiaUtil(novaData, "exame clínico");
        ag.setDataClinico(novaData);
        ag.setHoraClinico(novaHora);
        ag.setAtualizadoPor(getUsuarioAtual());
        Agendamento salvo = agendamentoRepo.save(ag);
        auditService.registrar("MOVER", "Agendamento", id,
            ag.getFuncionarioNome() + " → " + novaData + " " + novaHora);
        notificacaoService.broadcastEdicao(salvo, getUsuarioAtual());
        return salvo;
    }

    private void validarDiaUtil(LocalDate data, String contexto) {
        if (data == null) return;
        DayOfWeek dia = data.getDayOfWeek();
        if (dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY) {
            throw new RegraDeNegocioException(
                "Não é possível agendar " + contexto + " em fim de semana (" + data + ").");
        }
    }

    private String gerarMatriculaAdmissional() {
        int ano = Year.now().getValue();
        long proximo = funcionarioRepo.count() + 1;
        return String.format("ADM%d%04d", ano, proximo);
    }

    private static String getUsuarioAtual() {
        return SecurityUtils.getUsuarioAtual();
    }
}
