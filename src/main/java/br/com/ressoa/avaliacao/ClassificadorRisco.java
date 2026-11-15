package br.com.ressoa.avaliacao;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.ressoa.bairro.NivelRisco;

/**
 * Aplica a tabela de gatilhos do Ressoa (proposta inicial, a calibrar) quando o nível não é informado.
 * Mesma regra da função PL/SQL {@code fn_nivel_risco}.
 */
@Component
public class ClassificadorRisco {

    static final BigDecimal LIMITE_INCIDENCIA = new BigDecimal("100");
    static final BigDecimal LIMITE_RT = BigDecimal.ONE;
    static final BigDecimal LIMITE_TEMP_MIN = new BigDecimal("22");

    public NivelRisco classificar(BigDecimal casos100k, BigDecimal rt, BigDecimal tempMinMedia) {
        if (casos100k != null && casos100k.compareTo(LIMITE_INCIDENCIA) > 0) {
            return NivelRisco.VERMELHO;
        }
        if (rt != null && rt.compareTo(LIMITE_RT) > 0) {
            return NivelRisco.LARANJA;
        }
        if (tempMinMedia != null && tempMinMedia.compareTo(LIMITE_TEMP_MIN) > 0) {
            return NivelRisco.AMARELO;
        }
        return NivelRisco.VERDE;
    }
}
