package br.com.ressoa.mensageria;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia RabbitMQ (requisito 5, opção A):
 * exchange topic {@code ressoa.eventos} → routing key {@code risco.atualizado} → fila
 * {@code ressoa.risco.notificacoes}. Mensagens que falham após as tentativas vão para a DLQ.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "ressoa.eventos";
    public static final String ROUTING_KEY_RISCO = "risco.atualizado";
    public static final String FILA_NOTIFICACOES = "ressoa.risco.notificacoes";
    public static final String DLX = "ressoa.eventos.dlx";
    public static final String DLQ = FILA_NOTIFICACOES + ".dlq";

    @Bean
    TopicExchange ressoaEventosExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange ressoaDeadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    Queue filaNotificacoesRisco() {
        return QueueBuilder.durable(FILA_NOTIFICACOES)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(DLQ)
                .build();
    }

    @Bean
    Queue filaNotificacoesRiscoDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    Binding bindingRisco(Queue filaNotificacoesRisco, TopicExchange ressoaEventosExchange) {
        return BindingBuilder.bind(filaNotificacoesRisco).to(ressoaEventosExchange).with(ROUTING_KEY_RISCO);
    }

    @Bean
    Binding bindingDlq(Queue filaNotificacoesRiscoDlq, DirectExchange ressoaDeadLetterExchange) {
        return BindingBuilder.bind(filaNotificacoesRiscoDlq).to(ressoaDeadLetterExchange).with(DLQ);
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
