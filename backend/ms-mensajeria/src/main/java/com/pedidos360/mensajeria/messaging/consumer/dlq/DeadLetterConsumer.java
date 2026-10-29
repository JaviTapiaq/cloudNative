package com.pedidos360.mensajeria.messaging.consumer.dlq;

import com.pedidos360.mensajeria.service.registro.RegistroProcesamiento.EstadoProcesamiento;
import com.pedidos360.mensajeria.service.registro.RegistroProcesamientoService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Consumidor de las DLQs: registra el motivo del fallo (cabecera x-death) para auditoria.
 * Por defecto NO arranca (mensajeria.rabbit.dead-letter.consumer-auto-startup=false) para
 * que los mensajes fallidos queden visibles en la DLQ; puede activarse por configuracion.
 */
@Component
public class DeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterConsumer.class);

    private final RegistroProcesamientoService registro;

    public DeadLetterConsumer(RegistroProcesamientoService registro) {
        this.registro = registro;
    }

    @RabbitListener(
            id = "dead-letters",
            autoStartup = "${mensajeria.rabbit.dead-letter.consumer-auto-startup:false}",
            queues = {
                    "${mensajeria.rabbit.eventos.dlq}",
                    "${mensajeria.rabbit.notificaciones.dlq}",
                    "${mensajeria.rabbit.tickets.dlq}",
                    "${mensajeria.rabbit.documentos.dlq}"
            })
    public void recibir(Message message, Channel channel) throws IOException {
        MessageProperties props = message.getMessageProperties();
        long deliveryTag = props.getDeliveryTag();
        try {
            String detalle = describirMuerte(props.getXDeathHeader());
            log.warn("Mensaje {} recibido en {}: {}", props.getMessageId(), props.getConsumerQueue(), detalle);
            registro.registrar(props.getMessageId(), props.getConsumerQueue(),
                    EstadoProcesamiento.RECIBIDO_EN_DLQ, detalle);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            // Las DLQ no tienen otra DLQ detras: se reencola una sola vez para no perder el mensaje
            boolean reintentar = !Boolean.TRUE.equals(props.getRedelivered());
            log.error("Error procesando mensaje de DLQ {} (requeue={}): {}", props.getMessageId(), reintentar, e.getMessage());
            channel.basicNack(deliveryTag, false, reintentar);
        }
    }

    private String describirMuerte(List<Map<String, ?>> xDeath) {
        if (xDeath == null || xDeath.isEmpty()) {
            return "sin cabecera x-death";
        }
        Map<String, ?> ultima = xDeath.get(0);
        return "cola origen=" + ultima.get("queue") + ", motivo=" + ultima.get("reason")
                + ", veces=" + ultima.get("count");
    }
}
