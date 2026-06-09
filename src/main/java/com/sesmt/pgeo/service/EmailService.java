package com.sesmt.pgeo.service;

import com.sesmt.pgeo.config.NotificacaoProperties;
import com.sesmt.pgeo.model.Funcionario;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final NotificacaoProperties props;

    @Async
    public void enviarAlertaAso(List<Funcionario> vencidos, List<Funcionario> aVencer) {
        if (!props.isHabilitado()) return;
        if (props.getDestinatario() == null || props.getDestinatario().isBlank()) {
            log.warn("EmailService: notificacao habilitada mas destinatario nao configurado.");
            return;
        }

        try {
            Context ctx = new Context(new Locale("pt", "BR"));
            ctx.setVariable("vencidos",  vencidos);
            ctx.setVariable("aVencer",   aVencer);
            ctx.setVariable("diasAviso", props.getDiasAviso());
            ctx.setVariable("hoje",      LocalDate.now());

            String html = templateEngine.process("email/aso_vencimento", ctx);

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(props.getRemetente());
            helper.setTo(props.getDestinatario());
            helper.setSubject(String.format(
                "PGEO — Alerta ASO: %d vencido(s), %d a vencer",
                vencidos.size(), aVencer.size()));
            helper.setText(html, true);

            mailSender.send(msg);
            log.info("EmailService: alerta ASO enviado para {} ({} vencidos, {} a vencer)",
                props.getDestinatario(), vencidos.size(), aVencer.size());

        } catch (MessagingException e) {
            log.error("EmailService: falha ao enviar alerta ASO", e);
        }
    }
}
