package br.com.ressoa.mensageria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica {@code risco.atualizado} no RabbitMQ somente depois do commit da avaliação,
 * para nunca anunciar um nível que não foi gravado. Falha no broker não desfaz a avaliação.
 */
@Component
public class RiscoAtualizadoPublisher {

    private static final Logger log = LoggerFactory.getLogger(RiscoAtualizadoPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public RiscoAtualizadoPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(RiscoAtualizadoEvent evento) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY_RISCO, evento);
            log.info("Evento risco.atualizado publicado: bairro={} {} -> {}", evento.bairroId(),
                    evento.nivelAnterior(), evento.nivelNovo());
        } catch (AmqpException ex) {
            log.error("Falha ao publicar risco.atualizado (bairro={}): {}", evento.bairroId(), ex.getMessage());
        }
    }
}
