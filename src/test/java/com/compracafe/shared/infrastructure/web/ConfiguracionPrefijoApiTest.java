package com.compracafe.shared.infrastructure.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Un @RestController declara su ruta sin el prefijo y queda publicado bajo /api.
 */
@WebMvcTest(ConfiguracionPrefijoApiTest.ControladorX.class)
@Import(ConfiguracionPrefijoApiTest.ControladorX.class)
class ConfiguracionPrefijoApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void elControladorRespondeBajoElPrefijoApi() throws Exception {
        mockMvc.perform(get(ConfiguracionPrefijoApi.PREFIJO + "/x"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void elControladorNoRespondeSinElPrefijo() throws Exception {
        mockMvc.perform(get("/x"))
                .andExpect(status().isNotFound());
    }

    @RestController
    @RequestMapping("/x")
    static class ControladorX {

        @GetMapping
        String responder() {
            return "ok";
        }
    }
}
