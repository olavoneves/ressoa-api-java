package br.com.ressoa.orientacao;

import java.math.BigDecimal;

import br.com.ressoa.avaliacao.AvaliacaoRisco;
import br.com.ressoa.bairro.Bairro;

/** Prompts do Spring AI. Ficam isolados para facilitar ajuste e explicação na apresentação. */
final class PromptsOrientacao {

    private PromptsOrientacao() {
    }

    static final String SISTEMA_MORADOR = """
            Você é o assistente de saúde do Ressoa, um aparelho de ultrassom que mata larvas do Aedes aegypti
            dentro de tambores, baldes e caixas d'água, sem veneno.
            Escreva para um morador de periferia com abastecimento de água intermitente, em português do Brasil,
            com frases curtas, linguagem simples, sem termos técnicos e sem markdown complexo.
            Regras obrigatórias:
            - No máximo 6 itens curtos em lista com hífen, em ordem de prioridade.
            - Adapte as ações ao nível de risco informado.
            - Inclua um cuidado específico para bebês, gestantes e idosos.
            - Lembre de manter o aparelho Ressoa dentro do recipiente, com a bateria carregada (ele funciona a bateria, sem tomada), e o recipiente tampado.
            - Nunca faça diagnóstico nem indique remédio. Em sinais de alarme (dor abdominal forte, vômitos,
              sangramento, tontura) mande procurar a UBS ou pronto atendimento imediatamente.
            - Não invente números nem dados que não foram fornecidos.
            """;

    static final String SISTEMA_GESTOR = """
            Você é um assistente técnico de vigilância em saúde do Ressoa (plataforma de risco de dengue
            ligada ao El Niño e aparelhos de ultrassom contra larvas em recipientes de água).
            Escreva um plano de ação objetivo para o gestor municipal, em português do Brasil, em markdown simples.
            Estrutura obrigatória:
            1. Situação (1 a 2 frases com os indicadores fornecidos)
            2. Ações para as próximas 72 horas (até 5 itens)
            3. Ações para as próximas 4 semanas (até 4 itens)
            4. Público prioritário (bebês até 2 anos, gestantes e idosos acima de 65)
            5. Indicadores para acompanhar
            Baseie-se no protocolo do nível: VERDE = instalar Ressoa nos bairros com água intermitente;
            AMARELO = mais pulsos por dia, mutirão contra criadouros e alerta aos moradores;
            LARANJA = bloqueio de transmissão e salas de hidratação; VERMELHO = plano de emergência.
            Não invente dados que não foram fornecidos e não ultrapasse 250 palavras.
            """;

    static String contexto(Bairro bairro, AvaliacaoRisco avaliacao) {
        StringBuilder sb = new StringBuilder()
                .append("Bairro: ").append(bairro.getNome()).append(" — ").append(bairro.getMunicipio())
                .append("/").append(bairro.getUf()).append('\n')
                .append("Abastecimento de água intermitente: ").append(bairro.isAguaIntermitente() ? "sim" : "não")
                .append('\n')
                .append("Nível de risco atual: ").append(bairro.getNivelRisco()).append(" (")
                .append(bairro.getNivelRisco().getAcaoRecomendada()).append(")\n");
        if (avaliacao != null) {
            sb.append("Semana epidemiológica: ").append(avaliacao.getSemanaEpidemiologica()).append('\n');
            linha(sb, "Temperatura mínima média (°C)", avaliacao.getTempMinMedia());
            linha(sb, "Casos por 100 mil habitantes", avaliacao.getCasos100k());
            linha(sb, "Número de reprodução (Rt)", avaliacao.getRt());
        }
        return sb.toString();
    }

    private static void linha(StringBuilder sb, String rotulo, BigDecimal valor) {
        if (valor != null) {
            sb.append(rotulo).append(": ").append(valor.toPlainString()).append('\n');
        }
    }
}
