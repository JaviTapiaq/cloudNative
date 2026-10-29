package com.pedidos360.clientes.messaging.consumer.compras;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.clientes.messaging.model.PedidoCreadoMessage;
import com.pedidos360.clientes.service.RegistroCompraService;
import com.rabbitmq.client.Channel;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Consume pedido.creado y registra la compra en el historial del cliente.
 *
 * <ul>
 *   <li>OK                                    -> basicAck</li>
 *   <li>JSON invalido o sin correo de cliente -> basicReject(requeue=false): va a la DLQ</li>
 *   <li>Otro error (ej. base de datos caida)   -> basicNack(requeue=true) una vez; si se repite, a la DLQ</li>
 * </ul>
 */
@Component
public class CompraPedidoConsumer {

    private static final Logger log = LoggerFactory.getLogger(CompraPedidoConsumer.class);

    private final RegistroCompraService registroCompraService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public CompraPedidoConsumer(RegistroCompraService registroCompraService, ObjectMapper objectMapper,
                                Validator validator) {
        this.registroCompraService = registroCompraService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @RabbitListener(id = "clientes-compras", queues = "${clientes.mensajeria.compras.queue}")
    public void recibir(Message message, Channel channel) throws IOException {
        MessageProperties props = message.getMessageProperties();
        long deliveryTag = props.getDeliveryTag();
        String messageId = props.getMessageId();

        try {
            PedidoCreadoMessage evento = leer(message);
            String resultado = registroCompraService.registrar(evento.pedidoId(), evento.clienteEmail(),
                    evento.clienteNombre(), evento.total(), evento.fecha());
            channel.basicAck(deliveryTag, false);
            log.info("ACK {}: {}", messageId, resultado);

        } catch (MensajeInvalidoException e) {
            channel.basicReject(deliveryTag, false);
            log.warn("REJECT {} (error permanente, enviado a DLQ): {}", messageId, e.getMessage());

        } catch (Exception e) {
            boolean yaReintentado = Boolean.TRUE.equals(props.getRedelivered());
            channel.basicNack(deliveryTag, false, !yaReintentado);
            if (yaReintentado) {
                log.error("NACK {} (fallo tras reintento, enviado a DLQ): {}", messageId, e.getMessage());
            } else {
                log.warn("NACK {} (reencolado para reintento): {}", messageId, e.getMessage());
            }
        }
    }

    private PedidoCreadoMessage leer(Message message) {
        PedidoCreadoMessage evento;
        try {
            evento = objectMapper.readValue(message.getBody(), PedidoCreadoMessage.class);
        } catch (IOException e) {
            throw new MensajeInvalidoException("JSON invalido: " + e.getMessage());
        }
        Set<ConstraintViolation<PedidoCreadoMessage>> violaciones = validator.validate(evento);
        if (!violaciones.isEmpty()) {
            throw new MensajeInvalidoException("Evento invalido: " + violaciones.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", ")));
        }
        return evento;
    }
}
