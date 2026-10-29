package com.pedidos360.mensajeria.config;

import com.pedidos360.mensajeria.config.MensajeriaProperties.Destino;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declaracion de la topologia RabbitMQ (Exchange, Queue y Binding por cada caso de uso).
 * RabbitAdmin declara estos beans en el broker al establecer la conexion.
 *
 * <pre>
 *  ms-pedidos --pedido.*--> [eventos.exchange (topic)] --pedido.#--> eventos.queue
 *  ms-mensajeria ---------> [notificaciones.exchange]  --notificacion.enviar--> notificaciones.queue
 *  ms-mensajeria ---------> [tickets.exchange]         --ticket.generar------> tickets.queue
 *  ms-mensajeria ---------> [documentos.exchange]      --documento.generar---> documentos.queue
 *
 *  Toda cola principal tiene x-dead-letter-exchange = pedidos360.dlx y
 *  x-dead-letter-routing-key = nombre de su DLQ. Los mensajes rechazados
 *  (basicNack/basicReject sin requeue) terminan en la DLQ correspondiente.
 * </pre>
 */
@Configuration
public class RabbitTopologyConfig {

    private final MensajeriaProperties props;

    public RabbitTopologyConfig(MensajeriaProperties props) {
        this.props = props;
    }

    // ---------------------------------------------------------------- Dead Letter Exchange

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(props.deadLetter().exchange(), true, false);
    }

    // ---------------------------------------------------------------- Eventos de pedidos

    @Bean
    public TopicExchange eventosExchange() {
        return new TopicExchange(props.eventos().exchange(), true, false);
    }

    @Bean
    public Queue eventosQueue() {
        return colaConDlq(props.eventos());
    }

    @Bean
    public Queue eventosDlq() {
        return QueueBuilder.durable(props.eventos().dlq()).build();
    }

    @Bean
    public Binding eventosBinding() {
        return BindingBuilder.bind(eventosQueue()).to(eventosExchange()).with(props.eventos().routingKey());
    }

    @Bean
    public Binding eventosDlqBinding() {
        return dlqBinding(eventosDlq(), props.eventos());
    }

    // ---------------------------------------------------------------- Notificaciones

    @Bean
    public DirectExchange notificacionesExchange() {
        return new DirectExchange(props.notificaciones().exchange(), true, false);
    }

    @Bean
    public Queue notificacionesQueue() {
        return colaConDlq(props.notificaciones());
    }

    @Bean
    public Queue notificacionesDlq() {
        return QueueBuilder.durable(props.notificaciones().dlq()).build();
    }

    @Bean
    public Binding notificacionesBinding() {
        return BindingBuilder.bind(notificacionesQueue()).to(notificacionesExchange())
                .with(props.notificaciones().routingKey());
    }

    @Bean
    public Binding notificacionesDlqBinding() {
        return dlqBinding(notificacionesDlq(), props.notificaciones());
    }

    // ---------------------------------------------------------------- Tickets

    @Bean
    public DirectExchange ticketsExchange() {
        return new DirectExchange(props.tickets().exchange(), true, false);
    }

    @Bean
    public Queue ticketsQueue() {
        return colaConDlq(props.tickets());
    }

    @Bean
    public Queue ticketsDlq() {
        return QueueBuilder.durable(props.tickets().dlq()).build();
    }

    @Bean
    public Binding ticketsBinding() {
        return BindingBuilder.bind(ticketsQueue()).to(ticketsExchange()).with(props.tickets().routingKey());
    }

    @Bean
    public Binding ticketsDlqBinding() {
        return dlqBinding(ticketsDlq(), props.tickets());
    }

    // ---------------------------------------------------------------- Documentos

    @Bean
    public DirectExchange documentosExchange() {
        return new DirectExchange(props.documentos().exchange(), true, false);
    }

    @Bean
    public Queue documentosQueue() {
        return colaConDlq(props.documentos());
    }

    @Bean
    public Queue documentosDlq() {
        return QueueBuilder.durable(props.documentos().dlq()).build();
    }

    @Bean
    public Binding documentosBinding() {
        return BindingBuilder.bind(documentosQueue()).to(documentosExchange())
                .with(props.documentos().routingKey());
    }

    @Bean
    public Binding documentosDlqBinding() {
        return dlqBinding(documentosDlq(), props.documentos());
    }

    // ---------------------------------------------------------------- helpers

    private Queue colaConDlq(Destino destino) {
        return QueueBuilder.durable(destino.queue())
                .deadLetterExchange(props.deadLetter().exchange())
                .deadLetterRoutingKey(destino.dlq())
                .build();
    }

    private Binding dlqBinding(Queue dlq, Destino destino) {
        return BindingBuilder.bind(dlq).to(deadLetterExchange()).with(destino.dlq());
    }
}
