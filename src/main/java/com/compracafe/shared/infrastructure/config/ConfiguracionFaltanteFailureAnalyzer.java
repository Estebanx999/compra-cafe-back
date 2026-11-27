package com.compracafe.shared.infrastructure.config;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Convierte {@link ConfiguracionFaltanteException} en el bloque
 * "APPLICATION FAILED TO START" de Spring Boot, sin stacktrace.
 *
 * Registrado en META-INF/spring.factories.
 */
public class ConfiguracionFaltanteFailureAnalyzer extends AbstractFailureAnalyzer<ConfiguracionFaltanteException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, ConfiguracionFaltanteException cause) {
        String descripcion = "Faltan variables de configuración obligatorias: "
                + String.join(", ", cause.getFaltantes()) + ".";
        String accion = "Copia .env.example a .env en la raíz del proyecto (donde está pom.xml) "
                + "y completa: " + String.join(", ", cause.getFaltantes()) + ". "
                + "También puedes definirlas como variables de entorno del sistema.";
        return new FailureAnalysis(descripcion, accion, cause);
    }
}
