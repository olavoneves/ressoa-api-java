package br.com.ressoa.auth.dto;

import java.time.Instant;

import br.com.ressoa.usuario.Papel;
import br.com.ressoa.usuario.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** DTOs de autenticação. */
public final class AuthDtos {

    private AuthDtos() {
    }

    @Schema(description = "Cadastro de morador (papel CIDADAO)")
    public record RegistroRequest(
            @Schema(example = "Maria da Silva") @NotBlank(message = "Nome é obrigatório") @Size(min = 3, max = 120, message = "Nome deve ter entre 3 e 120 caracteres") String nome,
            @Schema(example = "maria@email.com") @NotBlank(message = "E-mail é obrigatório") @Email(message = "E-mail inválido") @Size(max = 160) String email,
            @Schema(example = "Senha@123") @NotBlank(message = "Senha é obrigatória") @Size(min = 8, max = 72, message = "Senha deve ter entre 8 e 72 caracteres") @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Senha deve conter letras e números") String senha,
            @Schema(example = "1") @NotNull(message = "Bairro é obrigatório") @Positive Long bairroId) {
    }

    public record LoginRequest(
            @Schema(example = "morador@ressoa.app") @NotBlank(message = "E-mail é obrigatório") @Email(message = "E-mail inválido") String email,
            @Schema(example = "Ressoa@2026") @NotBlank(message = "Senha é obrigatória") String senha) {
    }

    public record UsuarioResponse(Long id, String nome, String email, Papel papel, Long bairroId,
            String bairroNome, Instant criadoEm) {

        public static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPapel(),
                    u.getBairro() == null ? null : u.getBairro().getId(),
                    u.getBairro() == null ? null : u.getBairro().getNome(),
                    u.getCriadoEm());
        }
    }

    @Schema(description = "Token de acesso. Envie no header Authorization: Bearer {token}")
    public record TokenResponse(String token, String tipo, Instant expiraEm, Papel papel, UsuarioResponse usuario) {
    }
}
