package com.compracafe.shared.infrastructure.config;

import java.util.List;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Verifica, antes de crear cualquier bean, que las variables obligatorias
 * estén definidas (en .env o como variables de entorno).
 *
 * Se ejecuta después de que Spring importa el .env, así que si algo falta
 * el error dice exactamente qué variable es y cómo arreglarlo, en lugar de
 * un fallo de conexión poco claro. Aplica igual al arrancar la app y en los tests.
 *
 * Registrado en META-INF/spring.factories.
 */
public class ConfiguracionRequeridaValidator implements EnvironmentPostProcessor, Ordered {

    static final List<String> VARIABLES_REQUERIDAS =
            List.of("DB_NAME", "DB_USER", "DB_PASSWORD", "CORS_ORIGENES_PERMITIDOS");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        List<String> faltantes = VARIABLES_REQUERIDAS.stream()
                .filter(nombre -> {
                    String valor = environment.getProperty(nombre);
                    return valor == null || valor.isBlank();
                })
                .toList();

        if (!faltantes.isEmpty()) {
            throw new ConfiguracionFaltanteException(faltantes);
        }
    }

    @Override
    public int getOrder() {
        // Después de ConfigDataEnvironmentPostProcessor, que es quien importa el .env.
        return Ordered.LOWEST_PRECEDENCE;
    }
}
