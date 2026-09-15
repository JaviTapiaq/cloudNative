package com.pedidos360.pedidos.service;

import com.pedidos360.pedidos.model.Pedido;
import com.pedidos360.pedidos.repository.PedidoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PedidoService {

    private final PedidoRepository repository;

    public PedidoService(PedidoRepository repository) {
        this.repository = repository;
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

    public Pedido save(Pedido pedido) {
        if (pedido.getFechaCreacion() == null) {
            pedido.setFechaCreacion(LocalDateTime.now());
        }
        if (pedido.getEstado() == null) {
            pedido.setEstado("PENDIENTE");
        }
        calcularTotal(pedido);
        return repository.save(pedido);
    }

    public Optional<Pedido> updateEstado(Long id, String estado) {
        return repository.findById(id).map(existing -> {
            existing.setEstado(estado);
            return repository.save(existing);
        });
    }

    public boolean delete(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }

    private void calcularTotal(Pedido pedido) {
        BigDecimal total = pedido.getDetalles().stream()
                .map(d -> d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        pedido.setTotal(total);
    }
}
