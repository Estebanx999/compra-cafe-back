package com.compracafe.shared.infrastructure.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.compracafe.shared.domain.exception.RecursoNoEncontradoException;
import com.compracafe.shared.domain.exception.ReglaDeNegocioException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Prueba el manejador global de errores a través de un controlador falso que
 * existe solo en los tests (clase anidada: Spring Boot la excluye del escaneo
 * normal, así que no aparece en la aplicación ni en otros tests).
 */
@WebMvcTest(ManejadorGlobalErroresTest.ControladorFalso.class)
@Import(ManejadorGlobalErroresTest.ControladorFalso.class)
class ManejadorGlobalErroresTest {

    private static final String DETALLE_INTERNO = "conexión rechazada a 10.0.0.5, usuario=admin";

    @Autowired
    private MockMvc mockMvc;

    // ─── Negocio ────────────────────────────────────────────────────────────

    @Test
    void recursoNoEncontradoResponde404ConSuCodigoYMensaje() throws Exception {
        mockMvc.perform(get("/api/prueba/no-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("CLIENTE_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.detail").value("No existe el cliente con id 15"))
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(sinStacktrace());
    }

    @Test
    void reglaDeNegocioResponde409ConSuCodigoYMensaje() throws Exception {
        mockMvc.perform(get("/api/prueba/regla"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CAJA_SIN_SALDO"))
                .andExpect(jsonPath("$.detail").value("La caja no tiene saldo suficiente"))
                .andExpect(jsonPath("$.title").value("Regla de negocio incumplida"))
                .andExpect(sinStacktrace());
    }

    // ─── Validación ─────────────────────────────────────────────────────────

    @Test
    void validacionDelCuerpoResponde400ConLosCamposQueFallaronEnEspanol() throws Exception {
        mockMvc.perform(post("/api/prueba/validacion")
                        .header("Accept-Language", "en") // aun así debe responder en español
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "  ", "cantidad": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.errores", hasSize(2)))
                .andExpect(jsonPath("$.errores[?(@.campo == 'nombre')].mensaje").value("no debe estar vacío"))
                .andExpect(jsonPath("$.errores[?(@.campo == 'cantidad')].mensaje")
                        .value("debe ser mayor que o igual a 1"))
                .andExpect(sinStacktrace());
    }

    @Test
    void validacionDeUnParametroResponde400ConElNombreDelParametro() throws Exception {
        mockMvc.perform(get("/api/prueba/parametro").param("cantidad", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.errores", hasSize(1)))
                .andExpect(jsonPath("$.errores[0].campo").value("cantidad"))
                .andExpect(jsonPath("$.errores[0].mensaje").value("debe ser mayor que o igual a 1"))
                .andExpect(sinStacktrace());
    }

    // ─── Errores inesperados ────────────────────────────────────────────────

    @Test
    void errorInesperadoResponde500GenericoSinDetalleInterno() throws Exception {
        mockMvc.perform(get("/api/prueba/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
                .andExpect(jsonPath("$.title").value("Error interno"))
                .andExpect(jsonPath("$.detail", containsString("Ocurrió un error inesperado")))
                .andExpect(jsonPath("$.referencia").isNotEmpty())
                .andExpect(content().string(not(containsString(DETALLE_INTERNO))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(sinStacktrace());
    }

    // ─── Errores propios de Spring MVC ──────────────────────────────────────

    @Test
    void jsonMalFormadoResponde400EnEspanol() throws Exception {
        mockMvc.perform(post("/api/prueba/validacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.detail").value("La solicitud no es válida o está mal formada."))
                .andExpect(sinStacktrace());
    }

    @Test
    void metodoNoPermitidoResponde405EnEspanol() throws Exception {
        mockMvc.perform(delete("/api/prueba/regla"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.codigo").value("METODO_NO_PERMITIDO"))
                .andExpect(sinStacktrace());
    }

    @Test
    void rutaInexistenteResponde404EnEspanol() throws Exception {
        mockMvc.perform(get("/api/prueba/no-existe-esta-ruta"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RUTA_NO_ENCONTRADA"))
                .andExpect(sinStacktrace());
    }

    /** Ninguna respuesta de error puede llevar stacktrace ni nombres de clases internas. */
    private static ResultMatcher sinStacktrace() {
        return resultado -> {
            String cuerpo = resultado.getResponse().getContentAsString();
            for (String prohibido : new String[] { "\"trace\"", "\"stackTrace\"", "\"exception\"",
                    "at com.compracafe", "at org.springframework", "java.lang." }) {
                if (cuerpo.contains(prohibido)) {
                    throw new AssertionError("La respuesta contiene '" + prohibido + "': " + cuerpo);
                }
            }
        };
    }

    // ─── Controlador falso, solo para estos tests ──────────────────────────

    @RestController
    @RequestMapping("/prueba")
    static class ControladorFalso {

        @GetMapping("/no-encontrado")
        void noEncontrado() {
            throw new ClienteDePruebaNoEncontrado();
        }

        @GetMapping("/regla")
        void regla() {
            throw new CajaDePruebaSinSaldo();
        }

        @PostMapping("/validacion")
        void validacion(@Valid @RequestBody SolicitudDePrueba solicitud) {
        }

        @GetMapping("/parametro")
        void parametro(@RequestParam @Min(1) int cantidad) {
        }

        @GetMapping("/inesperado")
        void inesperado() {
            throw new IllegalStateException(DETALLE_INTERNO);
        }
    }

    record SolicitudDePrueba(@NotBlank String nombre, @Min(1) int cantidad) {
    }

    static class ClienteDePruebaNoEncontrado extends RecursoNoEncontradoException {
        ClienteDePruebaNoEncontrado() {
            super("CLIENTE_NO_ENCONTRADO", "No existe el cliente con id 15");
        }
    }

    static class CajaDePruebaSinSaldo extends ReglaDeNegocioException {
        CajaDePruebaSinSaldo() {
            super("CAJA_SIN_SALDO", "La caja no tiene saldo suficiente");
        }
    }
}
