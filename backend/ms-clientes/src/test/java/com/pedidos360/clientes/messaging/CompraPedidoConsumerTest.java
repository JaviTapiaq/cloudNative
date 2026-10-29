package com.pedidos360.clientes.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pedidos360.clientes.messaging.consumer.compras.CompraPedidoConsumer;
import com.pedidos360.clientes.service.RegistroCompraService;
import com.rabbitmq.client.Channel;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CompraPedidoConsumerTest {

    private static final long TAG = 3L;

    private final Channel channel = mock(Channel.class);
    private final RegistroCompraService servicio = mock(RegistroCompraService.class);
    private final CompraPedidoConsumer consumer = new CompraPedidoConsumer(servicio,
            new ObjectMapper().registerModule(new JavaTimeModule()),
            Validation.buildDefaultValidatorFactory().getValidator());

    @Test
    void eventoValidoHaceAck() throws Exception {
        when(servicio.registrar(any(), any(), any(), any(), any())).thenReturn("ok");

        consumer.recibir(mensaje("{\"pedidoId\":5,\"clienteEmail\":\"ana@duocuc.cl\",\"clienteNombre\":\"Ana\","
                + "\"total\":1000,\"fecha\":\"2026-10-29T10:00:00\",\"campoExtra\":1}"), channel);

        verify(servicio).registrar(any(), any(), any(), any(BigDecimal.class), any());
        verify(channel).basicAck(TAG, false);
        verifyNoMoreInteractions(channel);
    }

    @Test
    void eventoSinCorreoVaALaDlq() throws Exception {
        consumer.recibir(mensaje("{\"pedidoId\":5,\"total\":1000}"), channel);

        verify(channel).basicReject(TAG, false);
        verifyNoInteractions(servicio);
    }

    @Test
    void jsonMalFormadoVaALaDlq() throws Exception {
        consumer.recibir(mensaje("{no-json"), channel);

        verify(channel).basicReject(TAG, false);
    }

    private static Message mensaje(String json) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(TAG);
        props.setMessageId("test");
        return new Message(json.getBytes(StandardCharsets.UTF_8), props);
    }
}
