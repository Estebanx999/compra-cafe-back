package com.compracafe.shared.domain.exception;

/**
 * Se buscó algo que no existe (un cliente, un lote, una compra...).
 *
 * Abstracta: cada error es su propia clase en el {@code domain/exception} de su módulo,
 * con el código escrito una sola vez dentro de ella. Ejemplo:
 *
 * <pre>{@code
 * public class ClienteNoEncontradoException extends RecursoNoEncontradoException {
 *     public ClienteNoEncontradoException(long id) {
 *         super("CLIENTE_NO_ENCONTRADO", "No existe el cliente con id " + id);
 *     }
 * }
 *
 * throw new ClienteNoEncontradoException(15);
 * }</pre>
 */
public abstract non-sealed class RecursoNoEncontradoException extends ExcepcionNegocio {

    protected RecursoNoEncontradoException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }

    protected RecursoNoEncontradoException(String codigo, String mensaje, Throwable causa) {
        super(codigo, mensaje, causa);
    }
}
