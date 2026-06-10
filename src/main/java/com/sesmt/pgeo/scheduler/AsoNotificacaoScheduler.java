package com.sesmt.pgeo.scheduler;

import com.sesmt.pgeo.audit.AuditService;
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
    private final EmailService          emailService;
    private final NotificacaoProperties props;
    private final AuditService          auditService;

    /**
     * Roda às 07:00 em dias úteis (seg–sex), fuso de Brasília.
     * Registra no log de auditoria e envia e-mail se habilitado.
     */
    @Scheduled(cron = "0 0 7 * * MON-FRI", zone = "America/Sao_Paulo")
    public void verificarVencimentosAso() {
        LocalDate hoje   = LocalDate.now();
        LocalDate limite = hoje.plusDays(props.getDiasAviso());

        List<Funcionario> todos = funcionarioRepo.findByAsoVencendoAte(limite);

        List<Funcionario> vencidos = todos.stream()
            .filter(f -> f.getAso().isBefore(hoje)).toList();
        List<Funcionario> aVencer  = todos.stream()
            .filter(f -> !f.getAso().isBefore(hoje)).toList();

        String descricao = String.format(
            "Verificação automática: %d vencido(s), %d a vencer em %d dias.",
            vencidos.size(), aVencer.size(), props.getDiasAviso());

        log.info("AsoNotificacao: {}", descricao);

        // Sempre registra no log de auditoria (visível em /admin/auditoria)
        auditService.registrar("ASO_VERIFICACAO", "Agendamento", null, descricao);

        if (props.isHabilitado() && !todos.isEmpty()) {
            log.info("AsoNotificacao: enviando e-mail para {}.", props.getDestinatario());
            emailService.enviarAlertaAso(vencidos, aVencer);
        }
    }
}
