package com.pedidos360.productos.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.productos.messaging.consumer.stock.StockPedidoConsumer;
import com.pedidos360.productos.service.StockInsuficienteException;
import com.pedidos360.productos.service.StockService;
import com.rabbitmq.client.Channel;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class StockPedidoConsumerTest {

    private static final long TAG = 7L;
    private static final String EVENTO = "{\"pedidoId\":10,\"items\":[{\"productoId\":1,\"cantidad\":2},{\"productoId\":1,\"cantidad\":1}]}";

    private final Channel channel = mock(Channel.class);
    private final StockService stockService = mock(StockService.class);
    private final StockPedidoConsumer consumer = new StockPedidoConsumer(stockService, new ObjectMapper(),
            Validation.buildDefaultValidatorFactory().getValidator());

    @Test
    void descuentaStockAgrupadoYHaceAck() throws Exception {
        when(stockService.descontar(eq(10L), any())).thenReturn("ok");

        consumer.recibir(mensaje(EVENTO, false), channel);

        verify(stockService).descontar(10L, Map.of(1L, 3));
        verify(channel).basicAck(TAG, false);
        verifyNoMoreInteractions(channel);
    }

    @Test
    void sinStockSeRechazaHaciaLaDlq() throws Exception {
        when(stockService.descontar(eq(10L), any())).thenThrow(new StockInsuficienteException("sin stock"));

        consumer.recibir(mensaje(EVENTO, false), channel);

        verify(channel).basicReject(TAG, false);
    }

    @Test
    void eventoSinItemsEsInvalido() throws Exception {
        consumer.recibir(mensaje("{\"pedidoId\":10,\"items\":[]}", false), channel);

        verify(channel).basicReject(TAG, false);
        verifyNoInteractions(stockService);
    }

    @Test
    void errorTransitorioSeReintentaUnaVezYLuegoVaALaDlq() throws Exception {
        when(stockService.descontar(eq(10L), any())).thenThrow(new IllegalStateException("BD no disponible"));

        consumer.recibir(mensaje(EVENTO, false), channel);
        verify(channel).basicNack(TAG, false, true);

        consumer.recibir(mensaje(EVENTO, true), channel);
        verify(channel).basicNack(TAG, false, false);
    }

    private static Message mensaje(String json, boolean redelivered) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(TAG);
        props.setRedelivered(redelivered);
        props.setMessageId("test");
        return new Message(json.getBytes(StandardCharsets.UTF_8), props);
    }
}
