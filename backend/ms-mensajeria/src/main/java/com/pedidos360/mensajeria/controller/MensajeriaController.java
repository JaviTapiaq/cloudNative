package com.pedidos360.mensajeria.controller;

import com.pedidos360.mensajeria.messaging.model.DocumentoMessage;
import com.pedidos360.mensajeria.messaging.model.NotificacionMessage;
import com.pedidos360.mensajeria.messaging.model.TicketMessage;
import com.pedidos360.mensajeria.messaging.producer.MensajePublisher;
import com.pedidos360.mensajeria.service.documentos.DocumentoService;
import com.pedidos360.mensajeria.service.registro.RegistroProcesamiento;
import com.pedidos360.mensajeria.service.registro.RegistroProcesamientoService;
import com.pedidos360.mensajeria.service.tickets.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Endpoints para publicar tareas asincronas y observar su resultado.
 *
 * Los cuerpos NO se validan aqui a proposito: la validacion la hace el consumidor, para
 * poder demostrar que un mensaje invalido termina en la DLQ sin afectar al resto.
 * La respuesta es 202 Accepted: el procesamiento ocurre despues, de forma desacoplada.
 */
@RestController
@RequestMapping("/api/mensajeria")
public class MensajeriaController {

    private final MensajePublisher publisher;
    private final RegistroProcesamientoService registro;
    private final TicketService ticketService;
    private final DocumentoService documentoService;

    public MensajeriaController(MensajePublisher publisher, RegistroProcesamientoService registro,
                                TicketService ticketService, DocumentoService documentoService) {
        this.publisher = publisher;
        this.registro = registro;
        this.ticketService = ticketService;
        this.documentoService = documentoService;
    }

    @PostMapping("/notificaciones")
    public ResponseEntity<Map<String, String>> notificar(@RequestBody NotificacionMessage mensaje) {
        return aceptado(publisher.publicarNotificacion(mensaje));
    }

    @PostMapping("/tickets")
    public ResponseEntity<Map<String, String>> generarTicket(@RequestBody TicketMessage mensaje) {
        return aceptado(publisher.publicarTicket(mensaje));
    }

    @PostMapping("/documentos")
    public ResponseEntity<Map<String, String>> generarDocumento(@RequestBody DocumentoMessage mensaje) {
        return aceptado(publisher.publicarDocumento(mensaje));
    }

    @GetMapping("/registro")
    public List<RegistroProcesamiento> registro() {
        return registro.listar();
    }

    @DeleteMapping("/registro")
    public ResponseEntity<Void> limpiarRegistro() {
        registro.limpiar();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tickets")
    public List<String> tickets() {
        return ticketService.listarNumeros();
    }

    @GetMapping("/documentos")
    public List<String> documentos() {
        return documentoService.listarNombres();
    }

    private ResponseEntity<Map<String, String>> aceptado(String messageId) {
        return ResponseEntity.accepted().body(Map.of("messageId", messageId, "estado", "ENCOLADO"));
    }
}
