package com.pedidos360.analitica.service;

import com.pedidos360.analitica.dto.MetricasResponse;
import com.pedidos360.analitica.messaging.model.PedidoEventoMessage;
import com.pedidos360.analitica.model.EventoProcesado;
import com.pedidos360.analitica.model.PedidoReporte;
import com.pedidos360.analitica.model.VentaProducto;
import com.pedidos360.analitica.repository.EventoProcesadoRepository;
import com.pedidos360.analitica.repository.PedidoReporteRepository;
import com.pedidos360.analitica.repository.VentaProductoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Logica de reporteria: aplica los eventos de pedidos a las metricas y las expone.
 * No conoce Kafka: recibe el evento ya validado, lo que permite probarla de forma aislada.
 */
@Service
public class ReporteriaService {

    private static final int TOP_POR_DEFECTO = 5;
    private static final int TOP_MAXIMO = 50;

    private final PedidoReporteRepository pedidos;
    private final VentaProductoRepository ventas;
    private final EventoProcesadoRepository eventosProcesados;

    public ReporteriaService(PedidoReporteRepository pedidos, VentaProductoRepository ventas,
                             EventoProcesadoRepository eventosProcesados) {
        this.pedidos = pedidos;
        this.ventas = ventas;
        this.eventosProcesados = eventosProcesados;
    }

    /**
     * Aplica un evento a las metricas. Es idempotente por eventId.
     *
     * @return false si el evento ya se habia procesado (reentrega)
     */
    @Transactional
    public boolean procesar(PedidoEventoMessage evento) {
        if (eventosProcesados.existsById(evento.eventId())) {
            return false;
        }
        if (evento.esCreado()) {
            aplicarCreado(evento);
        } else if (evento.esCambioDeEstado()) {
            aplicarCambioDeEstado(evento);
        } else {
            throw new IllegalArgumentException("Tipo de evento desconocido: " + evento.tipoEvento());
        }
        eventosProcesados.save(new EventoProcesado(evento.eventId()));
        return true;
    }

    private void aplicarCreado(PedidoEventoMessage evento) {
        PedidoReporte pedido = pedidos.findById(evento.pedidoId())
                .orElseGet(() -> new PedidoReporte(evento.pedidoId(), evento.estado()));
        boolean yaContado = pedido.getFechaCreacion() != null;
        pedido.setClienteId(evento.clienteId());
        pedido.setClienteEmail(evento.clienteEmail());
        pedido.setClienteNombre(evento.clienteNombre());
        pedido.setTotal(evento.total());
        pedido.setCantidadItems(evento.cantidadItems());
        pedido.setFechaCreacion(evento.fechaEvento() != null ? evento.fechaEvento() : LocalDateTime.now());
        pedido.setUltimaActualizacion(LocalDateTime.now());
        pedidos.save(pedido);

        if (!yaContado && evento.items() != null) {
            for (PedidoEventoMessage.Item item : evento.items()) {
                acumularVenta(item);
            }
        }
    }

    /**
     * Suma atomica; si el producto aun no existe lo inserta. Si dos hebras lo insertan a la vez, una falla
     * por clave duplicada, se revierte todo el evento (incluida su marca de procesado) y el error handler
     * del consumidor lo reintenta: en el reintento el producto ya existe y se suma por la via atomica.
     */
    private void acumularVenta(PedidoEventoMessage.Item item) {
        BigDecimal ingresos = item.precioUnitario() == null ? BigDecimal.ZERO
                : item.precioUnitario().multiply(BigDecimal.valueOf(item.cantidad()));
        if (ventas.acumular(item.productoId(), item.cantidad(), ingresos) == 0) {
            VentaProducto nueva = new VentaProducto(item.productoId(), item.nombreProducto());
            nueva.acumular(item.cantidad(), item.precioUnitario());
            ventas.saveAndFlush(nueva);
        }
    }

    private void aplicarCambioDeEstado(PedidoEventoMessage evento) {
        PedidoReporte pedido = pedidos.findById(evento.pedidoId())
                .orElseGet(() -> new PedidoReporte(evento.pedidoId(), evento.estado()));
        pedido.setEstado(evento.estado());
        pedido.setUltimaActualizacion(LocalDateTime.now());
        pedidos.save(pedido);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> pedidosPorEstado() {
        Map<String, Long> resultado = new LinkedHashMap<>();
        for (Object[] fila : pedidos.contarPorEstado()) {
            resultado.put(String.valueOf(fila[0]), ((Number) fila[1]).longValue());
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<MetricasResponse.ProductoVendido> topProductos(Integer limite) {
        int n = limite == null ? TOP_POR_DEFECTO : Math.max(1, Math.min(limite, TOP_MAXIMO));
        return ventas.topPorUnidades(PageRequest.of(0, n)).stream()
                .map(v -> new MetricasResponse.ProductoVendido(v.getProductoId(), v.getNombreProducto(),
                        v.getUnidadesVendidas(), v.getIngresos()))
                .toList();
    }

    @Transactional(readOnly = true)
    public MetricasResponse metricas() {
        long vigentes = pedidos.contarVigentes();
        BigDecimal ventasTotales = pedidos.sumarVentas();
        BigDecimal ticket = vigentes == 0 ? BigDecimal.ZERO
                : ventasTotales.divide(BigDecimal.valueOf(vigentes), 2, RoundingMode.HALF_UP);
        return new MetricasResponse(pedidos.count(), ventasTotales, ticket, pedidos.sumarUnidades(),
                pedidosPorEstado(), topProductos(TOP_POR_DEFECTO), pedidos.ultimaActualizacion());
    }
}
