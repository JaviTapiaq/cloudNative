package com.pedidos360.productos.service;

import com.pedidos360.productos.model.MovimientoStock;
import com.pedidos360.productos.model.Producto;
import com.pedidos360.productos.repository.MovimientoStockRepository;
import com.pedidos360.productos.repository.ProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Logica de negocio del descuento de stock. No conoce RabbitMQ.
 */
@Service
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private final ProductoRepository productoRepository;
    private final MovimientoStockRepository movimientoRepository;

    public StockService(ProductoRepository productoRepository, MovimientoStockRepository movimientoRepository) {
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
    }

    /**
     * Descuenta el stock de todos los productos de un pedido en una sola transaccion:
     * si un producto no alcanza, no se descuenta ninguno.
     *
     * @param cantidades productoId -> unidades compradas
     * @return resumen para la bitacora
     */
    @Transactional
    public String descontar(Long pedidoId, Map<Long, Integer> cantidades) {
        if (movimientoRepository.existsByPedidoId(pedidoId)) {
            log.info("Stock del pedido {} ya fue descontado; evento duplicado ignorado", pedidoId);
            return "Pedido " + pedidoId + " ya procesado (duplicado ignorado)";
        }

        int unidades = 0;
        for (Map.Entry<Long, Integer> item : cantidades.entrySet()) {
            Producto producto = productoRepository.findById(item.getKey())
                    .orElseThrow(() -> new StockInsuficienteException("Producto " + item.getKey() + " no existe"));
            int disponible = producto.getStock() != null ? producto.getStock() : 0;
            if (disponible < item.getValue()) {
                throw new StockInsuficienteException("Stock insuficiente para '" + producto.getNombre()
                        + "': disponible " + disponible + ", solicitado " + item.getValue());
            }
            producto.setStock(disponible - item.getValue());
            productoRepository.save(producto);
            unidades += item.getValue();
            log.info("Stock de '{}' descontado: {} -> {}", producto.getNombre(), disponible, producto.getStock());
        }

        movimientoRepository.save(new MovimientoStock(pedidoId, unidades));
        return "Stock descontado para pedido " + pedidoId + " (" + unidades + " unidad(es))";
    }
}
