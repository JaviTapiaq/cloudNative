package com.pedidos360.mensajeria.service.documentos;

import com.pedidos360.mensajeria.messaging.model.DocumentoMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Logica de negocio para generar documentos (boleta, factura, guia de despacho).
 * El documento se genera como texto en memoria; en produccion se guardaria en un storage.
 */
@Service
public class DocumentoService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoService.class);

    private final AtomicLong folio = new AtomicLong(1000);
    private final Map<String, String> documentos = new ConcurrentHashMap<>();

    public String generar(DocumentoMessage solicitud) {
        String formato = solicitud.formato() != null ? solicitud.formato() : "PDF";
        String nombre = solicitud.tipoDocumento() + "-" + folio.incrementAndGet() + "." + formato.toLowerCase();
        String contenido = "%s%nPedido: %d%nCliente: %s%nEmitido: %s%n".formatted(
                solicitud.tipoDocumento(), solicitud.pedidoId(),
                solicitud.cliente() != null ? solicitud.cliente() : "-", LocalDateTime.now());
        documentos.put(nombre, contenido);
        log.info("Documento {} generado para pedido {}", nombre, solicitud.pedidoId());
        return "Documento " + nombre + " generado";
    }

    public List<String> listarNombres() {
        return documentos.keySet().stream().sorted().toList();
    }
}
