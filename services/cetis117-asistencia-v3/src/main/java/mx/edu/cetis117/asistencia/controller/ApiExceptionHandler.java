package mx.edu.cetis117.asistencia.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(mx.edu.cetis117.asistencia.service.ValidacionImportacionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleImportacion(
            mx.edu.cetis117.asistencia.service.ValidacionImportacionException exception) {
        return Map.of("error", exception.getMessage(), "registrosImportados", 0,
                "insertados", 0, "actualizados", 0, "totalErrores", exception.getErrores().size(),
                "errores", exception.getErrores());
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<Map<String, Object>> handleStatus(
            org.springframework.web.server.ResponseStatusException exception) {
        return org.springframework.http.ResponseEntity.status(exception.getStatusCode())
                .body(Map.of("error", exception.getReason() == null ? "Peticion rechazada" : exception.getReason()));
    }

    @ExceptionHandler(org.springframework.web.bind.ServletRequestBindingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBinding(Exception exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBadRequest(
            IllegalArgumentException exception
    ) {
        return Map.of(
                "error",
                exception.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleInternalError(
            Exception exception
    ) {
        String detalle = exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();

        return Map.of(
                "error", "Error interno",
                "detalle", detalle
        );
    }
}
