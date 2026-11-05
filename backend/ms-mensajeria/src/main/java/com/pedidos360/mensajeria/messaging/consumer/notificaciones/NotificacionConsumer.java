package com.pedidos360.mensajeria.messaging.consumer.notificaciones;

import com.pedidos360.mensajeria.messaging.consumer.support.AbstractManualAckConsumer;
import com.pedidos360.mensajeria.messaging.consumer.support.ConsumerSupport;
import com.pedidos360.mensajeria.messaging.model.NotificacionMessage;
import com.pedidos360.mensajeria.service.notificaciones.NotificacionService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class NotificacionConsumer extends AbstractManualAckConsumer<NotificacionMessage> {

    private final NotificacionService notificacionService;

    public NotificacionConsumer(ConsumerSupport support, NotificacionService notificacionService) {
        super(support, NotificacionMessage.class);
        this.notificacionService = notificacionService;
    }

    @RabbitListener(id = "notificaciones", queues = "${mensajeria.rabbit.notificaciones.queue}")
    public void recibir(Message message, Channel channel) throws IOException {
        consumir(message, channel);
    }

    @Override
    protected String procesar(NotificacionMessage payload, MessageProperties properties) {
        return notificacionService.enviar(payload);
    }
}
