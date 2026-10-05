package mx.edu.cetis117.asistencia.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import mx.edu.cetis117.asistencia.dto.CrearAutorizacionRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AutorizacionService {

    private static final ZoneId ZONE_ID =
            ZoneId.of("America/Mexico_City");

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

        String tipo =
                request.tipo().trim().toUpperCase();

        if (!tipo.equals("ALUMNO")
                && !tipo.equals("GRUPO")
                && !tipo.equals("GENERAL")) {

            throw new IllegalArgumentException(
                    "tipo debe ser ALUMNO, GRUPO o GENERAL"
            );
        }

        if (usuarioSistemaService
                .buscarPorIdUsuario(
                        request.idUsuarioAutoriza()
                ) == null) {

            throw new IllegalArgumentException(
                    "Usuario autorizador no encontrado o inactivo"
            );
        }

        String hoy =
                LocalDate.now(ZONE_ID).toString();

        /*
         * Si ya existe una salida GENERAL activa para el día,
         * no tiene sentido crear autorizaciones adicionales
         * por alumno o por grupo.
         */
        if (tipo.equals("ALUMNO")
                || tipo.equals("GRUPO")) {

            DocumentSnapshot salidaGeneralExistente =
                    buscarAutorizacionGeneralDelDia();

            if (salidaGeneralExistente != null) {

                Map<String, Object> respuesta =
                        new HashMap<>();

                respuesta.put(
                        "resultado",
                        "SALIDA_GENERAL_YA_AUTORIZADA"
                );

                respuesta.put(
                        "mensaje",
                        "Ya existe una salida general autorizada para hoy"
                );

                respuesta.put(
                        "idAutorizacion",
                        salidaGeneralExistente.getString(
                                "idAutorizacion"
                        )
                );

                respuesta.put(
                        "horaSalidaGeneral",
                        salidaGeneralExistente.getString(
                                "horaSalidaGeneral"
                        )
                );

                return respuesta;
            }
        }

        String iniciales = null;
        String horaSalidaGeneral = null;

        if (tipo.equals("ALUMNO")) {

            if (request.iniciales() == null
                    || request.iniciales().isBlank()) {

                throw new IllegalArgumentException(
                        "iniciales es obligatorio para autorización por ALUMNO"
                );
            }

            DocumentSnapshot alumno =
                    alumnoService.buscarPorIniciales(
                            request.iniciales()
                    );

            if (alumno == null) {
                throw new IllegalArgumentException(
                        "Alumno no registrado"
                );
            }

            iniciales =
                    alumnoService.obtenerIniciales(
                            alumno
                    );

            DocumentSnapshot autorizacionExistente =
                    buscarAutorizacionAlumnoDelDia(
                            iniciales
                    );

            if (autorizacionExistente != null) {
                Map<String, Object> respuesta =
                        new HashMap<>();

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
                        autorizacionExistente.getString(
                                "idAutorizacion"
                        )
                );
                respuesta.put(
                        "iniciales",
                        iniciales
                );

                return respuesta;
            }
        }

        if (tipo.equals("GRUPO")) {

            if (request.idGrupo() == null
                    || request.idGrupo().isBlank()) {

                throw new IllegalArgumentException(
                        "idGrupo es obligatorio para autorización por GRUPO"
                );
            }

            QuerySnapshot autorizacionesGrupo = firestore
                    .collection("autorizacionesSalida")
                    .whereEqualTo("fecha", hoy)
                    .get()
                    .get();

            for (DocumentSnapshot autorizacion
                    : autorizacionesGrupo.getDocuments()) {

                if (!Boolean.TRUE.equals(
                        autorizacion.getBoolean("activa")
                )) {
                    continue;
                }

                if (!"GRUPO".equals(
                        autorizacion.getString("tipo")
                )) {
                    continue;
                }

                if (request.idGrupo().equals(
                        String.valueOf(
                                autorizacion.get("idGrupo")
                        )
                )) {
                    Map<String, Object> respuesta =
                            new HashMap<>();

                    respuesta.put(
                            "resultado",
                            "AUTORIZACION_GRUPO_EXISTENTE"
                    );
                    respuesta.put(
                            "mensaje",
                            "Grupo ya fue autorizado previamente"
                    );

                    return respuesta;
                }
            }
        }

        if (tipo.equals("GENERAL")) {

            if (request.horaSalidaGeneral() == null
                    || request.horaSalidaGeneral().isBlank()) {

                throw new IllegalArgumentException(
                        "horaSalidaGeneral es obligatorio para autorización GENERAL"
                );
            }

            try {
                LocalTime.parse(
                        request.horaSalidaGeneral().trim()
                );
            } catch (Exception exception) {
                throw new IllegalArgumentException(
                        "horaSalidaGeneral debe tener formato HH:mm"
                );
            }

            DocumentSnapshot generalExistente =
                    buscarAutorizacionGeneralDelDia();

            if (generalExistente != null) {
                Map<String, Object> respuesta =
                        new HashMap<>();

                respuesta.put(
                        "resultado",
                        "AUTORIZACION_GENERAL_EXISTENTE"
                );
                respuesta.put(
                        "mensaje",
                        "La salida general ya fue autorizada previamente"
                );
                respuesta.put(
                        "idAutorizacion",
                        generalExistente.getString(
                                "idAutorizacion"
                        )
                );
                respuesta.put(
                        "horaSalidaGeneral",
                        generalExistente.getString(
                                "horaSalidaGeneral"
                        )
                );

                return respuesta;
            }

            horaSalidaGeneral =
                    request.horaSalidaGeneral().trim();
        }

        String id =
                "AUT-"
                        + UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "idAutorizacion",
                id
        );
        data.put(
                "tipo",
                tipo
        );
        data.put(
                "iniciales",
                tipo.equals("ALUMNO")
                        ? iniciales
                        : null
        );
        data.put(
                "idGrupo",
                tipo.equals("GRUPO")
                        ? request.idGrupo()
                        : null
        );
        data.put(
                "horaSalidaGeneral",
                tipo.equals("GENERAL")
                        ? horaSalidaGeneral
                        : null
        );
        data.put(
                "idUsuarioAutoriza",
                request.idUsuarioAutoriza()
        );
        data.put(
                "fechaHoraAutorizacion",
                Timestamp.now()
        );
        data.put(
                "motivo",
                request.motivo()
        );
        data.put(
                "activa",
                true
        );
        data.put(
                "fecha",
                hoy
        );

        firestore
                .collection("autorizacionesSalida")
                .document(id)
                .set(data)
                .get();

        data.put(
                "resultado",
                "AUTORIZACION_CREADA"
        );

        data.put(
                "mensaje",
                tipo.equals("GENERAL")
                        ? "Salida general autorizada correctamente"
                        : "Autorización registrada correctamente"
        );

        return data;
    }

    public DocumentSnapshot buscarAutorizacionActiva(
            String iniciales,
            String idGrupo
    ) throws Exception {

        String hoy =
                LocalDate.now(ZONE_ID).toString();

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

            if ("ALUMNO".equals(
                    autorizacion.getString("tipo")
            )
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

            if ("GRUPO".equals(
                    autorizacion.getString("tipo")
            )
                    && idGrupo.equals(
                    String.valueOf(
                            autorizacion.get("idGrupo")
                    )
            )) {

                return autorizacion;
            }
        }

        return null;
    }

    public DocumentSnapshot buscarAutorizacionGeneralDelDia()
            throws Exception {

        String hoy =
                LocalDate.now(ZONE_ID).toString();

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

            if ("GENERAL".equals(
                    autorizacion.getString("tipo")
            )) {
                return autorizacion;
            }
        }

        return null;
    }

    private DocumentSnapshot buscarAutorizacionAlumnoDelDia(
            String iniciales
    ) throws Exception {

        String hoy =
                LocalDate.now(ZONE_ID).toString();

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
