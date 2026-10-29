package com.pedidos360.mensajeria.messaging.producer;

import com.pedidos360.mensajeria.config.MensajeriaProperties;
import com.pedidos360.mensajeria.config.MensajeriaProperties.Destino;
import com.pedidos360.mensajeria.messaging.model.DocumentoMessage;
import com.pedidos360.mensajeria.messaging.model.NotificacionMessage;
import com.pedidos360.mensajeria.messaging.model.TicketMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Productor de tareas asincronas. Encapsula exchange y routing key de cada caso de uso,
 * de modo que quien publica solo entrega el mensaje de negocio.
 */
@Component
public class MensajePublisher {

    private static final Logger log = LoggerFactory.getLogger(MensajePublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final MensajeriaProperties props;

    public MensajePublisher(RabbitTemplate rabbitTemplate, MensajeriaProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    public String publicarNotificacion(NotificacionMessage mensaje) {
        return publicar(props.notificaciones(), mensaje);
    }

    public String publicarTicket(TicketMessage mensaje) {
        return publicar(props.tickets(), mensaje);
    }

    public String publicarDocumento(DocumentoMessage mensaje) {
        return publicar(props.documentos(), mensaje);
    }

    private String publicar(Destino destino, Object payload) {
        String messageId = UUID.randomUUID().toString();
        rabbitTemplate.convertAndSend(destino.exchange(), destino.routingKey(), payload, message -> {
            message.getMessageProperties().setMessageId(messageId);
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return message;
        }, new CorrelationData(messageId));
        log.debug("Publicado {} en exchange={} routingKey={}", messageId, destino.exchange(), destino.routingKey());
        return messageId;
    }
}
