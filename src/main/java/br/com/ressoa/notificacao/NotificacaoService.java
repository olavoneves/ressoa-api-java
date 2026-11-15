package br.com.ressoa.notificacao;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.ressoa.avaliacao.AvaliacaoRisco;
import br.com.ressoa.avaliacao.AvaliacaoRiscoRepository;
import br.com.ressoa.mensageria.RiscoAtualizadoEvent;
import br.com.ressoa.notificacao.dto.NotificacaoResponse;
import br.com.ressoa.shared.exception.RecursoNaoEncontradoException;
import br.com.ressoa.shared.web.PageResponse;
import br.com.ressoa.usuario.Papel;
import br.com.ressoa.usuario.Usuario;
import br.com.ressoa.usuario.UsuarioRepository;

@Service
public class NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);

    private final NotificacaoRepository notificacoes;
    private final UsuarioRepository usuarios;
    private final AvaliacaoRiscoRepository avaliacoes;

    public NotificacaoService(NotificacaoRepository notificacoes, UsuarioRepository usuarios,
            AvaliacaoRiscoRepository avaliacoes) {
        this.notificacoes = notificacoes;
        this.usuarios = usuarios;
        this.avaliacoes = avaliacoes;
    }

    /**
     * Cria um alerta para cada morador do bairro quando o risco sobe. Idempotente: reprocessar
     * o mesmo evento não duplica alertas (checagem + chave única USUARIO_ID/AVALIACAO_ID).
     *
     * @return quantidade de notificações criadas
     */
    @Transactional
    public int notificarMoradores(RiscoAtualizadoEvent evento) {
        if (!evento.riscoSubiu()) {
            log.info("Risco do bairro {} não subiu ({} -> {}): nenhum alerta", evento.bairroId(),
                    evento.nivelAnterior(), evento.nivelNovo());
            return 0;
        }
        AvaliacaoRisco avaliacao = avaliacoes.findById(evento.avaliacaoId()).orElse(null);
        if (avaliacao == null) {
            log.warn("Avaliação {} do evento não existe mais; evento descartado", evento.avaliacaoId());
            return 0;
        }
        String titulo = "Risco de dengue " + evento.nivelNovo() + " no " + evento.bairroNome();
        String mensagem = MensagensAlerta.para(evento.nivelNovo());
        List<Usuario> moradores = usuarios.findByBairroIdAndPapel(evento.bairroId(), Papel.CIDADAO);
        int criadas = 0;
        for (Usuario morador : moradores) {
            if (!notificacoes.existsByUsuarioIdAndAvaliacaoId(morador.getId(), avaliacao.getId())) {
                notificacoes.save(new Notificacao(morador, avaliacao, titulo, mensagem));
                criadas++;
            }
        }
        log.info("{} alerta(s) criado(s) para o bairro {}", criadas, evento.bairroId());
        return criadas;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificacaoResponse> listar(Long usuarioId, Boolean lida, Pageable pageable) {
        return PageResponse.of(notificacoes.buscar(usuarioId, lida, pageable), NotificacaoResponse::de);
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas(Long usuarioId) {
        return notificacoes.countByUsuarioIdAndLidaFalse(usuarioId);
    }

    @Transactional
    public NotificacaoResponse marcarComoLida(Long usuarioId, Long id) {
        Notificacao notificacao = notificacoes.findById(id)
                .filter(n -> n.getUsuario().getId().equals(usuarioId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notificação", id));
        notificacao.marcarComoLida();
        return NotificacaoResponse.de(notificacao);
    }
}
