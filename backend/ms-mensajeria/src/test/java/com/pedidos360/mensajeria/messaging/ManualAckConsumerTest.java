package com.pedidos360.mensajeria.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.mensajeria.messaging.consumer.support.ConsumerSupport;
import com.pedidos360.mensajeria.messaging.consumer.tickets.TicketConsumer;
import com.pedidos360.mensajeria.service.registro.RegistroProcesamiento.EstadoProcesamiento;
import com.pedidos360.mensajeria.service.registro.RegistroProcesamientoService;
import com.pedidos360.mensajeria.service.tickets.TicketService;
import com.rabbitmq.client.Channel;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class ManualAckConsumerTest {

    private static final long TAG = 42L;

    private final Channel channel = mock(Channel.class);
    private RegistroProcesamientoService registro;
    private TicketConsumer consumer;

    @BeforeEach
    void setUp() {
        registro = new RegistroProcesamientoService();
        ConsumerSupport support = new ConsumerSupport(new ObjectMapper(),
                Validation.buildDefaultValidatorFactory().getValidator(), registro);
        consumer = new TicketConsumer(support, new TicketService());
    }

    @Test
    void mensajeValidoHaceAck() throws Exception {
        consumer.recibir(mensaje("{\"pedidoId\":1,\"cliente\":\"ana@duocuc.cl\",\"total\":1500}", false), channel);

        verify(channel).basicAck(TAG, false);
        verifyNoMoreInteractions(channel);
        assertThat(registro.listar().get(0).estado()).isEqualTo(EstadoProcesamiento.PROCESADO);
    }

    @Test
    void mensajeInvalidoSeRechazaSinReencolar() throws Exception {
        consumer.recibir(mensaje("{\"pedidoId\":1}", false), channel);

        verify(channel).basicReject(TAG, false);
        verifyNoMoreInteractions(channel);
        assertThat(registro.listar().get(0).estado()).isEqualTo(EstadoProcesamiento.ENVIADO_A_DLQ);
    }

    @Test
    void jsonMalFormadoVaALaDlq() throws Exception {
        consumer.recibir(mensaje("no-es-json", false), channel);

        verify(channel).basicReject(TAG, false);
    }

    @Test
    void errorTransitorioSeReencolaLaPrimeraVez() throws Exception {
        consumer.recibir(mensaje(transitorio(), false), channel);

        verify(channel).basicNack(TAG, false, true);
        assertThat(registro.listar().get(0).estado()).isEqualTo(EstadoProcesamiento.REENCOLADO);
    }

    @Test
    void errorTransitorioReentregadoVaALaDlq() throws Exception {
        consumer.recibir(mensaje(transitorio(), true), channel);

        verify(channel).basicNack(TAG, false, false);
        assertThat(registro.listar().get(0).estado()).isEqualTo(EstadoProcesamiento.ENVIADO_A_DLQ);
    }

    private static String transitorio() {
        return "{\"pedidoId\":1,\"cliente\":\"ana@duocuc.cl\",\"total\":10,\"simularFallo\":\"TRANSITORIO\"}";
    }

    private static Message mensaje(String json, boolean redelivered) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(TAG);
        props.setRedelivered(redelivered);
        props.setMessageId("test-id");
        props.setConsumerQueue("pedidos360.tickets.queue");
        return new Message(json.getBytes(StandardCharsets.UTF_8), props);
    }
}
