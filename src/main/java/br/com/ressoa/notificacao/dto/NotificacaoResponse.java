package br.com.ressoa.notificacao.dto;

import java.time.Instant;

import br.com.ressoa.bairro.NivelRisco;
import br.com.ressoa.notificacao.Notificacao;

public record NotificacaoResponse(Long id, String titulo, String mensagem, boolean lida, Instant criadaEm,
        Long avaliacaoId, NivelRisco nivel, Long bairroId, String bairroNome) {

    public static NotificacaoResponse de(Notificacao n) {
        return new NotificacaoResponse(n.getId(), n.getTitulo(), n.getMensagem(), n.isLida(), n.getCriadaEm(),
                n.getAvaliacao().getId(), n.getAvaliacao().getNivel(), n.getAvaliacao().getBairro().getId(),
                n.getAvaliacao().getBairro().getNome());
    }
}
