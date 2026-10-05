package mx.edu.cetis117.asistencia.dto;

import jakarta.validation.constraints.NotBlank;

public record CrearAutorizacionRequest(
        @NotBlank String tipo,
        String iniciales,
        String idGrupo,
        @NotBlank String idUsuarioAutoriza,
        @NotBlank String motivo
) {
}
