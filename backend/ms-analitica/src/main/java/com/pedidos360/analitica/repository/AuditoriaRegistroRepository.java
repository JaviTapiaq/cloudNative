package com.pedidos360.analitica.repository;

import com.pedidos360.analitica.model.AuditoriaRegistro;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditoriaRegistroRepository extends JpaRepository<AuditoriaRegistro, Long> {

    boolean existsByEventId(String eventId);

    /** Filtros opcionales: un parametro nulo no restringe la busqueda. */
    @Query("""
            select a from AuditoriaRegistro a
            where (:entidadId is null or a.entidadId = :entidadId)
              and (:accion is null or a.accion = :accion)
              and (:actor is null or lower(a.actor) = lower(:actor))
            order by a.ocurridoEn desc, a.id desc
            """)
    List<AuditoriaRegistro> buscar(@Param("entidadId") String entidadId, @Param("accion") String accion,
                                   @Param("actor") String actor, Pageable pageable);
}
