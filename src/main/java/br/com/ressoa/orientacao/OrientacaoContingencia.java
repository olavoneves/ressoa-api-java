package br.com.ressoa.orientacao;

import br.com.ressoa.bairro.Bairro;
import br.com.ressoa.bairro.NivelRisco;

/**
 * Orientação de contingência (sem LLM), usada quando a IA está indisponível ou desligada.
 * Garante que o morador e o gestor sempre recebam uma resposta útil (ADR-006).
 */
final class OrientacaoContingencia {

    private OrientacaoContingencia() {
    }

    static String morador(NivelRisco nivel) {
        String base = """
                - Mantenha o aparelho Ressoa ligado dentro do recipiente e a tampa bem fechada.
                - Lave tambores e baldes com escova uma vez por semana.
                - Elimine água parada em pratos de plantas, pneus e garrafas.
                """;
        String especifico = switch (nivel) {
            case VERDE -> "- O risco está baixo, mas a seca aumenta a água guardada: continue a prevenção.\n";
            case AMARELO -> "- O calor favorece o mosquito: confira hoje se o Ressoa está ativo no app.\n"
                    + "- Participe do mutirão contra criadouros do seu bairro.\n";
            case LARANJA -> "- Há transmissão no bairro: use repelente, principalmente em bebês, gestantes e idosos.\n"
                    + "- Com febre, procure a UBS e beba bastante água.\n";
            case VERMELHO -> "- Epidemia no bairro: com febre, procure atendimento e beba bastante água.\n"
                    + "- Bebês, gestantes e idosos com febre devem ser atendidos o quanto antes.\n";
        };
        return base + especifico
                + "- Sinais de alarme (dor na barriga forte, vômitos, sangramento): vá ao pronto atendimento já.";
    }

    static String gestor(Bairro bairro) {
        NivelRisco nivel = bairro.getNivelRisco();
        return """
                **Situação:** %s (%s/%s) em nível **%s**.

                **Ações imediatas (protocolo Ressoa):** %s.

                **Público prioritário:** bebês até 2 anos, gestantes e idosos acima de 65 anos.

                **Indicadores para acompanhar:** percentual de aparelhos ativos no bairro, incidência por 100 mil
                habitantes, Rt semanal e índice LIRAa.

                _Plano gerado pelo protocolo padrão (IA indisponível no momento)._"""
                .formatted(bairro.getNome(), bairro.getMunicipio(), bairro.getUf(), nivel,
                        nivel.getAcaoRecomendada());
    }
}
