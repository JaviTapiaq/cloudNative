package com.pedidos360.analitica.service;

import com.pedidos360.analitica.config.AnaliticaKafkaProperties;
import com.pedidos360.analitica.dto.AlertaStockResponse;
import com.pedidos360.analitica.messaging.model.PedidoEventoMessage;
import com.pedidos360.analitica.model.AlertaStock;
import com.pedidos360.analitica.repository.AlertaStockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Logica de alertas de stock bajo: revisa los pedidos creados y registra una alerta por cada producto que
 * queda en o bajo el umbral. No conoce Kafka.
 *
 * El stock viene en el propio evento (stockRestante), calculado por ms-pedidos al cotizar con el stock que
 * informa ms-productos. Es una fotografia al momento del pedido: aviso oportuno, no inventario exacto.
 */
@Service
public class AlertaStockService {

    private static final Logger log = LoggerFactory.getLogger(AlertaStockService.class);
    private static final int LIMITE_POR_DEFECTO = 50;
    private static final int LIMITE_MAXIMO = 500;

    private final AlertaStockRepository repository;
    private final int umbral;

    public AlertaStockService(AlertaStockRepository repository, AnaliticaKafkaProperties props) {
        this.repository = repository;
        this.umbral = props.umbralStockBajo();
    }

    /**
     * Evalua un evento de pedido. Solo los pedidos CREADOS traen items.
     * Es idempotente por (eventId, productoId).
     *
     * @return cantidad de alertas nuevas registradas
     */
    @Transactional
    public int evaluar(PedidoEventoMessage evento) {
        if (!evento.esCreado() || evento.items() == null) {
            return 0;
        }
        int nuevas = 0;
        for (PedidoEventoMessage.Item item : evento.items()) {
            if (item.stockRestante() == null || item.stockRestante() > umbral
                    || repository.existsByEventIdAndProductoId(evento.eventId(), item.productoId())) {
                continue;
            }
            AlertaStock alerta = repository.save(new AlertaStock(evento.eventId(), item.productoId(),
                    item.nombreProducto(), item.stockRestante(), umbral, evento.pedidoId(),
                    evento.fechaEvento() != null ? evento.fechaEvento() : LocalDateTime.now()));
            log.warn("ALERTA {}: '{}' (id {}) queda con {} unidades tras el pedido {} (umbral {})",
                    alerta.getNivel(), item.nombreProducto(), item.productoId(), item.stockRestante(),
                    evento.pedidoId(), umbral);
            nuevas++;
        }
        return nuevas;
    }

    @Transactional(readOnly = true)
    public List<AlertaStockResponse> recientes(Integer limite) {
        int n = limite == null ? LIMITE_POR_DEFECTO : Math.max(1, Math.min(limite, LIMITE_MAXIMO));
        return repository.recientes(PageRequest.of(0, n)).stream().map(AlertaStockResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public List<AlertaStockResponse> productosCriticos() {
        return repository.ultimaPorProducto().stream().map(AlertaStockResponse::desde).toList();
    }
}
