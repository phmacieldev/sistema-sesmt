package com.sesmt.pgeo.scheduler;

import com.sesmt.pgeo.config.NotificacaoProperties;
import com.sesmt.pgeo.model.Funcionario;
import com.sesmt.pgeo.repository.FuncionarioRepository;
import com.sesmt.pgeo.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AsoNotificacaoScheduler {

    private final FuncionarioRepository funcionarioRepo;
    private final EmailService emailService;
    private final NotificacaoProperties props;

    /**
     * Roda às 07:00 em dias úteis (seg–sex), fuso de Brasília.
     * Busca ativos com ASO vencido ou a vencer nos próximos N dias e envia resumo por e-mail.
     */
    @Scheduled(cron = "0 0 7 * * MON-FRI", zone = "America/Sao_Paulo")
    public void verificarVencimentosAso() {
        if (!props.isHabilitado()) return;

        LocalDate hoje  = LocalDate.now();
        LocalDate limite = hoje.plusDays(props.getDiasAviso());

        List<Funcionario> todos = funcionarioRepo.findByAsoVencendoAte(limite);
        if (todos.isEmpty()) {
            log.info("AsoNotificacao: nenhum vencimento encontrado.");
            return;
        }

        List<Funcionario> vencidos = todos.stream()
            .filter(f -> f.getAso().isBefore(hoje))
            .toList();
        List<Funcionario> aVencer = todos.stream()
            .filter(f -> !f.getAso().isBefore(hoje))
            .toList();

        log.info("AsoNotificacao: {} vencidos, {} a vencer — enviando e-mail.",
            vencidos.size(), aVencer.size());
        emailService.enviarAlertaAso(vencidos, aVencer);
    }
}
