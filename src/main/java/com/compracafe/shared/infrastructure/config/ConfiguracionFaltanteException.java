package com.compracafe.shared.infrastructure.config;

import java.util.List;

/**
 * Falta una o más variables obligatorias de configuración.
 *
 * Es la única fuente del texto del error: {@link #getDescripcion()} dice qué pasa
 * y {@link #getAccion()} cómo arreglarlo. El mensaje de la excepción las une, y
 * {@link ConfiguracionFaltanteFailureAnalyzer} las muestra por separado.
 */
public class ConfiguracionFaltanteException extends IllegalStateException {

    private final List<String> faltantes;

    public ConfiguracionFaltanteException(List<String> faltantes) {
        super(descripcion(faltantes) + " Cómo arreglarlo: " + ACCION);
        this.faltantes = List.copyOf(faltantes);
    }

    private static final String ACCION = "Copia .env.example a .env en la raíz del proyecto "
            + "(la carpeta donde está pom.xml) y completa esas variables, "
            + "o defínelas como variables de entorno. "
            + "Si ejecutas desde el IDE, el directorio de trabajo debe ser la raíz del proyecto "
            + "para que se encuentre el .env.";

    public List<String> getFaltantes() {
        return faltantes;
    }

    /** Qué pasa: las variables que faltan. */
    public String getDescripcion() {
        return descripcion(faltantes);
    }

    /** Cómo arreglarlo. */
    public String getAccion() {
        return ACCION;
    }

    private static String descripcion(List<String> faltantes) {
        return "Faltan variables de configuración obligatorias: " + String.join(", ", faltantes) + ".";
    }
}
