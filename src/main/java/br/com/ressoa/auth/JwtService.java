package br.com.ressoa.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import br.com.ressoa.shared.security.JwtProperties;
import br.com.ressoa.shared.security.UsuarioAutenticado;
import br.com.ressoa.usuario.Usuario;

/** Emite o JWT de acesso. Claims combinadas com a API .NET (ADR-004). */
@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final JwtProperties props;

    public JwtService(JwtEncoder encoder, JwtProperties props) {
        this.encoder = encoder;
        this.props = props;
    }

    public TokenEmitido emitir(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(props.expiracaoMinutos(), ChronoUnit.MINUTES);
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .audience(java.util.List.of(props.audience()))
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .claim(UsuarioAutenticado.CLAIM_EMAIL, usuario.getEmail())
                .claim(UsuarioAutenticado.CLAIM_PAPEL, usuario.getPapel().name());
        if (usuario.getBairro() != null) {
            claims.claim(UsuarioAutenticado.CLAIM_BAIRRO, usuario.getBairro().getId());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.from(JwsAlgorithms.HS256)).type("JWT").build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new TokenEmitido(token, expiraEm);
    }

    public record TokenEmitido(String token, Instant expiraEm) {
    }
}
