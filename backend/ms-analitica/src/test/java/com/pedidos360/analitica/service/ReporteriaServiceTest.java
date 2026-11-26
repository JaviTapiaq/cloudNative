package com.pedidos360.analitica.service;

import com.pedidos360.analitica.dto.MetricasResponse;
import com.pedidos360.analitica.messaging.model.PedidoEventoMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(ReporteriaService.class)
class ReporteriaServiceTest {

    @Autowired
    private ReporteriaService service;

    private PedidoEventoMessage creado(String eventId, long pedidoId, String total, int unidades) {
        return new PedidoEventoMessage(eventId, "CREADO", 1, pedidoId, 1L, "ana@correo.cl", "Ana", "PENDIENTE",
                new BigDecimal(total), unidades,
                List.of(new PedidoEventoMessage.Item(10L, "Teclado", unidades, new BigDecimal("5000"), null)),
                LocalDateTime.now());
    }

    private PedidoEventoMessage estado(String eventId, long pedidoId, String estado) {
        return new PedidoEventoMessage(eventId, "ESTADO_ACTUALIZADO", 1, pedidoId, 1L, "ana@correo.cl", "Ana",
                estado, new BigDecimal("10000"), null, List.of(), LocalDateTime.now());
    }

    @Test
    void calculaMetricasDesdeLosEventos() {
        service.procesar(creado("e1", 1, "10000", 2));
        service.procesar(creado("e2", 2, "5000", 1));
        service.procesar(estado("e3", 1, "ENTREGADO"));

        MetricasResponse m = service.metricas();

        assertThat(m.totalPedidos()).isEqualTo(2);
        assertThat(m.ventasTotales()).isEqualByComparingTo("15000");
        assertThat(m.ticketPromedio()).isEqualByComparingTo("7500");
        assertThat(m.unidadesVendidas()).isEqualTo(3);
        assertThat(m.pedidosPorEstado()).containsEntry("ENTREGADO", 1L).containsEntry("PENDIENTE", 1L);
        assertThat(m.topProductos()).hasSize(1);
        assertThat(m.topProductos().get(0).unidadesVendidas()).isEqualTo(3);
    }

    @Test
    void esIdempotenteAnteReentregasDelMismoEvento() {
        assertThat(service.procesar(creado("e1", 1, "10000", 2))).isTrue();
        assertThat(service.procesar(creado("e1", 1, "10000", 2))).isFalse();

        MetricasResponse m = service.metricas();
        assertThat(m.totalPedidos()).isEqualTo(1);
        assertThat(m.unidadesVendidas()).isEqualTo(2);
    }

    @Test
    void losPedidosCanceladosNoCuentanEnLasVentas() {
        service.procesar(creado("e1", 1, "10000", 2));
        service.procesar(estado("e2", 1, "CANCELADO"));

        MetricasResponse m = service.metricas();
        assertThat(m.ventasTotales()).isEqualByComparingTo("0");
        assertThat(m.ticketPromedio()).isEqualByComparingTo("0");
        assertThat(m.pedidosPorEstado()).containsEntry("CANCELADO", 1L);
    }

    @Test
    void rechazaTiposDeEventoDesconocidos() {
        PedidoEventoMessage raro = new PedidoEventoMessage("e9", "OTRO", 1, 1L, null, null, null, "X", null, null,
                null, null);
        assertThatThrownBy(() -> service.procesar(raro)).isInstanceOf(IllegalArgumentException.class);
    }
}
