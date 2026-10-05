package mx.edu.cetis117.asistencia.dto;

public record GrupoResponse(
        String idGrupo,
        String nombre,
        String horaSalida,
        Boolean activo,
        String estadoSalida,
        Boolean salidaHabilitada,
        String horaSalidaEfectiva
) {
}
