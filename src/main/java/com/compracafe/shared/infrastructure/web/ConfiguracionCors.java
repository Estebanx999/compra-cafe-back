package com.compracafe.shared.infrastructure.web;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS para las rutas bajo /api (prefijo tomado de {@link ConfiguracionPrefijoApi#PREFIJO}).
 *
 * El frontend vive en otro repositorio y se sirve desde otro origen, así que el
 * navegador solo le deja llamar a la API si el origen está en
 * CORS_ORIGENES_PERMITIDOS (lista separada por comas, en .env o variable de entorno).
 *
 * Sin credenciales (cookies / Authorization) por ahora: todavía no hay inicio de sesión.
 */
@Configuration
public class ConfiguracionCors implements WebMvcConfigurer {

    static final String RUTAS = ConfiguracionPrefijoApi.PREFIJO + "/**";
    static final String[] METODOS_PERMITIDOS = { "GET", "POST", "PUT", "PATCH", "DELETE" };

    private final List<String> origenesPermitidos;

    public ConfiguracionCors(@Value("${app.cors.origenes-permitidos}") String origenesPermitidos) {
        this.origenesPermitidos = leerOrigenes(origenesPermitidos);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping(RUTAS)
                .allowedOrigins(origenesPermitidos.toArray(String[]::new))
                .allowedMethods(METODOS_PERMITIDOS)
                .allowCredentials(false);
    }

    /**
     * Separa la lista por comas y revisa cada origen. Falla al arrancar con un
     * mensaje claro ante los errores típicos, que de otro modo harían que el
     * navegador rechace las peticiones sin explicar por qué.
     */
    static List<String> leerOrigenes(String valor) {
        List<String> origenes = Arrays.stream(valor.split(","))
                .map(String::trim)
                .filter(origen -> !origen.isEmpty())
                .toList();

        if (origenes.isEmpty()) {
            throw new IllegalStateException(
                    "CORS_ORIGENES_PERMITIDOS no tiene ningún origen. Ej: CORS_ORIGENES_PERMITIDOS=http://localhost:5173");
        }
        for (String origen : origenes) {
            if (origen.equals("*")) {
                throw new IllegalStateException(
                        "CORS_ORIGENES_PERMITIDOS no admite '*': indica los orígenes del frontend uno por uno.");
            }
            if (!origen.startsWith("http://") && !origen.startsWith("https://")) {
                throw new IllegalStateException("El origen CORS '" + origen
                        + "' debe empezar por http:// o https://. Ej: http://localhost:5173");
            }
            if (origen.endsWith("/")) {
                throw new IllegalStateException("El origen CORS '" + origen
                        + "' no debe terminar en '/': el navegador envía el origen sin ella y no coincidiría.");
            }
        }
        return origenes;
    }
}
