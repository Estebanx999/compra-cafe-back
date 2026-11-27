package com.compracafe.shared.domain.exception;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Base de todas las excepciones de negocio.
 *
 * Cada error tiene:
 * - un {@code codigo} fijo en MAYÚSCULAS_CON_GUIONES_BAJOS (ej: "CAJA_SIN_SALDO"),
 *   estable en el tiempo, para que el frontend pueda reaccionar a él;
 * - un mensaje en español pensado para mostrarse al usuario.
 *
 * Java puro: el dominio no sabe de HTTP. La traducción a un estado HTTP la hace
 * la infraestructura web según el tipo de excepción.
 */
public abstract class ExcepcionNegocio extends RuntimeException {

    private static final Pattern FORMATO_CODIGO = Pattern.compile("[A-Z][A-Z0-9]*(_[A-Z0-9]+)*");

    private final String codigo;

    protected ExcepcionNegocio(String codigo, String mensaje) {
        super(validarMensaje(mensaje));
        this.codigo = validarCodigo(codigo);
    }

    protected ExcepcionNegocio(String codigo, String mensaje, Throwable causa) {
        super(validarMensaje(mensaje), causa);
        this.codigo = validarCodigo(codigo);
    }

    public String getCodigo() {
        return codigo;
    }

    private static String validarCodigo(String codigo) {
        Objects.requireNonNull(codigo, "El código de la excepción de negocio es obligatorio");
        if (!FORMATO_CODIGO.matcher(codigo).matches()) {
            throw new IllegalArgumentException(
                    "El código '" + codigo + "' debe estar en MAYÚSCULAS_CON_GUIONES_BAJOS, ej: CAJA_SIN_SALDO");
        }
        return codigo;
    }

    private static String validarMensaje(String mensaje) {
        Objects.requireNonNull(mensaje, "El mensaje de la excepción de negocio es obligatorio");
        if (mensaje.isBlank()) {
            throw new IllegalArgumentException("El mensaje de la excepción de negocio no puede estar vacío");
        }
        return mensaje;
    }
}
