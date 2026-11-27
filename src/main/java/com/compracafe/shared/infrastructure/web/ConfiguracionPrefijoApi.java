package com.compracafe.shared.infrastructure.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Antepone {@value #PREFIJO} a todas las rutas de los {@code @RestController}.
 *
 * Un controlador declara {@code @RequestMapping("/clientes")} y queda en
 * {@code /api/clientes}, sin escribir el prefijo. Así ninguna ruta de la API
 * puede quedar fuera de /api (y fuera de CORS) por olvido.
 */
@Configuration
public class ConfiguracionPrefijoApi implements WebMvcConfigurer {

    /** Prefijo de toda la API. Única fuente: también lo usa {@link ConfiguracionCors}. */
    public static final String PREFIJO = "/api";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(PREFIJO, HandlerTypePredicate.forAnnotation(RestController.class));
    }
}
