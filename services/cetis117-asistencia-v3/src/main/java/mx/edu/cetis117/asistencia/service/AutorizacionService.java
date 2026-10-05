package mx.edu.cetis117.asistencia.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import mx.edu.cetis117.asistencia.dto.CrearAutorizacionRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AutorizacionService {

    private static final ZoneId ZONE_ID = ZoneId.of("America/Mexico_City");

    private final Firestore firestore;
    private final AlumnoService alumnos;
    private final UsuarioSistemaService usuarios;

    public AutorizacionService(
            Firestore firestore,
            AlumnoService alumnos,
            UsuarioSistemaService usuarios
    ) {
        this.firestore = firestore;
        this.alumnos = alumnos;
        this.usuarios = usuarios;
    }

    public Map<String, Object> crear(CrearAutorizacionRequest request) throws Exception {

        String tipo = request.tipo().trim().toUpperCase();

        if (!tipo.equals("ALUMNO") && !tipo.equals("GRUPO")) {
            throw new IllegalArgumentException("tipo debe ser ALUMNO o GRUPO");
        }

        if (usuarios.buscarPorIdUsuario(request.idUsuarioAutoriza()) == null) {
            throw new IllegalArgumentException("Usuario autorizador no encontrado o inactivo");
        }

        if (tipo.equals("ALUMNO")) {
            if (request.fingerprintId() == null) {
                throw new IllegalArgumentException(
                        "fingerprintId es obligatorio para autorización por ALUMNO"
                );
            }

            if (alumnos.buscarPorFingerprint(request.fingerprintId()) == null) {
                throw new IllegalArgumentException("Alumno no encontrado");
            }
        }

        if (tipo.equals("GRUPO")
                && (request.idGrupo() == null || request.idGrupo().isBlank())) {
            throw new IllegalArgumentException(
                    "idGrupo es obligatorio para autorización por GRUPO"
            );
        }

        String id = "AUT-" +
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();

        Map<String, Object> data = new HashMap<>();
        data.put("idAutorizacion", id);
        data.put("tipo", tipo);
        data.put(
                "fingerprintId",
                tipo.equals("ALUMNO") ? request.fingerprintId() : null
        );
        data.put(
                "idGrupo",
                tipo.equals("GRUPO") ? request.idGrupo() : null
        );
        data.put("idUsuarioAutoriza", request.idUsuarioAutoriza());
        data.put("fechaHoraAutorizacion", Timestamp.now());
        data.put("motivo", request.motivo());
        data.put("activa", true);
        data.put("fecha", LocalDate.now(ZONE_ID).toString());

        firestore
                .collection("autorizacionesSalida")
                .document(id)
                .set(data)
                .get();

        return data;
    }

    public DocumentSnapshot buscarAutorizacionActiva(
            Long fingerprintId,
            String idGrupo
    ) throws Exception {

        String hoy = LocalDate.now(ZONE_ID).toString();

        QuerySnapshot porAlumno = firestore
                .collection("autorizacionesSalida")
                .whereEqualTo("activa", true)
                .whereEqualTo("tipo", "ALUMNO")
                .whereEqualTo("fingerprintId", fingerprintId)
                .whereEqualTo("fecha", hoy)
                .limit(1)
                .get()
                .get();

        if (!porAlumno.isEmpty()) {
            return porAlumno.getDocuments().get(0);
        }

        QuerySnapshot porGrupo = firestore
                .collection("autorizacionesSalida")
                .whereEqualTo("activa", true)
                .whereEqualTo("tipo", "GRUPO")
                .whereEqualTo("idGrupo", idGrupo)
                .whereEqualTo("fecha", hoy)
                .limit(1)
                .get()
                .get();

        return porGrupo.isEmpty()
                ? null
                : porGrupo.getDocuments().get(0);
    }
}
