package com.pedidos360.kafkaadmin;

import com.pedidos360.kafkaadmin.controller.KafkaAdminController;
import com.pedidos360.kafkaadmin.dto.TopicoResponse;
import com.pedidos360.kafkaadmin.exception.OperacionInvalidaException;
import com.pedidos360.kafkaadmin.exception.RecursoProtegidoException;
import com.pedidos360.kafkaadmin.service.KafkaAdminService;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KafkaAdminController.class)
@Import(com.pedidos360.kafkaadmin.exception.GlobalExceptionHandler.class)
class KafkaAdminControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private KafkaAdminService service;

    private TopicoResponse topico(String nombre) {
        return new TopicoResponse(nombre, false, 3, 3, Map.of("retention.ms", "604800000"), List.of());
    }

    @Test
    void creaTopicoValido() throws Exception {
        when(service.crearTopico(any())).thenReturn(topico("demo.topic"));

        mvc.perform(post("/api/kafka-admin/topicos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"demo.topic\",\"particiones\":3,\"factorReplicacion\":3,"
                                + "\"configuraciones\":{\"retention.ms\":\"604800000\"}}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/kafka-admin/topicos/demo.topic"))
                .andExpect(jsonPath("$.nombre").value("demo.topic"))
                .andExpect(jsonPath("$.particiones").value(3));
    }

    @Test
    void rechazaNombreInvalidoSinLlamarAKafka() throws Exception {
        mvc.perform(post("/api/kafka-admin/topicos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"nombre con espacios\",\"particiones\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores").isArray());
        verifyNoInteractions(service);
    }

    @Test
    void topicoDuplicadoDevuelve409() throws Exception {
        when(service.crearTopico(any())).thenThrow(new TopicExistsException("ya existe"));

        mvc.perform(post("/api/kafka-admin/topicos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"demo.topic\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void topicoInexistenteDevuelve404() throws Exception {
        when(service.obtenerTopico("nada")).thenThrow(new UnknownTopicOrPartitionException("no existe"));

        mvc.perform(get("/api/kafka-admin/topicos/nada")).andExpect(status().isNotFound());
    }

    @Test
    void reducirParticionesDevuelve400() throws Exception {
        when(service.actualizarTopico(eq("demo.topic"), any()))
                .thenThrow(new OperacionInvalidaException("Kafka no permite reducir particiones"));

        mvc.perform(put("/api/kafka-admin/topicos/demo.topic").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"particiones\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Operacion invalida"));
    }

    @Test
    void topicoInternoDevuelve403AlEliminar() throws Exception {
        doThrow(new RecursoProtegidoException("interno")).when(service).eliminarTopico("__consumer_offsets");

        mvc.perform(delete("/api/kafka-admin/topicos/__consumer_offsets")).andExpect(status().isForbidden());
    }

    @Test
    void eliminaTopicoExistente() throws Exception {
        mvc.perform(delete("/api/kafka-admin/topicos/demo.topic")).andExpect(status().isNoContent());
    }

    @Test
    void kafkaCaidoDevuelve503() throws Exception {
        when(service.listarTopicos()).thenThrow(new TimeoutException("sin respuesta"));

        mvc.perform(get("/api/kafka-admin/topicos")).andExpect(status().isServiceUnavailable());
    }
}
