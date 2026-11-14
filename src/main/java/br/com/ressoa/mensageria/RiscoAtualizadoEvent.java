package br.com.ressoa.mensageria;

import java.time.Instant;

import br.com.ressoa.bairro.NivelRisco;

/**
 * Evento {@code risco.atualizado}: o nível de risco de um bairro mudou.
 * É publicado no RabbitMQ (exchange {@code ressoa.eventos}) após o commit da avaliação.
 */
public record RiscoAtualizadoEvent(
        Long avaliacaoId,
        Long bairroId,
        String bairroNome,
        NivelRisco nivelAnterior,
        NivelRisco nivelNovo,
        Integer semanaEpidemiologica,
        Instant ocorridoEm) {

    public boolean riscoSubiu() {
        return nivelNovo.maisGraveQue(nivelAnterior);
    }
}
