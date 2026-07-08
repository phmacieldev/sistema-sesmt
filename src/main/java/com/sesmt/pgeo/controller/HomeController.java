/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.controller;

import com.sesmt.pgeo.model.Agendamento;
import com.sesmt.pgeo.model.Funcionario;
import com.sesmt.pgeo.model.MedicalLeave;
import com.sesmt.pgeo.repository.AgendamentoRepository;
import com.sesmt.pgeo.repository.FuncionarioRepository;
import com.sesmt.pgeo.repository.MedicalLeaveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final AgendamentoRepository agendamentoRepo;
    private final FuncionarioRepository funcionarioRepo;
    private final MedicalLeaveRepository medicalLeaveRepo;

    private static final int ASO_POR_PAG = 20;

    @GetMapping("/home")
    public String home(@RequestParam(defaultValue = "0") int paginaAso, Model model) {
        LocalDate hoje = LocalDate.now();

        // Agendamentos: este mês e próximos 7 dias
        List<Agendamento> doMes = agendamentoRepo.findByMesEAno(hoje.getMonthValue(), hoje.getYear());
        long proximos7dias = doMes.stream()
            .filter(a -> a.getDataClinico() != null
                      && !a.getDataClinico().isBefore(hoje)
                      && !a.getDataClinico().isAfter(hoje.plusDays(7)))
            .count();

        // ASO: vencidos (paginados) e a vencer em 30 dias
        List<Funcionario> todosAsoVencidos = funcionarioRepo.findByAsoVencendoAte(hoje.minusDays(1));
        List<Funcionario> asoAVencer30 = funcionarioRepo.findByAsoVencendoAte(hoje.plusDays(30))
            .stream().filter(f -> !f.getAso().isBefore(hoje)).toList();

        int totalAso     = todosAsoVencidos.size();
        int totalPagAso  = Math.max(1, (int) Math.ceil((double) totalAso / ASO_POR_PAG));
        int paginaAsoAtual = Math.max(0, Math.min(paginaAso, totalPagAso - 1));
        int ini = paginaAsoAtual * ASO_POR_PAG;
        List<Funcionario> asoVencidosPagina = todosAsoVencidos.subList(ini, Math.min(ini + ASO_POR_PAG, totalAso));

        // Atestados: últimos 60 dias (KPI) e últimos 30 dias (painel, limit 5)
        List<MedicalLeave> atestadosRecentes = medicalLeaveRepo.findRecentes(hoje.minusDays(60));
        long diasAfastamentoTotal = atestadosRecentes.stream()
            .mapToLong(MedicalLeave::getDiasAfastamento).sum();
        List<MedicalLeave> ultimosAtestados = medicalLeaveRepo
            .findRecentes(hoje.minusDays(30)).stream().limit(5).toList();

        // Próximos agendamentos (até 14 dias)
        List<Agendamento> proximosAgendamentos = agendamentoRepo
            .findByDataClinicoDesde(hoje).stream()
            .filter(a -> !a.getDataClinico().isAfter(hoje.plusDays(14)))
            .limit(8)
            .toList();

        model.addAttribute("totalMes",            doMes.size());
        model.addAttribute("proximos7dias",        proximos7dias);
        model.addAttribute("totalAsoVencidos",     totalAso);
        model.addAttribute("totalAsoAVencer30",    asoAVencer30.size());
        model.addAttribute("totalAtestados60dias", atestadosRecentes.size());
        model.addAttribute("diasAfastamento",      diasAfastamentoTotal);
        model.addAttribute("proximosAgendamentos", proximosAgendamentos);
        model.addAttribute("ultimosAtestados",     ultimosAtestados);
        model.addAttribute("asoVencidos",          asoVencidosPagina);
        model.addAttribute("totalPaginasAso",      totalPagAso);
        model.addAttribute("paginaAsoAtual",       paginaAsoAtual);
        model.addAttribute("totalItensAso",        totalAso);
        model.addAttribute("hoje",                 hoje);

        return "home";
    }
}
