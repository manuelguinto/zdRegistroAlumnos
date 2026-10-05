package mx.edu.cetis117.asistencia.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import mx.edu.cetis117.asistencia.dto.AsistenciaResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Service
public class AsistenciaService {

    private static final ZoneId ZONE_ID = ZoneId.of("America/Mexico_City");

    private final Firestore firestore;
    private final AlumnoService alumnoService;
    private final AutorizacionService autorizacionService;
    private final SystemParamsService systemParamsService;

    public AsistenciaService(
            Firestore firestore,
            AlumnoService alumnoService,
            AutorizacionService autorizacionService,
            SystemParamsService systemParamsService
    ) {
        this.firestore = firestore;
        this.alumnoService = alumnoService;
        this.autorizacionService = autorizacionService;
        this.systemParamsService = systemParamsService;
    }

    public AsistenciaResponse registrar(
            Long fingerprintId
    ) throws Exception {

        DocumentSnapshot alumno =
                alumnoService.buscarPorFingerprint(fingerprintId);

        if (alumno == null) {
            return new AsistenciaResponse(
                    "ALUMNO_NO_REGISTRADO",
                    "Alumno no registrado",
                    null,
                    null,
                    null,
                    null
            );
        }

        String nombreAlumno =
                alumnoService.obtenerNombreCompleto(alumno);

        String inicialesAlumno =
                alumnoService.generarIniciales(alumno);

        String idGrupo =
                String.valueOf(alumno.get("idGrupo"));

        String idRegistro =
                fingerprintId + "_" + LocalDate.now(ZONE_ID);

        DocumentReference registroRef = firestore
                .collection("registro")
                .document(idRegistro);

        DocumentSnapshot registro =
                registroRef.get().get();

        if (!registro.exists()) {
            Map<String, Object> data = new HashMap<>();
            data.put("fingerprintId", fingerprintId);
            data.put("fechaHoraEntrada", Timestamp.now());
            data.put("fechaHoraSalida", null);
            data.put("estado", "EntradaRegistrada");
            data.put("idAutorizacion", null);

            registroRef.set(data).get();

            return new AsistenciaResponse(
                    "ENTRADA_REGISTRADA",
                    "Registro de entrada exitoso, bienvenido",
                    "EntradaRegistrada",
                    idRegistro,
                    null,
                    nombreAlumno
            );
        }

        if (registro.get("fechaHoraSalida") != null
                || "Completado".equals(registro.getString("estado"))) {

            return new AsistenciaResponse(
                    "REGISTRO_COMPLETADO",
                    "Debe esperar al día de mañana para poder registrar su entrada nuevamente",
                    "Completado",
                    idRegistro,
                    registro.getString("idAutorizacion"),
                    nombreAlumno
            );
        }

        boolean requiereAutorizacion =
                systemParamsService.getRequiereAutorizacionSalida();

        if (!requiereAutorizacion) {
            registrarSalida(
                    registroRef,
                    null
            );

            return new AsistenciaResponse(
                    "SALIDA_REGISTRADA",
                    "Registro de salida exitosa, buen regreso a casa",
                    "Completado",
                    idRegistro,
                    null,
                    nombreAlumno
            );
        }

        DocumentSnapshot grupo =
                buscarGrupo(idGrupo);

        if (grupo == null) {
            return new AsistenciaResponse(
                    "GRUPO_NO_ENCONTRADO",
                    "No se encontró el grupo del alumno",
                    "EntradaRegistrada",
                    idRegistro,
                    null,
                    nombreAlumno
            );
        }

        String horaSalidaTexto =
                grupo.getString("horaSalida");

        if (horaSalidaTexto == null
                || horaSalidaTexto.isBlank()) {

            return new AsistenciaResponse(
                    "HORARIO_NO_CONFIGURADO",
                    "El grupo no tiene hora de salida configurada",
                    "EntradaRegistrada",
                    idRegistro,
                    null,
                    nombreAlumno
            );
        }

        int minutosTolerancia =
                systemParamsService.getMinutosToleranciaSalida();

        LocalTime horaSalida =
                LocalTime.parse(horaSalidaTexto);

        LocalTime horaPermitidaSalida =
                horaSalida.minusMinutes(minutosTolerancia);

        LocalTime horaActual =
                LocalTime.now(ZONE_ID);

        if (!horaActual.isBefore(horaPermitidaSalida)) {
            registrarSalida(
                    registroRef,
                    null
            );

            return new AsistenciaResponse(
                    "SALIDA_REGISTRADA",
                    "Registro de salida exitosa, buen regreso a casa",
                    "Completado",
                    idRegistro,
                    null,
                    nombreAlumno
            );
        }

        DocumentSnapshot autorizacion =
                autorizacionService.buscarAutorizacionActiva(
                        inicialesAlumno,
                        idGrupo
                );

        if (autorizacion == null) {
            return new AsistenciaResponse(
                    "REQUIERE_AUTORIZACION",
                    "Requiere autorización de salida",
                    "EntradaRegistrada",
                    idRegistro,
                    null,
                    nombreAlumno
            );
        }

        String idAutorizacion =
                autorizacion.getString("idAutorizacion");

        registrarSalida(
                registroRef,
                idAutorizacion
        );

        return new AsistenciaResponse(
                "SALIDA_AUTORIZADA",
                "Registro de salida exitosa, buen regreso a casa",
                "Completado",
                idRegistro,
                idAutorizacion,
                nombreAlumno
        );
    }

    private void registrarSalida(
            DocumentReference registroRef,
            String idAutorizacion
    ) throws Exception {

        Map<String, Object> update = new HashMap<>();
        update.put("fechaHoraSalida", Timestamp.now());
        update.put("estado", "Completado");
        update.put("idAutorizacion", idAutorizacion);

        registroRef.update(update).get();
    }

    private DocumentSnapshot buscarGrupo(
            String idGrupo
    ) throws Exception {

        QuerySnapshot query = firestore
                .collection("grupo")
                .whereEqualTo("idGrupo", idGrupo)
                .whereEqualTo("activo", true)
                .limit(1)
                .get()
                .get();

        if (!query.isEmpty()) {
            return query.getDocuments().get(0);
        }

        try {
            Long idGrupoNumerico =
                    Long.parseLong(idGrupo);

            query = firestore
                    .collection("grupo")
                    .whereEqualTo("idGrupo", idGrupoNumerico)
                    .whereEqualTo("activo", true)
                    .limit(1)
                    .get()
                    .get();

            return query.isEmpty()
                    ? null
                    : query.getDocuments().get(0);

        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
