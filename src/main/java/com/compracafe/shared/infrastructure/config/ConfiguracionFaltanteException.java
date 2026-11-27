package com.compracafe.shared.infrastructure.config;

import java.util.List;

/**
 * Falta una o más variables obligatorias de configuración.
 */
public class ConfiguracionFaltanteException extends IllegalStateException {

    private final List<String> faltantes;

    public ConfiguracionFaltanteException(List<String> faltantes) {
        super(construirMensaje(faltantes));
        this.faltantes = List.copyOf(faltantes);
    }

    public List<String> getFaltantes() {
        return faltantes;
    }

    static String construirMensaje(List<String> faltantes) {
        return "Faltan variables de configuración obligatorias: " + String.join(", ", faltantes) + ". "
                + "Cómo arreglarlo: copia .env.example a .env en la raíz del proyecto "
                + "(la carpeta donde está pom.xml) y completa esas variables, "
                + "o defínelas como variables de entorno. "
                + "Si ejecutas desde el IDE, el directorio de trabajo debe ser la raíz del proyecto "
                + "para que se encuentre el .env.";
    }
}
