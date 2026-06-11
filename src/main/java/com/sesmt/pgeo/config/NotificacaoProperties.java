/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "pgeo.notificacao.email")
@Data
public class NotificacaoProperties {

    /** Liga/desliga o envio de e-mail. Padrão false para não enviar sem SMTP configurado. */
    private boolean habilitado = false;

    /** Endereço que receberá o resumo diário (ex: sesmt@hospital.com.br). */
    private String destinatario;

    /** Endereço remetente (From:) do e-mail. */
    private String remetente = "pgeo@noreply.local";

    /** Quantos dias antes do vencimento o funcionário entra no alerta de "a vencer". */
    private int diasAviso = 30;
}
