package com.pedidos360.pedidos.controller;

import com.pedidos360.pedidos.model.Pedido;
import com.pedidos360.pedidos.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    static final String HEADER_USUARIO_EMAIL = "X-Usuario-Email";
    static final String HEADER_USUARIO_NOMBRE = "X-Usuario-Nombre";

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Pedido>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pedido> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<Pedido>> getByCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(service.findByClienteId(clienteId));
    }

    /**
     * Pedidos del usuario autenticado. La identidad la entrega el BFF a partir del JWT.
     */
    @GetMapping("/mios")
    public ResponseEntity<List<Pedido>> getMios(
            @RequestHeader(value = HEADER_USUARIO_EMAIL, required = false) String email) {
        if (email == null || email.isBlank()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(service.findByClienteEmail(email));
    }

    @PostMapping
    public ResponseEntity<Pedido> create(@Valid @RequestBody Pedido pedido,
                                         @RequestHeader(value = HEADER_USUARIO_EMAIL, required = false) String email,
                                         @RequestHeader(value = HEADER_USUARIO_NOMBRE, required = false) String nombre) {
        // Si la peticion viene del BFF, la identidad del token prevalece sobre la del cuerpo
        if (email != null && !email.isBlank()) {
            pedido.setClienteEmail(email);
            // El BFF codifica el nombre para admitir tildes en la cabecera HTTP
            pedido.setClienteNombre(nombre != null ? URLDecoder.decode(nombre, StandardCharsets.UTF_8) : null);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(pedido));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Pedido> updateEstado(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String estado = body.get("estado");
        if (estado == null) {
            return ResponseEntity.badRequest().build();
        }
        return service.updateEstado(id, estado)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (service.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
