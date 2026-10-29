package com.pedidos360.mensajeria.messaging.consumer.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.pedidos360.mensajeria.messaging.model.MensajeSimulable;
import com.pedidos360.mensajeria.service.registro.RegistroProcesamiento.EstadoProcesamiento;
import com.rabbitmq.client.Channel;
import jakarta.validation.ConstraintViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Plantilla de consumo con confirmacion manual (AcknowledgeMode.MANUAL).
 *
 * <ul>
 *   <li>Procesado OK                    -> basicAck: el broker elimina el mensaje.</li>
 *   <li>{@link MensajeInvalidoException} -> basicReject(requeue=false): va a la DLQ.</li>
 *   <li>Otro error, primera entrega      -> basicNack(requeue=true): se reintenta una vez.</li>
 *   <li>Otro error, ya reentregado       -> basicNack(requeue=false): va a la DLQ.</li>
 * </ul>
 *
 * Las subclases solo implementan {@link #procesar}, que delega en un servicio de negocio.
 */
public abstract class AbstractManualAckConsumer<T> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final ConsumerSupport support;
    private final Class<T> tipo;

    protected AbstractManualAckConsumer(ConsumerSupport support, Class<T> tipo) {
        this.support = support;
        this.tipo = tipo;
    }

    /**
     * Ejecuta la logica de negocio y devuelve un resumen para la bitacora.
     */
    protected abstract String procesar(T payload, MessageProperties properties);

    protected final void consumir(Message message, Channel channel) throws IOException {
        MessageProperties props = message.getMessageProperties();
        long deliveryTag = props.getDeliveryTag();
        String cola = props.getConsumerQueue();
        String messageId = props.getMessageId() != null ? props.getMessageId() : "sin-id";

        try {
            T payload = convertir(message);
            validar(payload);
            aplicarFalloSimulado(payload);

            String resultado = procesar(payload, props);

            channel.basicAck(deliveryTag, false);
            log.info("ACK {} en {}: {}", messageId, cola, resultado);
            support.registro().registrar(messageId, cola, EstadoProcesamiento.PROCESADO, resultado);

        } catch (MensajeInvalidoException e) {
            channel.basicReject(deliveryTag, false);
            log.warn("REJECT {} en {} (error permanente, enviado a DLQ): {}", messageId, cola, e.getMessage());
            support.registro().registrar(messageId, cola, EstadoProcesamiento.ENVIADO_A_DLQ, e.getMessage());

        } catch (Exception e) {
            boolean yaReintentado = Boolean.TRUE.equals(props.getRedelivered());
            channel.basicNack(deliveryTag, false, !yaReintentado);
            if (yaReintentado) {
                log.error("NACK {} en {} (fallo tras reintento, enviado a DLQ): {}", messageId, cola, e.getMessage());
                support.registro().registrar(messageId, cola, EstadoProcesamiento.ENVIADO_A_DLQ, e.getMessage());
            } else {
                log.warn("NACK {} en {} (reencolado para reintento): {}", messageId, cola, e.getMessage());
                support.registro().registrar(messageId, cola, EstadoProcesamiento.REENCOLADO, e.getMessage());
            }
        }
    }

    private T convertir(Message message) {
        try {
            return support.objectMapper().readValue(message.getBody(), tipo);
        } catch (JsonProcessingException e) {
            throw new MensajeInvalidoException("JSON invalido para " + tipo.getSimpleName() + ": "
                    + e.getOriginalMessage(), e);
        } catch (IOException e) {
            throw new MensajeInvalidoException("No se pudo leer el mensaje: " + e.getMessage(), e);
        }
    }

    private void validar(T payload) {
        if (payload == null) {
            throw new MensajeInvalidoException("Mensaje vacio");
        }
        Set<ConstraintViolation<T>> violaciones = support.validator().validate(payload);
        if (!violaciones.isEmpty()) {
            String detalle = violaciones.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new MensajeInvalidoException("Mensaje invalido: " + detalle);
        }
    }

    private void aplicarFalloSimulado(T payload) {
        if (!(payload instanceof MensajeSimulable simulable) || simulable.simularFallo() == null) {
            return;
        }
        switch (simulable.simularFallo().toUpperCase()) {
            case "PERMANENTE" -> throw new MensajeInvalidoException("Fallo PERMANENTE simulado");
            case "TRANSITORIO" -> throw new ErrorTransitorioException("Fallo TRANSITORIO simulado");
            default -> { }
        }
    }
}
