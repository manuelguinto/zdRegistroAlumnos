package mx.edu.cetis117.asistencia.dto;

public record AsistenciaResponse(
        String resultado,
        String mensaje,
        String estado,
        String idRegistro,
        String idAutorizacion,
        String nombreAlumno
) {
}
