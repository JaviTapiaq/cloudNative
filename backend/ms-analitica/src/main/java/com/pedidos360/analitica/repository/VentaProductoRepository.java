package com.pedidos360.analitica.repository;

import com.pedidos360.analitica.model.VentaProducto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface VentaProductoRepository extends JpaRepository<VentaProducto, Long> {

    @Query("select v from VentaProducto v order by v.unidadesVendidas desc, v.ingresos desc")
    List<VentaProducto> topPorUnidades(Pageable pageable);

    /**
     * Suma atomica en la base de datos. Las 3 hebras del consumidor (una por particion) pueden procesar
     * a la vez pedidos con el mismo producto; un "leer, sumar y guardar" en Java perderia actualizaciones.
     *
     * @return filas afectadas: 0 si el producto aun no tiene acumulado
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update VentaProducto v
               set v.unidadesVendidas = v.unidadesVendidas + :unidades,
                   v.ingresos = v.ingresos + :ingresos
             where v.productoId = :productoId
            """)
    int acumular(@Param("productoId") Long productoId, @Param("unidades") long unidades,
                 @Param("ingresos") BigDecimal ingresos);
}
