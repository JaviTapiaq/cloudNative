package com.pedidos360.bff.controller;

import com.pedidos360.bff.service.ProxyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
public class BffController {

    private final ProxyService proxyService;

    public BffController(ProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @RequestMapping("/clientes/**")
    public ResponseEntity<String> proxyClientes(HttpServletRequest request,
                                                 @RequestBody(required = false) String body) {
        return proxyService.forward(request, body, "clientes");
    }

    @RequestMapping("/productos/**")
    public ResponseEntity<String> proxyProductos(HttpServletRequest request,
                                                  @RequestBody(required = false) String body) {
        return proxyService.forward(request, body, "productos");
    }

    @RequestMapping("/pedidos/**")
    public ResponseEntity<String> proxyPedidos(HttpServletRequest request,
                                                @RequestBody(required = false) String body) {
        return proxyService.forward(request, body, "pedidos");
    }
}
