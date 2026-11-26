package com.pedidos360.analitica.controller;

import com.pedidos360.analitica.dto.MetricasResponse;
import com.pedidos360.analitica.service.ReporteriaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Metricas en tiempo real calculadas desde el stream Kafka de pedidos.
 */
@RestController
@RequestMapping("/api/analitica")
public class ReporteriaController {

    private final ReporteriaService service;

    public ReporteriaController(ReporteriaService service) {
        this.service = service;
    }

    @GetMapping("/metricas")
    public MetricasResponse metricas() {
        return service.metricas();
    }

    @GetMapping("/pedidos-por-estado")
    public Map<String, Long> pedidosPorEstado() {
        return service.pedidosPorEstado();
    }

    @GetMapping("/top-productos")
    public List<MetricasResponse.ProductoVendido> topProductos(@RequestParam(required = false) Integer limite) {
        return service.topProductos(limite);
    }
}
