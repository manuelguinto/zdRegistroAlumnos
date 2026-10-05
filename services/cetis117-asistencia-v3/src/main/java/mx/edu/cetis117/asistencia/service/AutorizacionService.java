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
    private final AlumnoService alumnoService;
    private final UsuarioSistemaService usuarioSistemaService;

    public AutorizacionService(
            Firestore firestore,
            AlumnoService alumnoService,
            UsuarioSistemaService usuarioSistemaService
    ) {
        this.firestore = firestore;
        this.alumnoService = alumnoService;
        this.usuarioSistemaService = usuarioSistemaService;
    }

    public Map<String, Object> crear(
            CrearAutorizacionRequest request
    ) throws Exception {

        String tipo = request.tipo().trim().toUpperCase();

        if (!tipo.equals("ALUMNO") && !tipo.equals("GRUPO")) {
            throw new IllegalArgumentException(
                    "tipo debe ser ALUMNO o GRUPO"
            );
        }

        if (usuarioSistemaService
                .buscarPorIdUsuario(request.idUsuarioAutoriza()) == null) {

            throw new IllegalArgumentException(
                    "Usuario autorizador no encontrado o inactivo"
            );
        }

        String iniciales = null;

        if (tipo.equals("ALUMNO")) {
            if (request.iniciales() == null
                    || request.iniciales().isBlank()) {

                throw new IllegalArgumentException(
                        "iniciales es obligatorio para autorización por ALUMNO"
                );
            }

            DocumentSnapshot alumno =
                    alumnoService.buscarPorIniciales(request.iniciales());

            if (alumno == null) {
                throw new IllegalArgumentException(
                        "Alumno no registrado"
                );
            }

            iniciales = alumnoService.generarIniciales(alumno);

            DocumentSnapshot autorizacionExistente =
                    buscarAutorizacionAlumnoDelDia(iniciales);

            if (autorizacionExistente != null) {
                Map<String, Object> respuesta = new HashMap<>();
                respuesta.put(
                        "resultado",
                        "AUTORIZACION_EXISTENTE"
                );
                respuesta.put(
                        "mensaje",
                        "Ya ha sido autorizada su salida."
                );
                respuesta.put(
                        "idAutorizacion",
                        autorizacionExistente.getString("idAutorizacion")
                );
                respuesta.put(
                        "iniciales",
                        iniciales
                );
                return respuesta;
            }
        }

        if (tipo.equals("GRUPO")
                && (request.idGrupo() == null
                || request.idGrupo().isBlank())) {

            throw new IllegalArgumentException(
                    "idGrupo es obligatorio para autorización por GRUPO"
            );
        }

        String id = "AUT-" + UUID
                .randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();

        Map<String, Object> data = new HashMap<>();
        data.put("idAutorizacion", id);
        data.put("tipo", tipo);
        data.put(
                "iniciales",
                tipo.equals("ALUMNO") ? iniciales : null
        );
        data.put(
                "idGrupo",
                tipo.equals("GRUPO") ? request.idGrupo() : null
        );
        data.put(
                "idUsuarioAutoriza",
                request.idUsuarioAutoriza()
        );
        data.put(
                "fechaHoraAutorizacion",
                Timestamp.now()
        );
        data.put("motivo", request.motivo());
        data.put("activa", true);
        data.put(
                "fecha",
                LocalDate.now(ZONE_ID).toString()
        );

        firestore
                .collection("autorizacionesSalida")
                .document(id)
                .set(data)
                .get();

        data.put("resultado", "AUTORIZACION_CREADA");
        data.put("mensaje", "Autorización registrada correctamente");

        return data;
    }

    public DocumentSnapshot buscarAutorizacionActiva(
            String iniciales,
            String idGrupo
    ) throws Exception {

        String hoy = LocalDate.now(ZONE_ID).toString();

        QuerySnapshot autorizaciones = firestore
                .collection("autorizacionesSalida")
                .whereEqualTo("fecha", hoy)
                .get()
                .get();

        for (DocumentSnapshot autorizacion
                : autorizaciones.getDocuments()) {

            if (!Boolean.TRUE.equals(
                    autorizacion.getBoolean("activa")
            )) {
                continue;
            }

            String tipo = autorizacion.getString("tipo");

            if ("ALUMNO".equals(tipo)
                    && iniciales.equals(
                    autorizacion.getString("iniciales")
            )) {
                return autorizacion;
            }
        }

        for (DocumentSnapshot autorizacion
                : autorizaciones.getDocuments()) {

            if (!Boolean.TRUE.equals(
                    autorizacion.getBoolean("activa")
            )) {
                continue;
            }

            String tipo = autorizacion.getString("tipo");

            if ("GRUPO".equals(tipo)
                    && idGrupo.equals(
                    String.valueOf(autorizacion.get("idGrupo"))
            )) {
                return autorizacion;
            }
        }

        return null;
    }

    private DocumentSnapshot buscarAutorizacionAlumnoDelDia(
            String iniciales
    ) throws Exception {

        String hoy = LocalDate.now(ZONE_ID).toString();

        QuerySnapshot autorizaciones = firestore
                .collection("autorizacionesSalida")
                .whereEqualTo("fecha", hoy)
                .get()
                .get();

        for (DocumentSnapshot autorizacion
                : autorizaciones.getDocuments()) {

            if (!Boolean.TRUE.equals(
                    autorizacion.getBoolean("activa")
            )) {
                continue;
            }

            if (!"ALUMNO".equals(
                    autorizacion.getString("tipo")
            )) {
                continue;
            }

            if (iniciales.equals(
                    autorizacion.getString("iniciales")
            )) {
                return autorizacion;
            }
        }

        return null;
    }
}
