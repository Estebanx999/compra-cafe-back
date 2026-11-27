package com.compracafe.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CORS de la API: qué orígenes pasan, cuáles se rechazan y cómo se valida la lista.
 *
 * Los orígenes se fijan aquí con @TestPropertySource para que el test no dependa
 * del valor que tenga cada uno en su .env.
 */
@WebMvcTest(ConfiguracionCorsTest.ControladorFalso.class)
@Import(ConfiguracionCorsTest.ControladorFalso.class)
@TestPropertySource(properties = "app.cors.origenes-permitidos="
        + ConfiguracionCorsTest.ORIGEN_PERMITIDO + " , " + ConfiguracionCorsTest.OTRO_ORIGEN_PERMITIDO)
class ConfiguracionCorsTest {

    static final String ORIGEN_PERMITIDO = "http://localhost:5173";
    static final String OTRO_ORIGEN_PERMITIDO = "https://cafe.ejemplo.com";
    static final String ORIGEN_NO_PERMITIDO = "http://sitio-ajeno.com";

    private static final String RUTA = ConfiguracionPrefijoApi.PREFIJO + "/prueba-cors";

    @Autowired
    private MockMvc mockMvc;

    // ─── Orígenes permitidos ────────────────────────────────────────────────

    @Test
    void origenPermitidoRecibeLaCabeceraDeCors() throws Exception {
        mockMvc.perform(get(RUTA).header(HttpHeaders.ORIGIN, ORIGEN_PERMITIDO))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEN_PERMITIDO))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void cadaOrigenDeLaListaSeparadaPorComasEsPermitido() throws Exception {
        mockMvc.perform(get(RUTA).header(HttpHeaders.ORIGIN, OTRO_ORIGEN_PERMITIDO))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, OTRO_ORIGEN_PERMITIDO));
    }

    @Test
    void preflightDeOrigenPermitidoDevuelveLosMetodosPermitidos() throws Exception {
        mockMvc.perform(options(RUTA)
                        .header(HttpHeaders.ORIGIN, ORIGEN_PERMITIDO)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEN_PERMITIDO))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,PATCH,DELETE"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    // ─── Orígenes no permitidos ─────────────────────────────────────────────

    @Test
    void origenNoPermitidoSeRechaza() throws Exception {
        mockMvc.perform(get(RUTA).header(HttpHeaders.ORIGIN, ORIGEN_NO_PERMITIDO))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void preflightDeOrigenNoPermitidoSeRechaza() throws Exception {
        mockMvc.perform(options(RUTA)
                        .header(HttpHeaders.ORIGIN, ORIGEN_NO_PERMITIDO)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    // ─── Validación de la lista de orígenes ─────────────────────────────────

    @Test
    void separaPorComasYQuitaEspacios() {
        assertThat(ConfiguracionCors.leerOrigenes(" http://localhost:5173 , https://cafe.ejemplo.com ,"))
                .containsExactly("http://localhost:5173", "https://cafe.ejemplo.com");
    }

    @Test
    void listaVaciaFalla() {
        assertThatThrownBy(() -> ConfiguracionCors.leerOrigenes(" , "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("CORS_ORIGENES_PERMITIDOS no tiene ningún origen. "
                        + "Ej: CORS_ORIGENES_PERMITIDOS=http://localhost:5173");
    }

    @Test
    void asteriscoFalla() {
        assertThatThrownBy(() -> ConfiguracionCors.leerOrigenes("http://localhost:5173,*"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("CORS_ORIGENES_PERMITIDOS no admite '*': indica los orígenes del frontend uno por uno.");
    }

    @Test
    void origenSinHttpNiHttpsFalla() {
        assertThatThrownBy(() -> ConfiguracionCors.leerOrigenes("localhost:5173"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("El origen CORS 'localhost:5173' debe empezar por http:// o https://. "
                        + "Ej: http://localhost:5173");
    }

    @Test
    void origenConBarraAlFinalFalla() {
        assertThatThrownBy(() -> ConfiguracionCors.leerOrigenes("http://localhost:5173/"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("El origen CORS 'http://localhost:5173/' no debe terminar en '/': "
                        + "el navegador envía el origen sin ella y no coincidiría.");
    }

    // ─── Controlador falso, solo para estos tests ──────────────────────────

    @RestController
    @RequestMapping("/prueba-cors")
    static class ControladorFalso {

        @GetMapping
        String leer() {
            return "ok";
        }

        @PostMapping
        String crear() {
            return "creado";
        }
    }
}
