package com.pedidos360.analitica.messaging;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pedidos360.analitica.messaging.consumer.AuditoriaConsumer;
import com.pedidos360.analitica.messaging.consumer.MensajeInvalidoException;
import com.pedidos360.analitica.messaging.consumer.PayloadValidator;
import com.pedidos360.analitica.messaging.consumer.PedidoEventosConsumer;
import com.pedidos360.analitica.messaging.model.AuditoriaEventoMessage;
import com.pedidos360.analitica.messaging.model.PedidoEventoMessage;
import com.pedidos360.analitica.service.AuditoriaService;
import com.pedidos360.analitica.service.ReporteriaService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConsumersTest {

    private ReporteriaService reporteria;
    private AuditoriaService auditoria;
    private PedidoEventosConsumer pedidosConsumer;
    private AuditoriaConsumer auditoriaConsumer;

    @BeforeEach
    void setUp() {
        reporteria = mock(ReporteriaService.class);
        auditoria = mock(AuditoriaService.class);
        PayloadValidator validator = new PayloadValidator(
                Validation.buildDefaultValidatorFactory().getValidator());
        pedidosConsumer = new PedidoEventosConsumer(reporteria, validator);
        auditoriaConsumer = new AuditoriaConsumer(auditoria, validator);
    }

    @Test
    void eventoValidoSeDelegaAlServicioDeReporteria() {
        PedidoEventoMessage evento = new PedidoEventoMessage("e1", "CREADO", 1, 5L, 1L, "a@b.cl", "Ana",
                "PENDIENTE", new BigDecimal("100"), 1,
                List.of(new PedidoEventoMessage.Item(1L, "X", 1, BigDecimal.TEN, null)), LocalDateTime.now());
        when(reporteria.procesar(evento)).thenReturn(true);

        pedidosConsumer.recibir(evento, 0, 10L);

        verify(reporteria).procesar(evento);
    }

    @Test
    void eventoSinCamposObligatoriosSeRechazaSinTocarElServicio() {
        PedidoEventoMessage invalido = new PedidoEventoMessage(null, "CREADO", 1, null, null, null, null, null,
                null, null, null, null);

        assertThatThrownBy(() -> pedidosConsumer.recibir(invalido, 0, 1L))
                .isInstanceOf(MensajeInvalidoException.class)
                .hasMessageContaining("eventId")
                .hasMessageContaining("pedidoId");
        verifyNoInteractions(reporteria);
    }

    @Test
    void auditoriaValidaSeRegistraEInvalidaSeRechaza() {
        AuditoriaEventoMessage valido = new AuditoriaEventoMessage("a1", "PEDIDO_CREADO", "PEDIDO", "5",
                "a@b.cl", "detalle", "ms-pedidos", LocalDateTime.now());
        when(auditoria.registrar(any())).thenReturn(true);

        auditoriaConsumer.recibir(valido, 1, 3L);
        verify(auditoria).registrar(valido);

        AuditoriaEventoMessage invalido = new AuditoriaEventoMessage("a2", " ", "PEDIDO", "5", null, null, null,
                null);
        assertThatThrownBy(() -> auditoriaConsumer.recibir(invalido, 1, 4L))
                .isInstanceOf(MensajeInvalidoException.class);
    }

    @Test
    void elContratoJsonDeMsPedidosSeDeserializaAlDto() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String json = """
                {"eventId":"abc","tipoEvento":"CREADO","version":1,"pedidoId":7,"clienteId":3,
                 "clienteEmail":"ana@correo.cl","clienteNombre":"Ana","estado":"PENDIENTE","total":15000,
                 "cantidadItems":2,"campoNuevo":"se ignora",
                 "items":[{"productoId":10,"nombreProducto":"Teclado","cantidad":2,"precioUnitario":7500}],
                 "fechaEvento":"2025-01-01T10:00:00"}
                """;

        PedidoEventoMessage evento = mapper.readValue(json, PedidoEventoMessage.class);

        assertThat(evento.pedidoId()).isEqualTo(7L);
        assertThat(evento.esCreado()).isTrue();
        assertThat(evento.items()).hasSize(1);
        assertThat(evento.fechaEvento()).isEqualTo(LocalDateTime.of(2025, 1, 1, 10, 0));
    }
}
