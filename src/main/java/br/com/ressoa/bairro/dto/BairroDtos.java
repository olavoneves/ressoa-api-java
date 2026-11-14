package br.com.ressoa.bairro.dto;

import br.com.ressoa.bairro.Bairro;
import br.com.ressoa.bairro.NivelRisco;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class BairroDtos {

    private BairroDtos() {
    }

    @Schema(description = "Dados de um bairro. O nível de risco não é editável aqui: muda pelas avaliações de risco.")
    public record BairroRequest(
            @Schema(example = "Jardim Ângela") @NotBlank(message = "Nome é obrigatório") @Size(max = 120) String nome,
            @Schema(example = "São Paulo") @NotBlank(message = "Município é obrigatório") @Size(max = 120) String municipio,
            @Schema(example = "SP") @NotBlank(message = "UF é obrigatória") @Pattern(regexp = "^[A-Za-z]{2}$", message = "UF deve ter 2 letras") String uf,
            @Schema(example = "3550308") @NotBlank(message = "Código IBGE é obrigatório") @Pattern(regexp = "^\\d{7}$", message = "Código IBGE deve ter 7 dígitos") String codigoIbge,
            @Schema(example = "true") @NotNull(message = "Informe se o abastecimento de água é intermitente") Boolean aguaIntermitente) {
    }

    public record BairroResponse(Long id, String nome, String municipio, String uf, String codigoIbge,
            boolean aguaIntermitente, NivelRisco nivelRisco, String acaoRecomendada) {

        public static BairroResponse de(Bairro b) {
            return new BairroResponse(b.getId(), b.getNome(), b.getMunicipio(), b.getUf(), b.getCodigoIbge(),
                    b.isAguaIntermitente(), b.getNivelRisco(), b.getNivelRisco().getAcaoRecomendada());
        }
    }
}
