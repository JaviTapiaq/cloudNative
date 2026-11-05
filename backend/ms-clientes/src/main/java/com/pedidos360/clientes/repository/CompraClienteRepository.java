package com.pedidos360.clientes.repository;

import com.pedidos360.clientes.model.CompraCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompraClienteRepository extends JpaRepository<CompraCliente, Long> {
    boolean existsByPedidoId(Long pedidoId);
    List<CompraCliente> findByClienteIdOrderByFechaDesc(Long clienteId);
}
