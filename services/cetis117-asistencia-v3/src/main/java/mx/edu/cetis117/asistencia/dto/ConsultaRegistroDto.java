package mx.edu.cetis117.asistencia.dto;

public record ConsultaRegistroDto(
        String fecha,
        String horaEntrada,
        String horaSalida,
        String estado,
        String nombreAutoriza,
        String motivo
) {
}
