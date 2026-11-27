package com.compracafe.shared.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.diagnostics.FailureAnalysis;

class ConfiguracionFaltanteFailureAnalyzerTest {

    @Test
    void muestraElMismoTextoQueLaExcepcion() {
        ConfiguracionFaltanteException excepcion =
                new ConfiguracionFaltanteException(List.of("DB_PASSWORD", "CORS_ORIGENES_PERMITIDOS"));

        FailureAnalysis analisis = new ConfiguracionFaltanteFailureAnalyzer().analyze(excepcion);

        assertThat(analisis.getDescription())
                .isEqualTo(excepcion.getDescripcion())
                .contains("DB_PASSWORD", "CORS_ORIGENES_PERMITIDOS");
        assertThat(analisis.getAction()).isEqualTo(excepcion.getAccion());
        assertThat(excepcion.getMessage())
                .contains(excepcion.getDescripcion())
                .contains(excepcion.getAccion());
    }
}
