package mx.edu.cetis117.asistencia.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import mx.edu.cetis117.asistencia.dto.ConsultaAlumnoResponse;
import mx.edu.cetis117.asistencia.dto.ConsultaRegistroDto;
import mx.edu.cetis117.asistencia.util.FirestoreUtil;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class ConsultaService {

    private static final ZoneId ZONE_ID =
            ZoneId.of("America/Mexico_City");

    private final Firestore firestore;
    private final AlumnoService alumnoService;
    private final UsuarioSistemaService usuarioSistemaService;

    public ConsultaService(
            Firestore firestore,
            AlumnoService alumnoService,
            UsuarioSistemaService usuarioSistemaService
    ) {
        this.firestore = firestore;
        this.alumnoService = alumnoService;
        this.usuarioSistemaService = usuarioSistemaService;
    }

    public List<ConsultaAlumnoResponse> consultar(
            String codigo,
            String nombre,
            String idGrupo,
            String iniciales
    ) throws Exception {

        DocumentSnapshot superUsuario =
                usuarioSistemaService.buscarPorCodigo(codigo);

        List<DocumentSnapshot> alumnos = new ArrayList<>();

        if (superUsuario != null) {
            QuerySnapshot query = firestore
                    .collection("alumnos")
                    .whereEqualTo("activo", true)
                    .get()
                    .get();

            for (QueryDocumentSnapshot documento : query.getDocuments()) {
                if (!cumpleFiltros(
                        documento,
                        nombre,
                        idGrupo,
                        iniciales
                )) {
                    continue;
                }

                alumnos.add(documento);
            }

        } else {
            DocumentSnapshot alumno =
                    alumnoService.buscarPorCodigoTutor(codigo);

            if (alumno == null) {
                return List.of();
            }

            alumnos.add(alumno);
        }

        List<ConsultaAlumnoResponse> salida = new ArrayList<>();

        for (DocumentSnapshot alumno : alumnos) {
            salida.add(mapAlumno(alumno));
        }

        salida.sort(
                Comparator.comparing(
                        ConsultaAlumnoResponse::nombreCompleto,
                        Comparator.nullsLast(
                                String.CASE_INSENSITIVE_ORDER
                        )
                )
        );

        return salida;
    }

    private boolean cumpleFiltros(
            DocumentSnapshot alumno,
            String nombre,
            String idGrupo,
            String iniciales
    ) {

        if (nombre != null && !nombre.isBlank()) {
            String nombreCompleto =
                    alumnoService.obtenerNombreCompleto(alumno);

            if (!nombreCompleto
                    .toLowerCase()
                    .contains(nombre.trim().toLowerCase())) {

                return false;
            }
        }

        if (idGrupo != null && !idGrupo.isBlank()) {
            String grupoAlumno =
                    String.valueOf(alumno.get("idGrupo"));

            if (!grupoAlumno.equals(idGrupo.trim())) {
                return false;
            }
        }

        if (iniciales != null && !iniciales.isBlank()) {
            String inicialesAlumno =
                    alumnoService.obtenerIniciales(alumno);

            String filtroIniciales =
                    alumnoService.normalizarClaveIniciales(iniciales);

            if (!inicialesAlumno.equals(filtroIniciales)) {
                return false;
            }
        }

        return true;
    }

    private ConsultaAlumnoResponse mapAlumno(
            DocumentSnapshot alumno
    ) throws Exception {

        Long fingerprintId =
                alumno.getLong("fingerprintId");

        QuerySnapshot registros = firestore
                .collection("registro")
                .whereEqualTo("fingerprintId", fingerprintId)
                .get()
                .get();

        List<ConsultaRegistroDto> filas = new ArrayList<>();

        for (DocumentSnapshot registro : registros.getDocuments()) {

            String nombreAutoriza = null;
            String motivo = null;

            String idAutorizacion =
                    registro.getString("idAutorizacion");

            if (idAutorizacion != null
                    && !idAutorizacion.isBlank()) {

                DocumentSnapshot autorizacion = firestore
                        .collection("autorizacionesSalida")
                        .document(idAutorizacion)
                        .get()
                        .get();

                if (autorizacion.exists()) {
                    motivo = autorizacion.getString("motivo");

                    String idUsuario =
                            autorizacion.getString(
                                    "idUsuarioAutoriza"
                            );

                    if (idUsuario != null) {
                        QuerySnapshot usuarioQuery = firestore
                                .collection("usuariosSistema")
                                .whereEqualTo("idUsuario", idUsuario)
                                .limit(1)
                                .get()
                                .get();

                        if (!usuarioQuery.isEmpty()) {
                            nombreAutoriza = usuarioQuery
                                    .getDocuments()
                                    .get(0)
                                    .getString("nombre");
                        }
                    }
                }
            }

            filas.add(
                    new ConsultaRegistroDto(
                            FirestoreUtil.fecha(
                                    registro.get("fechaHoraEntrada"),
                                    ZONE_ID
                            ),
                            FirestoreUtil.hora(
                                    registro.get("fechaHoraEntrada"),
                                    ZONE_ID
                            ),
                            FirestoreUtil.hora(
                                    registro.get("fechaHoraSalida"),
                                    ZONE_ID
                            ),
                            registro.getString("estado"),
                            nombreAutoriza,
                            motivo
                    )
            );
        }

        filas.sort(
                Comparator.comparing(
                        ConsultaRegistroDto::fecha,
                        Comparator.nullsLast(
                                Comparator.reverseOrder()
                        )
                )
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> tutor =
                (Map<String, Object>) alumno.get("tutor");

        return new ConsultaAlumnoResponse(
                alumno.getString("idAlumno"),
                alumnoService.obtenerNombreCompleto(alumno),
                String.valueOf(alumno.get("idGrupo")),
                alumnoService.obtenerIniciales(alumno),
                alumno.getBoolean("activo"),
                tutor,
                filas
        );
    }
}
