package com.compracafe.shared.domain.exception;

/**
 * La operación es válida en forma, pero el estado actual del negocio no la permite
 * (ej: la caja no tiene saldo suficiente para pagar una compra).
 *
 * Abstracta: cada error es su propia clase en el {@code domain/exception} de su módulo,
 * con el código escrito una sola vez dentro de ella. Ejemplo:
 *
 * <pre>{@code
 * public class CajaSinSaldoException extends ReglaDeNegocioException {
 *     public CajaSinSaldoException() {
 *         super("CAJA_SIN_SALDO", "La caja no tiene saldo suficiente para esta operación");
 *     }
 * }
 *
 * throw new CajaSinSaldoException();
 * }</pre>
 */
public abstract non-sealed class ReglaDeNegocioException extends ExcepcionNegocio {

    protected ReglaDeNegocioException(String codigo, String mensaje) {
        super(codigo, mensaje);
    }

    protected ReglaDeNegocioException(String codigo, String mensaje, Throwable causa) {
        super(codigo, mensaje, causa);
    }
}
