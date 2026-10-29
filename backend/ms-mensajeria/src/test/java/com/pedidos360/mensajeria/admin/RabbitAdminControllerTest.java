package com.pedidos360.mensajeria.admin;

import com.pedidos360.mensajeria.admin.controller.RabbitAdminController;
import com.pedidos360.mensajeria.admin.dto.ColaResponse;
import com.pedidos360.mensajeria.admin.service.RabbitAdminService;
import com.pedidos360.mensajeria.exception.RecursoProtegidoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RabbitAdminController.class)
class RabbitAdminControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RabbitAdminService adminService;

    @Test
    void creaColaValida() throws Exception {
        when(adminService.crearCola(any())).thenReturn(new ColaResponse("demo.queue", 0, 0, false));

        mvc.perform(post("/api/admin/queues").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"demo.queue\",\"crearDlq\":true,\"ttlMs\":60000}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/admin/queues/demo.queue"))
                .andExpect(jsonPath("$.nombre").value("demo.queue"));
    }

    @Test
    void rechazaColaConNombreVacio() throws Exception {
        mvc.perform(post("/api/admin/queues").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores").isArray());
        verifyNoInteractions(adminService);
    }

    @Test
    void rechazaNombreReservadoYConfiguracionInvalida() throws Exception {
        mvc.perform(post("/api/admin/queues").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"amq.hack\",\"ttlMs\":-5,\"exchange\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.length()").value(3));
        verifyNoInteractions(adminService);
    }

    @Test
    void rechazaTipoDeExchangeInvalido() throws Exception {
        mvc.perform(post("/api/admin/exchanges").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"demo.exchange\",\"tipo\":\"broadcast\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(adminService);
    }

    @Test
    void rechazaNombreInvalidoEnPath() throws Exception {
        mvc.perform(delete("/api/admin/queues/{name}", "cola con espacios"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(adminService);
    }

    @Test
    void noPermiteEliminarColaProtegida() throws Exception {
        doThrow(new RecursoProtegidoException("protegida"))
                .when(adminService).eliminarCola("pedidos360.tickets.queue", false, false);

        mvc.perform(delete("/api/admin/queues/{name}", "pedidos360.tickets.queue"))
                .andExpect(status().isForbidden());
    }

    @Test
    void eliminaCola() throws Exception {
        mvc.perform(delete("/api/admin/queues/{name}", "demo.queue"))
                .andExpect(status().isNoContent());
    }
}
