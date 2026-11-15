package br.com.ressoa.auth;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.ressoa.auth.dto.AuthDtos.LoginRequest;
import br.com.ressoa.auth.dto.AuthDtos.RegistroRequest;
import br.com.ressoa.auth.dto.AuthDtos.TokenResponse;
import br.com.ressoa.auth.dto.AuthDtos.UsuarioResponse;
import br.com.ressoa.bairro.Bairro;
import br.com.ressoa.bairro.BairroRepository;
import br.com.ressoa.shared.exception.ConflitoException;
import br.com.ressoa.shared.exception.RecursoNaoEncontradoException;
import br.com.ressoa.usuario.Papel;
import br.com.ressoa.usuario.Usuario;
import br.com.ressoa.usuario.UsuarioRepository;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final BairroRepository bairros;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    /** Hash usado quando o e-mail não existe, para o tempo de resposta não revelar contas cadastradas. */
    private final String hashFicticio;

    public AuthService(UsuarioRepository usuarios, BairroRepository bairros, PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.usuarios = usuarios;
        this.bairros = bairros;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.hashFicticio = passwordEncoder.encode("ressoa-hash-ficticio");
    }

    @Transactional
    public TokenResponse registrar(RegistroRequest req) {
        if (usuarios.existsByEmailIgnoreCase(req.email())) {
            throw new ConflitoException("Já existe uma conta com este e-mail.");
        }
        Bairro bairro = bairros.findById(req.bairroId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Bairro", req.bairroId()));
        Usuario usuario = usuarios.save(new Usuario(req.nome(), req.email(),
                passwordEncoder.encode(req.senha()), Papel.CIDADAO, bairro));
        return token(usuario);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(req.email().trim()).orElse(null);
        String hash = usuario == null ? hashFicticio : usuario.getSenhaHash();
        if (!passwordEncoder.matches(req.senha(), hash) || usuario == null) {
            throw new BadCredentialsException("Credenciais inválidas");
        }
        return token(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse perfil(Long usuarioId) {
        return usuarios.findById(usuarioId)
                .map(UsuarioResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", usuarioId));
    }

    private TokenResponse token(Usuario usuario) {
        JwtService.TokenEmitido emitido = jwtService.emitir(usuario);
        return new TokenResponse(emitido.token(), "Bearer", emitido.expiraEm(), usuario.getPapel(),
                UsuarioResponse.de(usuario));
    }
}
