package br.com.ressoa.notificacao;

import br.com.ressoa.bairro.NivelRisco;

/** Textos curtos e acionáveis dos alertas push/in-app, por nível de risco. */
final class MensagensAlerta {

    private MensagensAlerta() {
    }

    static String para(NivelRisco nivel) {
        return switch (nivel) {
            case VERDE -> "Mantenha seu aparelho Ressoa ligado e a tampa dos recipientes bem fechada.";
            case AMARELO -> "O calor e a água guardada favorecem o mosquito. Confira se seu Ressoa está ativo, "
                    + "tampe tambores e caixas e elimine água parada. Veja a orientação completa no app.";
            case LARANJA -> "Há transmissão de dengue no seu bairro. Use repelente, proteja bebês, gestantes e "
                    + "idosos e procure a UBS se tiver febre. Veja a orientação completa no app.";
            case VERMELHO -> "Epidemia de dengue no seu bairro. Em caso de febre, dor abdominal, vômitos ou "
                    + "sangramento, procure atendimento imediatamente e beba bastante água.";
        };
    }
}
