package mx.edu.cetis117.asistencia.service;

import java.util.List;
import java.util.Map;

public class ValidacionImportacionException extends IllegalArgumentException {
    private final List<Map<String, Object>> errores;

    public ValidacionImportacionException(List<Map<String, Object>> errores) {
        super("Importacion cancelada: corrija todos los errores del CSV");
        this.errores = List.copyOf(errores);
    }

    public List<Map<String, Object>> getErrores() { return errores; }
}
