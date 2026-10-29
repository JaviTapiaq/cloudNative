package com.pedidos360.productos.messaging.consumer.stock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.productos.messaging.model.PedidoCreadoMessage;
import com.pedidos360.productos.service.StockInsuficienteException;
import com.pedidos360.productos.service.StockService;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Consume pedido.creado y descuenta el stock de los productos comprados.
 *
 * <ul>
 *   <li>OK                                   -> basicAck</li>
 *   <li>JSON/datos invalidos o sin stock      -> basicReject(requeue=false): va a la DLQ</li>
 *   <li>Otro error (ej. base de datos caida)  -> basicNack(requeue=true) una vez; si se repite, a la DLQ</li>
 * </ul>
 */
@Component
public class StockPedidoConsumer {

    private static final Logger log = LoggerFactory.getLogger(StockPedidoConsumer.class);

    private final StockService stockService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public StockPedidoConsumer(StockService stockService, ObjectMapper objectMapper, Validator validator) {
        this.stockService = stockService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @RabbitListener(id = "productos-stock", queues = "${productos.mensajeria.stock.queue}")
    public void recibir(Message message, Channel channel) throws IOException {
        MessageProperties props = message.getMessageProperties();
        long deliveryTag = props.getDeliveryTag();
        String messageId = props.getMessageId();

        try {
            PedidoCreadoMessage evento = leer(message);
            String resultado = stockService.descontar(evento.pedidoId(), agruparCantidades(evento));
            channel.basicAck(deliveryTag, false);
            log.info("ACK {}: {}", messageId, resultado);

        } catch (MensajeInvalidoException | StockInsuficienteException e) {
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

    private Map<Long, Integer> agruparCantidades(PedidoCreadoMessage evento) {
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        evento.items().forEach(i -> cantidades.merge(i.productoId(), i.cantidad(), Integer::sum));
        return cantidades;
    }
}
