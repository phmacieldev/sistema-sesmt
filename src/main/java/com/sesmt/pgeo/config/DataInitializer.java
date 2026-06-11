/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.config;

import com.sesmt.pgeo.model.Agendamento;
import com.sesmt.pgeo.model.Funcionario;
import com.sesmt.pgeo.model.MedicalLeave;
import com.sesmt.pgeo.model.Usuario;
import com.sesmt.pgeo.model.enums.Role;
import com.sesmt.pgeo.model.enums.StatusFuncionario;
import com.sesmt.pgeo.model.enums.TipoAtestado;
import com.sesmt.pgeo.model.enums.TipoExame;
import com.sesmt.pgeo.repository.AgendamentoRepository;
import com.sesmt.pgeo.repository.FuncionarioRepository;
import com.sesmt.pgeo.repository.MedicalLeaveRepository;
import com.sesmt.pgeo.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Popula o banco com dados iniciais ao subir a aplicação.
 * Executado apenas nos perfis dev e local.
 * Idempotente: verifica se os dados já existem antes de inserir.
 */
@Component
@Profile({"dev", "local"})
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UsuarioRepository usuarioRepo;
    private final FuncionarioRepository funcionarioRepo;
    private final AgendamentoRepository agendamentoRepo;
    private final MedicalLeaveRepository medicalLeaveRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("DataInitializer: verificando dados iniciais...");
        criarUsuarios();
        if (funcionarioRepo.count() == 0) {
            List<Funcionario> funcionarios = criarFuncionarios();
            criarAgendamentos(funcionarios);
            criarAtestados(funcionarios);
            log.info("DataInitializer: banco populado com dados de exemplo.");
        } else {
            log.info("DataInitializer: dados já existem, nenhuma ação necessária.");
        }
    }

    // ── Usuários ──────────────────────────────────────────────────────────────

    private void criarUsuarios() {
        criarUsuarioSeNaoExistir("admin",       "admin@123",  "Administrador do Sistema", Role.ADMIN);
        criarUsuarioSeNaoExistir("operador",    "oper@123",   "Operador SESMT",           Role.OPERADOR);
        criarUsuarioSeNaoExistir("visualizador","view@123",   "Visualizador",             Role.VISUALIZADOR);
    }

    private void criarUsuarioSeNaoExistir(String username, String senha, String nomeCompleto, Role role) {
        if (usuarioRepo.findByUsername(username).isEmpty()) {
            Usuario u = new Usuario();
            u.setUsername(username);
            u.setPassword(passwordEncoder.encode(senha));
            u.setNomeCompleto(nomeCompleto);
            u.setRole(role);
            u.setAtivo(true);
            usuarioRepo.save(u);
            log.info("DataInitializer: usuário '{}' criado.", username);
        }
    }

    // ── Funcionários ──────────────────────────────────────────────────────────

    private List<Funcionario> criarFuncionarios() {
        LocalDate hoje = LocalDate.now();

        List<Funcionario> lista = List.of(
            // ── HEAA ────────────────────────────────────────────────────────
            func("001234", "Ana Paula Ferreira",     "Enfermagem - UTI",        "Técnica de Enfermagem", "HEAA", true,  hoje.plusMonths(8)),
            func("001235", "Carlos Eduardo Souza",   "Enfermagem - Pronto Socorro","Enfermeiro",         "HEAA", true,  hoje.minusMonths(2)),  // vencido
            func("001236", "Mariana Costa Lima",     "Farmácia",                "Farmacêutica",          "HEAA", true,  hoje.plusDays(20)),    // a vencer
            func("001237", "João Batista Oliveira",  "Manutenção Predial",      "Eletricista",           "HEAA", true,  hoje.plusMonths(3)),
            func("001238", "Fernanda Rocha Alves",   "Administração",           "Assistente Administrativo","HEAA",false, hoje.plusMonths(11)),
            func("001239", "Roberto Mendes Silva",   "Radiologia",              "Técnico em Radiologia", "HEAA", true,  hoje.minusMonths(1)),  // vencido
            func("001240", "Patrícia Santos Gomes",  "Nutrição e Dietética",    "Nutricionista",         "HEAA", false, hoje.plusMonths(5)),
            func("001241", "Lucas Henrique Torres",  "Higiene e Limpeza",       "Auxiliar de Higienização","HEAA",true, null),                // sem ASO

            // ── FMC ─────────────────────────────────────────────────────────
            func("002101", "Beatriz Nascimento Cruz","Setor FMC - Clínica",     "Médica Clínica",        "FMC",  false, hoje.plusMonths(14)),
            func("002102", "Thiago Pereira Campos",  "Setor FMC - Laboratório", "Analista Clínico",      "FMC",  true,  hoje.plusDays(15)),   // a vencer
            func("002103", "Simone Cardoso Vieira",  "Setor FMC - Recepção",    "Recepcionista",         "FMC",  false, hoje.plusMonths(7)),

            // ── CSEC ────────────────────────────────────────────────────────
            func("003050", "Eduardo Lima Barbosa",   "Setor CSEC - Cirurgia",   "Instrumentador Cirúrgico","CSEC",true, hoje.plusMonths(2)),
            func("003051", "Amanda Moreira Freitas", "Setor CSEC - CME",        "Técnica em Esterilização","CSEC",true, hoje.minusDays(10)), // vencido

            // ── Pré-admissional (sem matrícula) ──────────────────────────────
            funcPreAdmissional("Paulo Ricardo Andrade",  "TI - Infraestrutura",  "Analista de TI",       "HEAA"),
            funcPreAdmissional("Gabriela Pinto Ramos",   "Fisioterapia",         "Fisioterapeuta",        "HEAA")
        );

        List<Funcionario> salvos = funcionarioRepo.saveAll(lista);
        log.info("DataInitializer: {} funcionários criados.", salvos.size());
        return salvos;
    }

    private Funcionario func(String matricula, String nome, String setor, String funcao,
                              String estabelecimento, boolean exigeSangue, LocalDate aso) {
        Funcionario f = new Funcionario();
        f.setMatricula(matricula);
        f.setNome(nome);
        f.setSetor(setor);
        f.setFuncao(funcao);
        f.setEstabelecimento(estabelecimento);
        f.setExigeSangue(exigeSangue);
        f.setAso(aso);
        f.setStatus(StatusFuncionario.ATIVO);
        f.setAtivo(true);
        return f;
    }

    private Funcionario funcPreAdmissional(String nome, String setor, String funcao, String estabelecimento) {
        Funcionario f = new Funcionario();
        f.setNome(nome);
        f.setSetor(setor);
        f.setFuncao(funcao);
        f.setEstabelecimento(estabelecimento);
        f.setExigeSangue(true);
        f.setStatus(StatusFuncionario.PRE_ADMISSIONAL);
        f.setAtivo(true);
        return f;
    }

    // ── Agendamentos ──────────────────────────────────────────────────────────

    private void criarAgendamentos(List<Funcionario> fs) {
        LocalDate hoje = LocalDate.now();

        List<Agendamento> lista = List.of(
            // Agendamentos passados (concluídos)
            ag(fs.get(1),  TipoExame.PERIODICO,           hoje.minusMonths(2), "07:30", hoje.minusMonths(2).minusDays(2), true,  true),
            ag(fs.get(5),  TipoExame.PERIODICO,           hoje.minusMonths(1), "08:00", hoje.minusMonths(1).minusDays(3), true,  true),
            ag(fs.get(12), TipoExame.PERIODICO,           hoje.minusDays(10),  "09:00", hoje.minusDays(12),               true,  false),
            ag(fs.get(0),  TipoExame.PERIODICO,           hoje.minusMonths(4), "10:00", hoje.minusMonths(4).minusDays(1), true,  true),
            ag(fs.get(6),  TipoExame.PERIODICO,           hoje.minusMonths(6), "07:00", null,                             true,  true),
            ag(fs.get(9),  TipoExame.PERIODICO,           hoje.minusMonths(3), "08:30", hoje.minusMonths(3).minusDays(2), true,  true),
            ag(fs.get(11), TipoExame.ADMISSIONAL,         hoje.minusMonths(5), "09:30", hoje.minusMonths(5).minusDays(1), true,  true),
            ag(fs.get(13), TipoExame.ADMISSIONAL,         hoje.minusDays(5),   "08:00", hoje.minusDays(7),                false, false),
            ag(fs.get(14), TipoExame.ADMISSIONAL,         hoje.minusDays(3),   "09:00", hoje.minusDays(5),                false, false),
            ag(fs.get(3),  TipoExame.RETORNO_AO_TRABALHO, hoje.minusDays(20),  "07:30", null,                             true,  true),
            ag(fs.get(7),  TipoExame.MUDANCA_DE_RISCO,    hoje.minusMonths(2), "10:30", hoje.minusMonths(2).minusDays(3), true,  true),

            // Agendamentos futuros (próximas semanas)
            ag(fs.get(2),  TipoExame.PERIODICO,           hoje.plusDays(7),    "08:00", hoje.plusDays(5),                 false, false),
            ag(fs.get(9),  TipoExame.PERIODICO,           hoje.plusDays(10),   "09:30", hoje.plusDays(8),                 false, false),
            ag(fs.get(1),  TipoExame.PERIODICO,           hoje.plusDays(14),   "07:30", hoje.plusDays(12),                false, false),
            ag(fs.get(5),  TipoExame.PERIODICO,           hoje.plusDays(21),   "08:00", hoje.plusDays(19),                false, false),
            ag(fs.get(12), TipoExame.PERIODICO,           hoje.plusDays(28),   "10:00", hoje.plusDays(26),                false, false),
            ag(fs.get(4),  TipoExame.DEMISSIONAL,         hoje.plusDays(3),    "07:00", null,                             false, false),
            ag(fs.get(10), TipoExame.RETORNO_AO_TRABALHO, hoje.plusDays(5),    "09:00", null,                             false, false),
            ag(fs.get(8),  TipoExame.PERIODICO,           hoje.plusDays(45),   "08:30", hoje.plusDays(43),                false, false),
            ag(fs.get(3),  TipoExame.PERIODICO,           hoje.plusMonths(3),  "07:30", hoje.plusMonths(3).minusDays(2),  false, false)
        );

        agendamentoRepo.saveAll(lista);
        log.info("DataInitializer: {} agendamentos criados.", lista.size());
    }

    private Agendamento ag(Funcionario func, TipoExame tipo, LocalDate dataClinico,
                            String horaClinico, LocalDate dataSangue,
                            boolean asoEnviado, boolean asoRecebido) {
        Agendamento a = new Agendamento();
        a.setFuncionario(func);
        a.setTipoExame(tipo);
        a.setDataClinico(dataClinico);
        a.setHoraClinico(horaClinico);
        a.setDataSangue(dataSangue);
        a.setAsoEnviado(asoEnviado);
        a.setAsoRecebido(asoRecebido);
        a.setCriadoPor("admin");
        return a;
    }

    // ── Atestados ─────────────────────────────────────────────────────────────

    private void criarAtestados(List<Funcionario> fs) {
        LocalDate hoje = LocalDate.now();

        List<MedicalLeave> lista = List.of(
            atestado(fs.get(1),  hoje.minusMonths(3),       5, "Gripe / Síndrome Gripal",   "J11",  "Dr. Roberto Alves",     "CRM 12345", TipoAtestado.DOENCA),
            atestado(fs.get(5),  hoje.minusMonths(2),       3, "Lombalgias",                "M54.5","Dra. Carla Mendes",     "CRM 67890", TipoAtestado.DOENCA),
            atestado(fs.get(7),  hoje.minusMonths(1),      15, "Fratura membro superior",   "S52",  "Dr. Paulo Nunes",       "CRM 11223", TipoAtestado.ACIDENTE_TRABALHO),
            atestado(fs.get(3),  hoje.minusDays(25),        2, "Cefaleia tensional",        "G44.2","Dra. Fernanda Costa",   "CRM 44556", TipoAtestado.DOENCA),
            atestado(fs.get(9),  hoje.minusDays(40),        7, "Gastroenterite aguda",      "A09",  "Dr. Marcos Ribeiro",    "CRM 77889", TipoAtestado.DOENCA),
            atestado(fs.get(12), hoje.minusMonths(5),      30, "Cirurgia eletiva",          "Z48",  "Dr. Antônio Figueiredo","CRM 99001", TipoAtestado.ACIDENTE_TRABALHO),
            atestado(fs.get(0),  hoje.minusMonths(4),       1, "Consulta médica",           "Z71",  "Dra. Juliana Torres",   "CRM 33445", TipoAtestado.DOENCA)
        );

        medicalLeaveRepo.saveAll(lista);
        log.info("DataInitializer: {} atestados criados.", lista.size());
    }

    private MedicalLeave atestado(Funcionario func, LocalDate dataAfastamento, int dias,
                                   String motivo, String cid, String medicoNome, String medicoCrm,
                                   TipoAtestado tipo) {
        MedicalLeave ml = new MedicalLeave();
        ml.setFuncionario(func);
        ml.setDataAfastamento(dataAfastamento);
        ml.setDiasAfastamento(dias);
        ml.setMotivo(motivo);
        ml.setCid(cid);
        ml.setMedicoNome(medicoNome);
        ml.setMedicoCrm(medicoCrm);
        ml.setTipo(tipo);
        ml.setDataLancamento(LocalDateTime.now());
        return ml;
    }
}
