package com.pedidos360.analitica.repository;

import com.pedidos360.analitica.model.PedidoReporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PedidoReporteRepository extends JpaRepository<PedidoReporte, Long> {

    @Query("select p.estado, count(p) from PedidoReporte p group by p.estado")
    List<Object[]> contarPorEstado();

    @Query("select coalesce(sum(p.total), 0) from PedidoReporte p where upper(p.estado) <> 'CANCELADO'")
    BigDecimal sumarVentas();

    @Query("select count(p) from PedidoReporte p where upper(p.estado) <> 'CANCELADO'")
    long contarVigentes();

    @Query("select coalesce(sum(p.cantidadItems), 0) from PedidoReporte p where upper(p.estado) <> 'CANCELADO'")
    long sumarUnidades();

    @Query("select max(p.ultimaActualizacion) from PedidoReporte p")
    LocalDateTime ultimaActualizacion();
}
