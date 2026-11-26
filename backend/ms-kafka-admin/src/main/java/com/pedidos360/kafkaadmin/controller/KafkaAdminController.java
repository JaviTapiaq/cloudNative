package com.pedidos360.kafkaadmin.controller;

import com.pedidos360.kafkaadmin.dto.ActualizarTopicoRequest;
import com.pedidos360.kafkaadmin.dto.ClusterResponse;
import com.pedidos360.kafkaadmin.dto.ConsumerGroupLagResponse;
import com.pedidos360.kafkaadmin.dto.ConsumerGroupResponse;
import com.pedidos360.kafkaadmin.dto.CrearTopicoRequest;
import com.pedidos360.kafkaadmin.dto.TopicoResponse;
import com.pedidos360.kafkaadmin.service.KafkaAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * API de administracion de Kafka (se expone a traves del BFF en /api/kafka-admin/**).
 */
@RestController
@RequestMapping("/api/kafka-admin")
public class KafkaAdminController {

    private final KafkaAdminService service;

    public KafkaAdminController(KafkaAdminService service) {
        this.service = service;
    }

    // --- Cluster ---

    @GetMapping("/cluster")
    public ClusterResponse cluster() {
        return service.describirCluster();
    }

    // --- Topicos ---

    @GetMapping("/topicos")
    public List<TopicoResponse> listarTopicos() {
        return service.listarTopicos();
    }

    @GetMapping("/topicos/{nombre}")
    public TopicoResponse obtenerTopico(@PathVariable String nombre) {
        return service.obtenerTopico(nombre);
    }

    @PostMapping("/topicos")
    public ResponseEntity<TopicoResponse> crearTopico(@Valid @RequestBody CrearTopicoRequest request) {
        TopicoResponse creado = service.crearTopico(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{nombre}")
                .buildAndExpand(creado.nombre()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PutMapping("/topicos/{nombre}")
    public TopicoResponse actualizarTopico(@PathVariable String nombre,
                                           @Valid @RequestBody ActualizarTopicoRequest request) {
        return service.actualizarTopico(nombre, request);
    }

    @DeleteMapping("/topicos/{nombre}")
    public ResponseEntity<Void> eliminarTopico(@PathVariable String nombre) {
        service.eliminarTopico(nombre);
        return ResponseEntity.noContent().build();
    }

    // --- Consumer groups ---

    @GetMapping("/consumer-groups")
    public List<ConsumerGroupResponse> listarConsumerGroups() {
        return service.listarConsumerGroups();
    }

    @GetMapping("/consumer-groups/{groupId}")
    public ConsumerGroupResponse obtenerConsumerGroup(@PathVariable String groupId) {
        return service.obtenerConsumerGroup(groupId);
    }

    @GetMapping("/consumer-groups/{groupId}/lag")
    public ConsumerGroupLagResponse lag(@PathVariable String groupId) {
        return service.lagDeConsumerGroup(groupId);
    }

    @DeleteMapping("/consumer-groups/{groupId}")
    public ResponseEntity<Void> eliminarConsumerGroup(@PathVariable String groupId) {
        service.eliminarConsumerGroup(groupId);
        return ResponseEntity.noContent().build();
    }
}
