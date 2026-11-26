package com.pedidos360.pedidos.messaging.kafka;

import com.pedidos360.pedidos.event.PedidoEvent;
import com.pedidos360.pedidos.messaging.kafka.config.PedidosKafkaProperties;
import com.pedidos360.pedidos.messaging.kafka.dto.AuditoriaEventoKafka;
import com.pedidos360.pedidos.messaging.kafka.dto.PedidoEventoKafka;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class PedidoEventKafkaPublisherTest {

    private static final String TOPIC_PEDIDOS = "pedidos360.pedidos.eventos";
    private static final String TOPIC_AUDITORIA = "pedidos360.auditoria.eventos";

    private KafkaTemplate<String, Object> kafkaTemplate;
    private PedidoEventKafkaPublisher publisher;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        publisher = new PedidoEventKafkaPublisher(kafkaTemplate,
                new PedidosKafkaProperties(true, TOPIC_PEDIDOS, TOPIC_AUDITORIA, "ms-pedidos"));
    }

    private PedidoEvent eventoCreado() {
        return new PedidoEvent(PedidoEvent.Tipo.CREADO, 7L, 3L, "ana@correo.cl", "Ana", "PENDIENTE",
                new BigDecimal("15000"), 2,
                List.of(new PedidoEvent.Item(10L, "Teclado", 2, new BigDecimal("7500"), 3)),
                LocalDateTime.of(2025, 1, 1, 10, 0));
    }

    @Test
    void publicaEventoDeNegocioYDeAuditoriaConKeyPedidoId() {
        when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenReturn(new CompletableFuture<>());

        publisher.onPedidoEvent(eventoCreado());

        ArgumentCaptor<Object> negocio = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(TOPIC_PEDIDOS), eq("7"), negocio.capture());
        PedidoEventoKafka evento = (PedidoEventoKafka) negocio.getValue();
        assertThat(evento.tipoEvento()).isEqualTo("CREADO");
        assertThat(evento.eventId()).isNotBlank();
        assertThat(evento.items()).hasSize(1);
        assertThat(evento.items().get(0).stockRestante()).isEqualTo(3);

        ArgumentCaptor<Object> auditoria = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(TOPIC_AUDITORIA), eq("7"), auditoria.capture());
        AuditoriaEventoKafka registro = (AuditoriaEventoKafka) auditoria.getValue();
        assertThat(registro.accion()).isEqualTo("PEDIDO_CREADO");
        assertThat(registro.actor()).isEqualTo("ana@correo.cl");
        assertThat(registro.origen()).isEqualTo("ms-pedidos");
    }

    @Test
    void unFalloDeKafkaNoSePropagaAlFlujoDelPedido() {
        when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenThrow(new TimeoutException("broker caido"));

        assertThatCode(() -> publisher.onPedidoEvent(eventoCreado())).doesNotThrowAnyException();
    }

    @Test
    void unFalloAsincronoTampocoSePropaga() {
        when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenReturn(CompletableFuture.failedFuture(new TimeoutException("sin respuesta")));

        assertThatCode(() -> publisher.onPedidoEvent(eventoCreado())).doesNotThrowAnyException();
    }
}
