package com.pedidos360.analitica.repository;

import com.pedidos360.analitica.model.AlertaStock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AlertaStockRepository extends JpaRepository<AlertaStock, Long> {

    boolean existsByEventIdAndProductoId(String eventId, Long productoId);

    @Query("select a from AlertaStock a order by a.id desc")
    List<AlertaStock> recientes(Pageable pageable);

    /** La ultima alerta de cada producto, de menor a mayor stock restante. */
    @Query("""
            select a from AlertaStock a
            where a.id in (select max(b.id) from AlertaStock b group by b.productoId)
            order by a.stockRestante asc, a.id desc
            """)
    List<AlertaStock> ultimaPorProducto();
}
