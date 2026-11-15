package br.com.ressoa.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI ressoaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ressoa — API principal")
                        .version("v1")
                        .description("""
                                Matar a larva com som antes que a seca vire epidemia.

                                API do núcleo do Ressoa: contas (CIDADAO e GESTOR), bairros, aparelhos de ultrassom,
                                avaliações de risco de dengue, orientação gerada por IA e alertas aos moradores.
                                Faça login em `POST /api/auth/login`, copie o `token` e clique em **Authorize**.""")
                        .contact(new Contact().name("Equipe Ressoa — FIAP 2TDS")
                                .url("https://github.com/olavoneves/ressoa-api-java")))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
