package com.pedidos360.analitica.controller;

import com.pedidos360.analitica.dto.AlertaStockResponse;
import com.pedidos360.analitica.service.AlertaStockService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Alertas de stock bajo generadas desde el stream Kafka de pedidos.
 */
@RestController
@RequestMapping("/api/analitica/alertas")
public class AlertasController {

    private final AlertaStockService service;

    public AlertasController(AlertaStockService service) {
        this.service = service;
    }

    /** Historial de alertas, de la mas reciente a la mas antigua. */
    @GetMapping("/stock-bajo")
    public List<AlertaStockResponse> recientes(@RequestParam(required = false) Integer limite) {
        return service.recientes(limite);
    }

    /** Ultima alerta de cada producto, de menor a mayor stock restante. */
    @GetMapping("/productos-criticos")
    public List<AlertaStockResponse> productosCriticos() {
        return service.productosCriticos();
    }
}
