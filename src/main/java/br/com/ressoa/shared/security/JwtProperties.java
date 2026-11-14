package br.com.ressoa.shared.security;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do JWT HS256 compartilhado com a API .NET (mesmo segredo, emissor e audiência).
 */
@ConfigurationProperties(prefix = "ressoa.jwt")
public record JwtProperties(String secret, String issuer, String audience, long expiracaoMinutos) {

    private static final int TAMANHO_MINIMO_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET ausente ou curto: defina a variável de ambiente com pelo menos 32 bytes.");
        }
    }

    public byte[] secretBytes() {
        return secret.getBytes(StandardCharsets.UTF_8);
    }
}
