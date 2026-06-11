/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pgeoOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("PGEO — Sistema de Exames Ocupacionais (SESMT)")
                .description("API REST para agendamento e gestão de exames ocupacionais (ASO). " +
                    "Autenticação via sessão web — faça login em /login antes de usar os endpoints.")
                .version("1.0.0")
                .contact(new Contact()
                    .name("SESMT")
                    .email("sesmt@empresa.com.br")));
    }
}
