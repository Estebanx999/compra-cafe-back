package com.compracafe.shared.infrastructure.config;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Convierte {@link ConfiguracionFaltanteException} en el bloque
 * "APPLICATION FAILED TO START" de Spring Boot, sin stacktrace.
 *
 * El texto sale de la excepción; aquí no se escribe ninguno.
 *
 * Registrado en META-INF/spring.factories.
 */
public class ConfiguracionFaltanteFailureAnalyzer extends AbstractFailureAnalyzer<ConfiguracionFaltanteException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, ConfiguracionFaltanteException cause) {
        return new FailureAnalysis(cause.getDescripcion(), cause.getAccion(), cause);
    }
}
