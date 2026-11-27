package com.compracafe.shared.infrastructure.web;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.compracafe.shared.domain.exception.ExcepcionNegocio;
import com.compracafe.shared.domain.exception.RecursoNoEncontradoException;
import com.compracafe.shared.domain.exception.ReglaDeNegocioException;

/**
 * Traduce toda excepción que salga de un controlador a una respuesta JSON
 * (ProblemDetail, RFC 9457) con un mensaje en español y la propiedad {@code codigo}.
 *
 * <ul>
 *   <li>{@link RecursoNoEncontradoException} → 404</li>
 *   <li>{@link ReglaDeNegocioException} → 409</li>
 *   <li>Validación ({@code @Valid}) → 400, con la lista de campos en {@code errores}</li>
 *   <li>Errores propios de Spring MVC (JSON mal formado, método no permitido, ruta
 *       inexistente...) → su estado HTTP correspondiente, con mensaje en español</li>
 *   <li>Cualquier otra → 500 con mensaje genérico; el detalle va solo al log</li>
 * </ul>
 *
 * La respuesta nunca incluye el stacktrace ni el mensaje interno de la excepción,
 * salvo el de las excepciones de negocio, que está escrito para el usuario.
 */
@RestControllerAdvice
public class ManejadorGlobalErrores extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalErrores.class);

    static final String PROPIEDAD_CODIGO = "codigo";
    static final String PROPIEDAD_ERRORES = "errores";
    static final String PROPIEDAD_REFERENCIA = "referencia";

    static final String CODIGO_VALIDACION = "VALIDACION_FALLIDA";
    static final String CODIGO_ERROR_INTERNO = "ERROR_INTERNO";

    // ─── Excepciones de negocio ─────────────────────────────────────────────

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail manejarRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        return problemaDeNegocio(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex);
    }

    @ExceptionHandler(ReglaDeNegocioException.class)
    public ProblemDetail manejarReglaDeNegocio(ReglaDeNegocioException ex) {
        return problemaDeNegocio(HttpStatus.CONFLICT, "Regla de negocio incumplida", ex);
    }

    private ProblemDetail problemaDeNegocio(HttpStatus estado, String titulo, ExcepcionNegocio ex) {
        log.debug("Excepción de negocio {}: {}", ex.getCodigo(), ex.getMessage());
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, ex.getMessage());
        problema.setTitle(titulo);
        problema.setProperty(PROPIEDAD_CODIGO, ex.getCodigo());
        return problema;
    }

    // ─── Validación (@Valid) ────────────────────────────────────────────────

    /** {@code @Valid} sobre un {@code @RequestBody} u objeto de formulario. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return respuestaDeValidacion(erroresDe(ex.getBindingResult()), headers);
    }

    /** Validación de parámetros del método (@RequestParam, @PathVariable, o @Valid combinado con ellos). */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ErrorDeCampo> errores = new ArrayList<>();
        for (ParameterValidationResult resultado : ex.getParameterValidationResults()) {
            if (resultado instanceof ParameterErrors erroresDeObjeto) {
                errores.addAll(erroresDe(erroresDeObjeto));
            } else {
                String parametro = resultado.getMethodParameter().getParameterName();
                for (MessageSourceResolvable error : resultado.getResolvableErrors()) {
                    errores.add(new ErrorDeCampo(parametro, error.getDefaultMessage()));
                }
            }
        }
        for (MessageSourceResolvable error : ex.getCrossParameterValidationResults()) {
            errores.add(new ErrorDeCampo(null, error.getDefaultMessage()));
        }
        return respuestaDeValidacion(errores, headers);
    }

    private static List<ErrorDeCampo> erroresDe(Errors resultado) {
        List<ErrorDeCampo> errores = new ArrayList<>();
        for (FieldError error : resultado.getFieldErrors()) {
            errores.add(new ErrorDeCampo(error.getField(), error.getDefaultMessage()));
        }
        resultado.getGlobalErrors()
                .forEach(error -> errores.add(new ErrorDeCampo(null, error.getDefaultMessage())));
        return errores;
    }

    private ResponseEntity<Object> respuestaDeValidacion(List<ErrorDeCampo> errores, HttpHeaders headers) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Hay datos inválidos en la solicitud. Revisa los campos indicados.");
        problema.setTitle("Datos inválidos");
        problema.setProperty(PROPIEDAD_CODIGO, CODIGO_VALIDACION);
        problema.setProperty(PROPIEDAD_ERRORES, errores);
        return ResponseEntity.badRequest().headers(headers).body(problema);
    }

    // ─── Errores propios de Spring MVC ──────────────────────────────────────

    /**
     * Punto común por el que pasan las demás excepciones de Spring MVC que maneja
     * {@link ResponseEntityExceptionHandler}. Se conserva su estado HTTP, pero se
     * reemplaza el cuerpo (en inglés y con detalles internos) por uno en español.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        ErrorDeSolicitud error = ErrorDeSolicitud.para(statusCode);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(statusCode, error.mensaje());
        problema.setTitle(error.titulo());
        problema.setProperty(PROPIEDAD_CODIGO, error.codigo());

        if (statusCode.is5xxServerError()) {
            String referencia = nuevaReferencia();
            log.error("Error {} en la solicitud [referencia={}]", statusCode.value(), referencia, ex);
            problema.setProperty(PROPIEDAD_REFERENCIA, referencia);
        } else {
            log.debug("Solicitud rechazada con {}: {}", statusCode.value(), ex.getMessage());
        }
        return super.handleExceptionInternal(ex, problema, headers, statusCode, request);
    }

    private record ErrorDeSolicitud(String codigo, String titulo, String mensaje) {

        static ErrorDeSolicitud para(HttpStatusCode estado) {
            return switch (estado.value()) {
                case 400 -> new ErrorDeSolicitud("SOLICITUD_INVALIDA", "Solicitud inválida",
                        "La solicitud no es válida o está mal formada.");
                case 404 -> new ErrorDeSolicitud("RUTA_NO_ENCONTRADA", "Ruta no encontrada",
                        "La ruta solicitada no existe.");
                case 405 -> new ErrorDeSolicitud("METODO_NO_PERMITIDO", "Método no permitido",
                        "El método HTTP no está permitido para esta ruta.");
                case 406 -> new ErrorDeSolicitud("FORMATO_NO_ACEPTABLE", "Formato no aceptable",
                        "No se puede responder en el formato solicitado.");
                case 413 -> new ErrorDeSolicitud("SOLICITUD_DEMASIADO_GRANDE", "Solicitud demasiado grande",
                        "La solicitud supera el tamaño permitido.");
                case 415 -> new ErrorDeSolicitud("TIPO_CONTENIDO_NO_SOPORTADO", "Tipo de contenido no soportado",
                        "El tipo de contenido enviado no está soportado.");
                default -> estado.is5xxServerError()
                        ? errorInterno()
                        : new ErrorDeSolicitud("SOLICITUD_INVALIDA", "Solicitud inválida",
                                "No se pudo procesar la solicitud.");
            };
        }

        static ErrorDeSolicitud errorInterno() {
            return new ErrorDeSolicitud(CODIGO_ERROR_INTERNO, "Error interno",
                    "Ocurrió un error inesperado. Intenta de nuevo y, si el problema continúa, "
                            + "comunícate con el administrador indicando la referencia.");
        }
    }

    // ─── Cualquier otra excepción ───────────────────────────────────────────

    /**
     * Último recurso: errores no previstos (bugs, base de datos caída...).
     * El detalle completo va solo al log, junto con una referencia que también se
     * devuelve al cliente para poder encontrar el error en el log.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail manejarErrorInesperado(Exception ex) {
        String referencia = nuevaReferencia();
        log.error("Error inesperado [referencia={}]", referencia, ex);

        ErrorDeSolicitud error = ErrorDeSolicitud.errorInterno();
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, error.mensaje());
        problema.setTitle(error.titulo());
        problema.setProperty(PROPIEDAD_CODIGO, error.codigo());
        problema.setProperty(PROPIEDAD_REFERENCIA, referencia);
        return problema;
    }

    private static String nuevaReferencia() {
        return UUID.randomUUID().toString();
    }
}
