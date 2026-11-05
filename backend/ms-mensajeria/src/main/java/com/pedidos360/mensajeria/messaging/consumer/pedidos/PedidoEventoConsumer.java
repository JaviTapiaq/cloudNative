package com.pedidos360.mensajeria.messaging.consumer.pedidos;

import com.pedidos360.mensajeria.messaging.consumer.support.AbstractManualAckConsumer;
import com.pedidos360.mensajeria.messaging.consumer.support.ConsumerSupport;
import com.pedidos360.mensajeria.messaging.consumer.support.MensajeInvalidoException;
import com.pedidos360.mensajeria.messaging.model.PedidoEventoMessage;
import com.pedidos360.mensajeria.service.pedidos.PedidoEventoService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consume los eventos de dominio publicados por ms-pedidos y los enruta segun la routing key.
 */
@Component
public class PedidoEventoConsumer extends AbstractManualAckConsumer<PedidoEventoMessage> {

    static final String PEDIDO_CREADO = "pedido.creado";
    static final String PEDIDO_ESTADO_ACTUALIZADO = "pedido.estado.actualizado";

    private final PedidoEventoService pedidoEventoService;

    public PedidoEventoConsumer(ConsumerSupport support, PedidoEventoService pedidoEventoService) {
        super(support, PedidoEventoMessage.class);
        this.pedidoEventoService = pedidoEventoService;
    }

    @RabbitListener(id = "pedidos-eventos", queues = "${mensajeria.rabbit.eventos.queue}")
    public void recibir(Message message, Channel channel) throws IOException {
        consumir(message, channel);
    }

    @Override
    protected String procesar(PedidoEventoMessage evento, MessageProperties properties) {
        String routingKey = String.valueOf(properties.getReceivedRoutingKey());
        return switch (routingKey) {
            case PEDIDO_CREADO -> pedidoEventoService.onPedidoCreado(evento);
            case PEDIDO_ESTADO_ACTUALIZADO -> pedidoEventoService.onEstadoActualizado(evento);
            default -> throw new MensajeInvalidoException("Evento no soportado: " + routingKey);
        };
    }
}
