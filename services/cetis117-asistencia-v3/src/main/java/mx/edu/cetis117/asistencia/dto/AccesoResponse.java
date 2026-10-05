package mx.edu.cetis117.asistencia.dto;

public record AccesoResponse(
        boolean valido,
        String tipoAcceso,
        String mensaje,
        String idUsuario,
        String nombreUsuario,
        String rol,
        String idAlumno
) {
}
