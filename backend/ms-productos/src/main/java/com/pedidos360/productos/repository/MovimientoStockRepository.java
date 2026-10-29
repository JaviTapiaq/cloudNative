package com.pedidos360.productos.repository;

import com.pedidos360.productos.model.MovimientoStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {
    boolean existsByPedidoId(Long pedidoId);
}
