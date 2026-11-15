package br.com.ressoa.avaliacao.dto;

import java.math.BigDecimal;
import java.time.Instant;

import br.com.ressoa.avaliacao.AvaliacaoRisco;
import br.com.ressoa.bairro.NivelRisco;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class AvaliacaoDtos {

    private AvaliacaoDtos() {
    }

    @Schema(description = """
            Avaliação de risco de um bairro. Se o nível não for informado, a API o calcula pelos gatilhos
            do Ressoa (incidência > 100/100 mil = VERMELHO; Rt > 1 = LARANJA; temp. mínima > 22 °C = AMARELO).""")
    public record AvaliacaoRequest(
            @Schema(example = "1") @NotNull(message = "Bairro é obrigatório") @Positive Long bairroId,
            @Schema(description = "Semana epidemiológica no formato AAAASS", example = "202647") @NotNull(message = "Semana epidemiológica é obrigatória") @Min(value = 200001, message = "Semana inválida (AAAASS)") @Max(value = 209953, message = "Semana inválida (AAAASS)") Integer semanaEpidemiologica,
            @Schema(description = "Opcional: se ausente, é calculado pelos indicadores", example = "LARANJA") NivelRisco nivel,
            @Schema(example = "23.4") @DecimalMin(value = "-10.0") @DecimalMax(value = "45.0") @Digits(integer = 3, fraction = 1) BigDecimal tempMinMedia,
            @Schema(example = "48.5") @DecimalMin(value = "0.0") @Digits(integer = 6, fraction = 2) BigDecimal casos100k,
            @Schema(example = "1.12") @DecimalMin(value = "0.0") @DecimalMax(value = "20.0") @Digits(integer = 3, fraction = 2) BigDecimal rt) {
    }

    public record AvaliacaoResponse(Long id, Long bairroId, String bairroNome, Integer semanaEpidemiologica,
            NivelRisco nivel, String acaoRecomendada, BigDecimal tempMinMedia, BigDecimal casos100k, BigDecimal rt,
            String orientacaoIa, Instant criadaEm) {

        public static AvaliacaoResponse de(AvaliacaoRisco a) {
            return new AvaliacaoResponse(a.getId(), a.getBairro().getId(), a.getBairro().getNome(),
                    a.getSemanaEpidemiologica(), a.getNivel(), a.getNivel().getAcaoRecomendada(),
                    a.getTempMinMedia(), a.getCasos100k(), a.getRt(), a.getOrientacaoIa(), a.getCriadaEm());
        }
    }
}
