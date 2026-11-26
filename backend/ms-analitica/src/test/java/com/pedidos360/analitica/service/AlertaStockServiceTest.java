package com.pedidos360.analitica.service;

import com.pedidos360.analitica.config.AnaliticaKafkaProperties;
import com.pedidos360.analitica.dto.AlertaStockResponse;
import com.pedidos360.analitica.messaging.model.PedidoEventoMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({AlertaStockService.class, AlertaStockServiceTest.Config.class})
class AlertaStockServiceTest {

    @TestConfiguration
    static class Config {
        @Bean
        AnaliticaKafkaProperties props() {
            // Umbral de stock bajo = 5
            return new AnaliticaKafkaProperties(false, "pedidos", "auditoria", "g1", "g2", "g3", "g4", 5, 3, 3,
                    1000, 2.0, 8000);
        }
    }

    @Autowired
    private AlertaStockService service;

    private PedidoEventoMessage pedido(String eventId, long pedidoId, PedidoEventoMessage.Item... items) {
        return new PedidoEventoMessage(eventId, "CREADO", 1, pedidoId, 1L, "a@b.cl", "Ana", "PENDIENTE",
                new BigDecimal("1000"), 1, List.of(items), LocalDateTime.now());
    }

    private PedidoEventoMessage.Item item(long productoId, String nombre, Integer stockRestante) {
        return new PedidoEventoMessage.Item(productoId, nombre, 1, BigDecimal.TEN, stockRestante);
    }

    @Test
    void alertaCuandoElStockQuedaEnOBajoElUmbral() {
        int nuevas = service.evaluar(pedido("e1", 1, item(1, "Mouse", 5), item(2, "Teclado", 6)));

        assertThat(nuevas).isEqualTo(1);
        List<AlertaStockResponse> alertas = service.recientes(null);
        assertThat(alertas).hasSize(1);
        assertThat(alertas.get(0).productoId()).isEqualTo(1L);
        assertThat(alertas.get(0).nivel()).isEqualTo("STOCK_BAJO");
        assertThat(alertas.get(0).umbral()).isEqualTo(5);
    }

    @Test
    void nivelSinStockCuandoQuedaCero() {
        service.evaluar(pedido("e1", 1, item(1, "Mouse", 0)));

        assertThat(service.recientes(null).get(0).nivel()).isEqualTo("SIN_STOCK");
    }

    @Test
    void noAlertaConStockSuficienteSinDatoOEnEventosDeEstado() {
        assertThat(service.evaluar(pedido("e1", 1, item(1, "Mouse", 30)))).isZero();
        assertThat(service.evaluar(pedido("e2", 2, item(1, "Mouse", null)))).isZero();
        PedidoEventoMessage estado = new PedidoEventoMessage("e3", "ESTADO_ACTUALIZADO", 1, 3L, 1L, null, null,
                "ENVIADO", null, null, List.of(), LocalDateTime.now());
        assertThat(service.evaluar(estado)).isZero();
        assertThat(service.recientes(null)).isEmpty();
    }

    @Test
    void esIdempotenteAnteReentregas() {
        PedidoEventoMessage evento = pedido("e1", 1, item(1, "Mouse", 2));

        assertThat(service.evaluar(evento)).isEqualTo(1);
        assertThat(service.evaluar(evento)).isZero();
        assertThat(service.recientes(null)).hasSize(1);
    }

    @Test
    void productosCriticosMuestraLaUltimaAlertaDeCadaProductoDeMenorAMayorStock() {
        service.evaluar(pedido("e1", 1, item(1, "Mouse", 4), item(2, "Teclado", 1)));
        service.evaluar(pedido("e2", 2, item(1, "Mouse", 3)));

        List<AlertaStockResponse> criticos = service.productosCriticos();

        assertThat(criticos).extracting(AlertaStockResponse::productoId).containsExactly(2L, 1L);
        assertThat(criticos).extracting(AlertaStockResponse::stockRestante).containsExactly(1, 3);
    }
}
