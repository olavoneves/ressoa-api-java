package br.com.ressoa.notificacao;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import br.com.ressoa.mensageria.RabbitConfig;
import br.com.ressoa.mensageria.RiscoAtualizadoEvent;

/** Consumidor assíncrono de {@code risco.atualizado}: transforma o evento em alertas aos moradores. */
@Component
public class RiscoAtualizadoListener {

    private final NotificacaoService notificacaoService;

    public RiscoAtualizadoListener(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @RabbitListener(queues = RabbitConfig.FILA_NOTIFICACOES)
    public void aoAtualizarRisco(RiscoAtualizadoEvent evento) {
        notificacaoService.notificarMoradores(evento);
    }
}
