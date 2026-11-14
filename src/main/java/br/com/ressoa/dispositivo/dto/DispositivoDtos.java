package br.com.ressoa.dispositivo.dto;

import java.time.Instant;

import br.com.ressoa.dispositivo.Dispositivo;
import br.com.ressoa.dispositivo.StatusDispositivo;
import br.com.ressoa.dispositivo.TipoRecipiente;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public final class DispositivoDtos {

    private DispositivoDtos() {
    }

    @Schema(description = "Cadastro de aparelho. Para CIDADAO o dono é sempre o próprio usuário e o bairro padrão é o dele.")
    public record DispositivoCriarRequest(
            @Schema(example = "RSA-2026-0001") @NotBlank(message = "Número de série é obrigatório") @Pattern(regexp = "^[A-Za-z0-9-]{4,40}$", message = "Número de série deve ter de 4 a 40 letras, números ou hífen") String numeroSerie,
            @Schema(example = "TAMBOR") @NotNull(message = "Tipo de recipiente é obrigatório") TipoRecipiente tipoRecipiente,
            @Schema(example = "200") @NotNull(message = "Volume é obrigatório") @Min(value = 1, message = "Volume mínimo é 1 litro") @Max(value = 20000, message = "Volume máximo é 20000 litros") Integer volumeLitros,
            @Schema(example = "1") @Min(value = 1, message = "Mínimo de 1 pulso por dia") @Max(value = 24, message = "Máximo de 24 pulsos por dia") Integer pulsosPorDia,
            @Schema(description = "Opcional para CIDADAO (usa o bairro do cadastro)", example = "1") @Positive Long bairroId,
            @Schema(description = "Somente GESTOR: dono do aparelho", example = "2") @Positive Long usuarioId) {
    }

    public record DispositivoAtualizarRequest(
            @Schema(example = "CAIXA_DAGUA") @NotNull(message = "Tipo de recipiente é obrigatório") TipoRecipiente tipoRecipiente,
            @Schema(example = "500") @NotNull(message = "Volume é obrigatório") @Min(value = 1, message = "Volume mínimo é 1 litro") @Max(value = 20000, message = "Volume máximo é 20000 litros") Integer volumeLitros,
            @Schema(example = "2") @NotNull(message = "Pulsos por dia é obrigatório") @Min(value = 1, message = "Mínimo de 1 pulso por dia") @Max(value = 24, message = "Máximo de 24 pulsos por dia") Integer pulsosPorDia,
            @Schema(example = "1") @NotNull(message = "Bairro é obrigatório") @Positive Long bairroId,
            @Schema(description = "Opcional: ATIVO ou INATIVO (os demais são definidos pela telemetria)", example = "ATIVO") StatusDispositivo status) {
    }

    public record DispositivoResponse(Long id, String numeroSerie, TipoRecipiente tipoRecipiente, Integer volumeLitros,
            Integer pulsosPorDia, StatusDispositivo status, Instant instaladoEm, Long usuarioId, String usuarioNome,
            Long bairroId, String bairroNome) {

        public static DispositivoResponse de(Dispositivo d) {
            return new DispositivoResponse(d.getId(), d.getNumeroSerie(), d.getTipoRecipiente(), d.getVolumeLitros(),
                    d.getPulsosPorDia(), d.getStatus(), d.getInstaladoEm(), d.getUsuario().getId(),
                    d.getUsuario().getNome(), d.getBairro().getId(), d.getBairro().getNome());
        }
    }
}
