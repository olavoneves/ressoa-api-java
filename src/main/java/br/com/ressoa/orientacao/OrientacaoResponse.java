package br.com.ressoa.orientacao;

import java.time.Instant;

import br.com.ressoa.bairro.NivelRisco;
import io.swagger.v3.oas.annotations.media.Schema;

public record OrientacaoResponse(
        Long bairroId,
        String bairroNome,
        NivelRisco nivel,
        @Schema(description = "MORADOR (orientação simples) ou GESTOR (plano de ação)") Publico publico,
        String texto,
        @Schema(description = "false quando a IA estava indisponível e foi usado o protocolo padrão") boolean geradaPorIa,
        Instant geradaEm) {

    public enum Publico {
        MORADOR,
        GESTOR
    }
}
