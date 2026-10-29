package com.pedidos360.mensajeria.admin.controller;

import com.pedidos360.mensajeria.admin.dto.BindingRequest;
import com.pedidos360.mensajeria.admin.dto.ColaResponse;
import com.pedidos360.mensajeria.admin.dto.CrearColaRequest;
import com.pedidos360.mensajeria.admin.dto.CrearExchangeRequest;
import com.pedidos360.mensajeria.admin.dto.ExchangeResponse;
import com.pedidos360.mensajeria.admin.service.RabbitAdminService;
import com.pedidos360.mensajeria.admin.validation.NombreRecurso;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * API de administracion de RabbitMQ. Valida la entrada y delega en {@link RabbitAdminService}.
 */
@RestController
@RequestMapping("/api/admin")
public class RabbitAdminController {

    private final RabbitAdminService adminService;

    public RabbitAdminController(RabbitAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/topologia")
    public Map<String, Object> topologia() {
        return adminService.topologia();
    }

    // ------------------------------------------------------------------ colas

    @GetMapping("/queues")
    public List<ColaResponse> listarColas() {
        return adminService.listarColasDelSistema();
    }

    @PostMapping("/queues")
    public ResponseEntity<ColaResponse> crearCola(@Valid @RequestBody CrearColaRequest request) {
        ColaResponse cola = adminService.crearCola(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{nombre}").buildAndExpand(cola.nombre()).toUri();
        return ResponseEntity.created(location).body(cola);
    }

    @GetMapping("/queues/{name}")
    public ColaResponse obtenerCola(@PathVariable("name") @NombreRecurso String name) {
        return adminService.obtenerCola(name);
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Void> eliminarCola(@PathVariable("name") @NombreRecurso String name,
                                             @RequestParam(defaultValue = "false") boolean soloSiVacia,
                                             @RequestParam(defaultValue = "false") boolean incluirDlq) {
        adminService.eliminarCola(name, soloSiVacia, incluirDlq);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/queues/{name}/messages")
    public Map<String, Object> purgarCola(@PathVariable("name") @NombreRecurso String name) {
        return Map.of("cola", name, "mensajesEliminados", adminService.purgarCola(name));
    }

    // ------------------------------------------------------------------ exchanges

    @PostMapping("/exchanges")
    public ResponseEntity<ExchangeResponse> crearExchange(@Valid @RequestBody CrearExchangeRequest request) {
        ExchangeResponse exchange = adminService.crearExchange(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{nombre}").buildAndExpand(exchange.nombre()).toUri();
        return ResponseEntity.created(location).body(exchange);
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> eliminarExchange(@PathVariable("name") @NombreRecurso String name) {
        adminService.eliminarExchange(name);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ bindings

    @PostMapping("/bindings")
    public ResponseEntity<BindingRequest> crearBinding(@Valid @RequestBody BindingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.crearBinding(request));
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> eliminarBinding(@Valid @RequestBody BindingRequest request) {
        adminService.eliminarBinding(request);
        return ResponseEntity.noContent().build();
    }
}
