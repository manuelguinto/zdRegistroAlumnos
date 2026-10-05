package mx.edu.cetis117.asistencia.service;

import com.google.cloud.firestore.DocumentSnapshot;
import mx.edu.cetis117.asistencia.dto.AccesoResponse;
import org.springframework.stereotype.Service;

@Service
public class AccesoService {

    private final UsuarioSistemaService usuarioSistemaService;
    private final AlumnoService alumnoService;

    public AccesoService(
            UsuarioSistemaService usuarioSistemaService,
            AlumnoService alumnoService
    ) {
        this.usuarioSistemaService = usuarioSistemaService;
        this.alumnoService = alumnoService;
    }

    public AccesoResponse validar(
            String codigo
    ) throws Exception {

        if (codigo == null || codigo.isBlank()) {
            return new AccesoResponse(
                    false,
                    null,
                    "Código de seguridad requerido",
                    null,
                    null,
                    null,
                    null
            );
        }

        DocumentSnapshot usuario =
                usuarioSistemaService.buscarPorCodigo(codigo);

        if (usuario != null) {
            return new AccesoResponse(
                    true,
                    "SISTEMA",
                    "Acceso autorizado",
                    usuario.getString("idUsuario"),
                    usuario.getString("nombre"),
                    usuario.getString("rol"),
                    null
            );
        }

        DocumentSnapshot alumno =
                alumnoService.buscarPorCodigoTutor(codigo);

        if (alumno != null) {
            return new AccesoResponse(
                    true,
                    "TUTOR",
                    "Código válido",
                    null,
                    null,
                    null,
                    alumno.getString("idAlumno")
            );
        }

        return new AccesoResponse(
                false,
                null,
                "Código de seguridad inválido",
                null,
                null,
                null,
                null
        );
    }
}
