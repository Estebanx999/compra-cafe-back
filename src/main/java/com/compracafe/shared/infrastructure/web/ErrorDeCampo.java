package com.compracafe.shared.infrastructure.web;

/**
 * Un campo que no pasó la validación, tal como se devuelve en la propiedad
 * {@code errores} de la respuesta 400.
 *
 * No incluye el valor rechazado a propósito: podría ser un dato sensible.
 */
public record ErrorDeCampo(String campo, String mensaje) {
}
