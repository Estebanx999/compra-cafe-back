package com.compracafe.shared.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class ConfiguracionRequeridaValidatorTest {

    private final ConfiguracionRequeridaValidator validator = new ConfiguracionRequeridaValidator();

    @Test
    void pasaCuandoEstanTodasLasVariables() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("DB_NAME", "compra_cafe")
                .withProperty("DB_USER", "usuario")
                .withProperty("DB_PASSWORD", "secreto")
                .withProperty("CORS_ORIGENES_PERMITIDOS", "http://localhost:5173");

        assertThatCode(() -> validator.postProcessEnvironment(env, new SpringApplication()))
                .doesNotThrowAnyException();
    }

    @Test
    void fallaIndicandoCualVariableFaltaYComoArreglarlo() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("DB_NAME", "compra_cafe")
                .withProperty("DB_USER", "usuario")
                .withProperty("DB_PASSWORD", "  ")
                .withProperty("CORS_ORIGENES_PERMITIDOS", "http://localhost:5173");

        assertThatThrownBy(() -> validator.postProcessEnvironment(env, new SpringApplication()))
                .isInstanceOf(ConfiguracionFaltanteException.class)
                .hasMessageContaining("DB_PASSWORD")
                .hasMessageContaining("Copia .env.example a .env")
                .satisfies(e -> assertThat(((ConfiguracionFaltanteException) e).getFaltantes())
                        .containsExactly("DB_PASSWORD"));
    }
}
