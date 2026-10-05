package mx.edu.cetis117.asistencia.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

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
