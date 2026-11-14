package br.com.ressoa.bairro;

/**
 * Níveis de risco de dengue do Ressoa (contrato do briefing), em ordem crescente de gravidade.
 */
public enum NivelRisco {
    VERDE("Instalar o Ressoa nos bairros com água intermitente"),
    AMARELO("Mais pulsos por dia, mutirão contra criadouros e alerta aos moradores"),
    LARANJA("Bloqueio da transmissão nos primeiros casos e salas de hidratação"),
    VERMELHO("Plano de emergência, com prioridade a bebês, gestantes e idosos");

    private final String acaoRecomendada;

    NivelRisco(String acaoRecomendada) {
        this.acaoRecomendada = acaoRecomendada;
    }

    public String getAcaoRecomendada() {
        return acaoRecomendada;
    }

    public boolean maisGraveQue(NivelRisco outro) {
        return outro == null || this.ordinal() > outro.ordinal();
    }
}
