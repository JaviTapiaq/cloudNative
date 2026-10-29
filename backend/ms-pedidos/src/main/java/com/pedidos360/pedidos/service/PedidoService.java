package com.pedidos360.pedidos.service;

import com.pedidos360.pedidos.client.ProductoClient;
import com.pedidos360.pedidos.client.ProductoInfo;
import com.pedidos360.pedidos.event.PedidoEvent;
import com.pedidos360.pedidos.exception.PedidoInvalidoException;
import com.pedidos360.pedidos.exception.StockInsuficienteException;
import com.pedidos360.pedidos.model.DetallePedido;
import com.pedidos360.pedidos.model.Pedido;
import com.pedidos360.pedidos.repository.PedidoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PedidoService {

    private final PedidoRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final ProductoClient productoClient;

    public PedidoService(PedidoRepository repository, ApplicationEventPublisher eventPublisher,
                         ProductoClient productoClient) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.productoClient = productoClient;
    }

    public List<Pedido> findAll() {
        return repository.findAll();
    }

    public Optional<Pedido> findById(Long id) {
        return repository.findById(id);
    }

    public List<Pedido> findByClienteId(Long clienteId) {
        return repository.findByClienteId(clienteId);
    }

    public List<Pedido> findByClienteEmail(String clienteEmail) {
        return repository.findByClienteEmailIgnoreCaseOrderByFechaCreacionDesc(clienteEmail);
    }

    public Pedido save(Pedido pedido) {
        if (pedido.getFechaCreacion() == null) {
            pedido.setFechaCreacion(LocalDateTime.now());
        }
        if (pedido.getEstado() == null) {
            pedido.setEstado("PENDIENTE");
        }
        cotizarConPreciosVigentes(pedido);
        calcularTotal(pedido);
        Pedido guardado = repository.save(pedido);
        eventPublisher.publishEvent(PedidoEvent.creado(guardado));
        return guardado;
    }

    public Optional<Pedido> updateEstado(Long id, String estado) {
        return repository.findById(id).map(existing -> {
            existing.setEstado(estado);
            Pedido actualizado = repository.save(existing);
            eventPublisher.publishEvent(PedidoEvent.estadoActualizado(actualizado));
            return actualizado;
        });
    }

    public boolean delete(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Valida cada producto contra ms-productos y reemplaza el precio enviado por el cliente
     * por el precio vigente, para que el total no pueda manipularse desde el navegador.
     */
    private void cotizarConPreciosVigentes(Pedido pedido) {
        if (pedido.getDetalles() == null || pedido.getDetalles().isEmpty()) {
            throw new PedidoInvalidoException("El pedido debe incluir al menos un producto");
        }

        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (DetallePedido detalle : pedido.getDetalles()) {
            if (detalle.getProductoId() == null || detalle.getCantidad() == null || detalle.getCantidad() <= 0) {
                throw new PedidoInvalidoException("Cada producto debe tener productoId y una cantidad mayor a 0");
            }
            cantidades.merge(detalle.getProductoId(), detalle.getCantidad(), Integer::sum);
        }

        Map<Long, ProductoInfo> productos = new HashMap<>();
        cantidades.forEach((productoId, cantidad) -> {
            ProductoInfo producto = productoClient.obtener(productoId);
            int stock = producto.stock() != null ? producto.stock() : 0;
            if (stock < cantidad) {
                throw new StockInsuficienteException(producto.nombre(), stock, cantidad);
            }
            productos.put(productoId, producto);
        });

        for (DetallePedido detalle : pedido.getDetalles()) {
            ProductoInfo producto = productos.get(detalle.getProductoId());
            detalle.setPrecioUnitario(producto.precio());
            detalle.setNombreProducto(producto.nombre());
        }
    }

    private void calcularTotal(Pedido pedido) {
        BigDecimal total = pedido.getDetalles().stream()
                .map(d -> d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        pedido.setTotal(total);
    }
}
