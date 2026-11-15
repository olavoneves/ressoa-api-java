package br.com.ressoa.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import br.com.ressoa.usuario.Papel;

/**
 * Dados do usuário extraídos do JWT da requisição atual (sem ir ao banco).
 */
public record UsuarioAutenticado(Long id, String email, Papel papel, Long bairroId) {

    public static final String CLAIM_PAPEL = "papel";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_BAIRRO = "bairroId";

    public static UsuarioAutenticado de(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken token)) {
            throw new IllegalStateException("Requisição sem autenticação JWT");
        }
        Jwt jwt = token.getToken();
        Number bairro = jwt.getClaim(CLAIM_BAIRRO);
        return new UsuarioAutenticado(
                Long.valueOf(jwt.getSubject()),
                jwt.getClaimAsString(CLAIM_EMAIL),
                Papel.valueOf(jwt.getClaimAsString(CLAIM_PAPEL)),
                bairro == null ? null : bairro.longValue());
    }

    public boolean isGestor() {
        return papel == Papel.GESTOR;
    }
}
