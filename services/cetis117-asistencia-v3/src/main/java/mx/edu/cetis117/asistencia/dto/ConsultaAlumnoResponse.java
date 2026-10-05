package mx.edu.cetis117.asistencia.dto;

import java.util.List;
import java.util.Map;

public record ConsultaAlumnoResponse(
        String idAlumno,
        String nombreCompleto,
        String idGrupo,
        Map<String, Object> tutor,
        List<ConsultaRegistroDto> registros
) {
}
