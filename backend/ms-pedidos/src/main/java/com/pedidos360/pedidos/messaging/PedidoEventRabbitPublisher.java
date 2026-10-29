package com.pedidos360.pedidos.messaging;

import com.pedidos360.pedidos.event.PedidoEvent;
import com.pedidos360.pedidos.messaging.config.PedidosMensajeriaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Traduce los eventos de dominio de pedidos a mensajes RabbitMQ.
 *
 * Un fallo del broker se registra pero NO se propaga: la creacion o actualizacion del pedido
 * nunca falla por la mensajeria (las tareas asincronas son secundarias al flujo principal).
 */
@Component
@ConditionalOnProperty(prefix = "pedidos.mensajeria", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PedidoEventRabbitPublisher {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventRabbitPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final PedidosMensajeriaProperties props;

    public PedidoEventRabbitPublisher(RabbitTemplate rabbitTemplate, PedidosMensajeriaProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    @EventListener
    public void onPedidoEvent(PedidoEvent evento) {
        String routingKey = evento.tipo() == PedidoEvent.Tipo.CREADO
                ? props.routingKeyCreado()
                : props.routingKeyEstado();
        try {
            rabbitTemplate.convertAndSend(props.exchange(), routingKey, PedidoEventoMessage.desde(evento), m -> {
                m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                return m;
            });
            log.info("Evento {} publicado para pedido {} (routingKey={})", evento.tipo(), evento.pedidoId(), routingKey);
        } catch (AmqpException e) {
            log.error("No se pudo publicar el evento {} del pedido {}: {}. El pedido se proceso igualmente.",
                    evento.tipo(), evento.pedidoId(), e.getMessage());
        }
    }
}
