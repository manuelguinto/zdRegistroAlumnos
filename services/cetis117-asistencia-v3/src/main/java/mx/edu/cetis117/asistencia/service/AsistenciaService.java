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
    private final AlumnoService alumnos;
    private final AutorizacionService autorizacionService;
    private final SystemParamsService paramsService;

    public AsistenciaService(
            Firestore firestore,
            AlumnoService alumnos,
            AutorizacionService autorizacionService,
            SystemParamsService paramsService
    ) {
        this.firestore = firestore;
        this.alumnos = alumnos;
        this.autorizacionService = autorizacionService;
        this.paramsService = paramsService;
    }

    public AsistenciaResponse registrar(Long fingerprintId) throws Exception {

        DocumentSnapshot alumno = alumnos.buscarPorFingerprint(fingerprintId);

        if (alumno == null) {
            return new AsistenciaResponse(
                    "HUELLA_NO_REGISTRADA",
                    "Huella no registrada",
                    null, null, null, null
            );
        }

        String nombre = alumno.getString("nombreCompleto");
        String idGrupo = String.valueOf(alumno.get("idGrupo"));
        String idRegistro = fingerprintId + "_" + LocalDate.now(ZONE_ID);

        DocumentReference ref = firestore
                .collection("registro")
                .document(idRegistro);

        DocumentSnapshot registro = ref.get().get();

        // Primera lectura del día = entrada
        if (!registro.exists()) {

            Map<String, Object> data = new HashMap<>();
            data.put("fingerprintId", fingerprintId);
            data.put("fechaHoraEntrada", Timestamp.now());
            data.put("fechaHoraSalida", null);
            data.put("estado", "EntradaRegistrada");
            data.put("idAutorizacion", null);

            ref.set(data).get();

            return new AsistenciaResponse(
                    "ENTRADA_REGISTRADA",
                    "Registro de entrada exitoso, bienvenido",
                    "EntradaRegistrada",
                    idRegistro,
                    null,
                    nombre
            );
        }

        // Ya completó entrada y salida
        if (registro.get("fechaHoraSalida") != null) {
            return new AsistenciaResponse(
                    "REGISTRO_COMPLETO",
                    "La entrada y salida del día ya fueron registradas",
                    registro.getString("estado"),
                    idRegistro,
                    registro.getString("idAutorizacion"),
                    nombre
            );
        }

        DocumentSnapshot grupo = buscarGrupo(idGrupo);

        if (grupo == null) {
            return new AsistenciaResponse(
                    "GRUPO_NO_ENCONTRADO",
                    "No se encontró el grupo del alumno",
                    "EntradaRegistrada",
                    idRegistro,
                    null,
                    nombre
            );
        }

        String horaSalidaTxt = grupo.getString("horaSalida");

        if (horaSalidaTxt == null || horaSalidaTxt.isBlank()) {
            return new AsistenciaResponse(
                    "HORARIO_NO_CONFIGURADO",
                    "El grupo no tiene hora de salida configurada",
                    "EntradaRegistrada",
                    idRegistro,
                    null,
                    nombre
            );
        }

        LocalTime horaSalida = LocalTime.parse(horaSalidaTxt);
        int minutosTolerancia = paramsService.getMinutosToleranciaSalida();
        LocalTime horaPermitida = horaSalida.minusMinutes(minutosTolerancia);
        LocalTime ahora = LocalTime.now(ZONE_ID);

        // Salida normal
        if (!ahora.isBefore(horaPermitida)) {

            Map<String, Object> update = new HashMap<>();
            update.put("fechaHoraSalida", Timestamp.now());
            update.put("estado", "SalidaRegistrada");
            update.put("idAutorizacion", null);

            ref.update(update).get();

            return new AsistenciaResponse(
                    "SALIDA_REGISTRADA",
                    "Registro de salida exitosa, buen regreso a casa",
                    "SalidaRegistrada",
                    idRegistro,
                    null,
                    nombre
            );
        }

        // Salida antes de horario: buscar autorización alumno/grupo
        DocumentSnapshot autorizacion =
                autorizacionService.buscarAutorizacionActiva(fingerprintId, idGrupo);

        if (autorizacion == null) {
            return new AsistenciaResponse(
                    "REQUIERE_AUTORIZACION",
                    "Requiere autorización de salida",
                    "EntradaRegistrada",
                    idRegistro,
                    null,
                    nombre
            );
        }

        String idAutorizacion = autorizacion.getString("idAutorizacion");

        Map<String, Object> update = new HashMap<>();
        update.put("fechaHoraSalida", Timestamp.now());
        update.put("estado", "SalidaAutorizada");
        update.put("idAutorizacion", idAutorizacion);

        ref.update(update).get();

        return new AsistenciaResponse(
                "SALIDA_AUTORIZADA",
                "Registro de salida exitosa, buen regreso a casa",
                "SalidaAutorizada",
                idRegistro,
                idAutorizacion,
                nombre
        );
    }

    private DocumentSnapshot buscarGrupo(String idGrupo) throws Exception {

        // Primero intenta String
        QuerySnapshot qs = firestore
                .collection("grupo")
                .whereEqualTo("idGrupo", idGrupo)
                .whereEqualTo("activo", true)
                .limit(1)
                .get()
                .get();

        if (!qs.isEmpty()) {
            return qs.getDocuments().get(0);
        }

        // Compatibilidad con el dato actual de Firestore si idGrupo fue creado como número
        try {
            Long idNumerico = Long.parseLong(idGrupo);

            qs = firestore
                    .collection("grupo")
                    .whereEqualTo("idGrupo", idNumerico)
                    .whereEqualTo("activo", true)
                    .limit(1)
                    .get()
                    .get();

            return qs.isEmpty() ? null : qs.getDocuments().get(0);

        } catch (NumberFormatException e) {
            return null;
        }
    }
}
