package com.pedidos360.kafkaadmin.dto;

import java.util.List;

public record ClusterResponse(String clusterId, Integer controlador, List<Broker> brokers) {

    public record Broker(int id, String host, int puerto) {
    }
}
