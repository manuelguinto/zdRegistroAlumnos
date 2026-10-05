package mx.edu.cetis117.asistencia.dto;

import jakarta.validation.constraints.NotNull;

public record RegistroHuellaRequest(
        @NotNull Long fingerprintId
) {
}
