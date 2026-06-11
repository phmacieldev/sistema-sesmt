/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @EnableAsync    → habilita @Async no AuditService (log em background)
 * @EnableScheduling → habilita @Scheduled (jobs futuros: sincronização, relatórios)
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class PgeoApplication {
    public static void main(String[] args) {
        SpringApplication.run(PgeoApplication.class, args);
    }
}
