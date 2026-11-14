package br.com.ressoa.avaliacao;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import br.com.ressoa.bairro.NivelRisco;

class ClassificadorRiscoTest {

    private final ClassificadorRisco classificador = new ClassificadorRisco();

    @ParameterizedTest(name = "casos={0}, rt={1}, temp={2} -> {3}")
    @DisplayName("aplica a tabela de gatilhos do Ressoa, do mais grave para o mais leve")
    @CsvSource(nullValues = "null", value = {
            "150.0, 0.8, 20.0, VERMELHO",
            "100.0, 1.20, 20.0, LARANJA",
            "50.0, 1.00, 23.0, AMARELO",
            "50.0, 0.90, 22.0, VERDE",
            "null, null, null, VERDE",
            "101, 2.5, 25, VERMELHO"
    })
    void classifica(BigDecimal casos, BigDecimal rt, BigDecimal temp, NivelRisco esperado) {
        assertThat(classificador.classificar(casos, rt, temp)).isEqualTo(esperado);
    }
}
