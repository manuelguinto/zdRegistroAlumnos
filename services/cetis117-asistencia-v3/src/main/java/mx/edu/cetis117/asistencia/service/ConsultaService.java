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
import java.util.HashMap;
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
                usuarioSistemaService.buscarPorCodigo(
                        codigo
                );

        List<DocumentSnapshot> alumnos =
                new ArrayList<>();

        if (superUsuario != null) {

            QuerySnapshot query = firestore
                    .collection("alumnos")
                    .whereEqualTo("activo", true)
                    .get()
                    .get();

            for (QueryDocumentSnapshot documento
                    : query.getDocuments()) {

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
                    alumnoService.buscarPorCodigoTutor(
                            codigo
                    );

            if (alumno == null) {
                return List.of();
            }

            alumnos.add(alumno);
        }

        Map<String, DocumentSnapshot> salidasGenerales =
                obtenerSalidasGeneralesPorFecha();

        List<ConsultaAlumnoResponse> salida =
                new ArrayList<>();

        for (DocumentSnapshot alumno
                : alumnos) {

            salida.add(
                    mapAlumno(
                            alumno,
                            salidasGenerales
                    )
            );
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

        if (nombre != null
                && !nombre.isBlank()) {

            String nombreCompleto =
                    alumnoService.obtenerNombreCompleto(
                            alumno
                    );

            if (!nombreCompleto
                    .toLowerCase()
                    .contains(
                            nombre.trim().toLowerCase()
                    )) {

                return false;
            }
        }

        if (idGrupo != null
                && !idGrupo.isBlank()) {

            String grupoAlumno =
                    String.valueOf(
                            alumno.get("idGrupo")
                    );

            if (!grupoAlumno.equals(
                    idGrupo.trim()
            )) {
                return false;
            }
        }

        if (iniciales != null
                && !iniciales.isBlank()) {

            String inicialesAlumno =
                    alumnoService.obtenerIniciales(
                            alumno
                    );

            String filtroIniciales =
                    alumnoService.normalizarClaveIniciales(
                            iniciales
                    );

            if (!inicialesAlumno.equals(
                    filtroIniciales
            )) {
                return false;
            }
        }

        return true;
    }

    private ConsultaAlumnoResponse mapAlumno(
            DocumentSnapshot alumno,
            Map<String, DocumentSnapshot> salidasGenerales
    ) throws Exception {

        Long fingerprintId =
                alumno.getLong("fingerprintId");

        QuerySnapshot registros = firestore
                .collection("registro")
                .whereEqualTo(
                        "fingerprintId",
                        fingerprintId
                )
                .get()
                .get();

        List<ConsultaRegistroDto> filas =
                new ArrayList<>();

        for (DocumentSnapshot registro
                : registros.getDocuments()) {

            String fecha =
                    FirestoreUtil.fecha(
                            registro.get(
                                    "fechaHoraEntrada"
                            ),
                            ZONE_ID
                    );

            String horaEntrada =
                    FirestoreUtil.hora(
                            registro.get(
                                    "fechaHoraEntrada"
                            ),
                            ZONE_ID
                    );

            String horaSalida =
                    FirestoreUtil.hora(
                            registro.get(
                                    "fechaHoraSalida"
                            ),
                            ZONE_ID
                    );

            String estado =
                    registro.getString("estado");

            String nombreAutoriza = null;
            String motivo = null;

            String idAutorizacion =
                    registro.getString(
                            "idAutorizacion"
                    );

            /*
             * Prioridad 1:
             * Si existe salida real, nunca se sustituye por
             * una salida general.
             */
            if (horaSalida != null) {

                if (idAutorizacion != null
                        && !idAutorizacion.isBlank()) {

                    DatosAutorizacion datos =
                            obtenerDatosAutorizacion(
                                    idAutorizacion
                            );

                    nombreAutoriza =
                            datos.nombreAutoriza();

                    motivo =
                            datos.motivo();

                    if ("ALUMNO".equals(datos.tipo())
                            || "GRUPO".equals(datos.tipo())) {

                        estado = "SalidaAnticipada";
                    }
                }

            } else {

                /*
                 * Prioridad 2:
                 * Si el registro ya tiene una autorización individual
                 * o de grupo asociada, se conserva esa referencia.
                 */
                if (idAutorizacion != null
                        && !idAutorizacion.isBlank()) {

                    DatosAutorizacion datos =
                            obtenerDatosAutorizacion(
                                    idAutorizacion
                            );

                    nombreAutoriza =
                            datos.nombreAutoriza();

                    motivo =
                            datos.motivo();

                } else {

                    /*
                     * Prioridad 3:
                     * Salida GENERAL virtual.
                     *
                     * No modifica el documento "registro".
                     * Solo se aplica si hubo entrada y no existe
                     * una salida real.
                     */
                    DocumentSnapshot salidaGeneral =
                            salidasGenerales.get(
                                    fecha
                            );

                    if (salidaGeneral != null
                            && horaEntrada != null) {

                        horaSalida =
                                salidaGeneral.getString(
                                        "horaSalidaGeneral"
                                );

                        estado = "SalidaGeneral";

                        DatosAutorizacion datos =
                                obtenerDatosAutorizacion(
                                        salidaGeneral.getString(
                                                "idAutorizacion"
                                        )
                                );

                        nombreAutoriza =
                                datos.nombreAutoriza();

                        motivo =
                                datos.motivo();
                    }
                }
            }

            filas.add(
                    new ConsultaRegistroDto(
                            fecha,
                            horaEntrada,
                            horaSalida,
                            estado,
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
                (Map<String, Object>)
                        alumno.get("tutor");

        return new ConsultaAlumnoResponse(
                alumno.getString("idAlumno"),
                alumnoService.obtenerNombreCompleto(
                        alumno
                ),
                String.valueOf(
                        alumno.get("idGrupo")
                ),
                alumnoService.obtenerIniciales(
                        alumno
                ),
                alumno.getBoolean("activo"),
                tutor,
                filas
        );
    }

    private Map<String, DocumentSnapshot>
    obtenerSalidasGeneralesPorFecha()
            throws Exception {

        QuerySnapshot autorizaciones = firestore
                .collection("autorizacionesSalida")
                .get()
                .get();

        Map<String, DocumentSnapshot> resultado =
                new HashMap<>();

        for (DocumentSnapshot autorizacion
                : autorizaciones.getDocuments()) {

            if (!Boolean.TRUE.equals(
                    autorizacion.getBoolean("activa")
            )) {
                continue;
            }

            if (!"GENERAL".equals(
                    autorizacion.getString("tipo")
            )) {
                continue;
            }

            String fecha =
                    autorizacion.getString("fecha");

            String hora =
                    autorizacion.getString(
                            "horaSalidaGeneral"
                    );

            if (fecha == null
                    || fecha.isBlank()
                    || hora == null
                    || hora.isBlank()) {

                continue;
            }

            resultado.put(
                    fecha,
                    autorizacion
            );
        }

        return resultado;
    }

    private DatosAutorizacion obtenerDatosAutorizacion(
            String idAutorizacion
    ) throws Exception {

        if (idAutorizacion == null
                || idAutorizacion.isBlank()) {

            return new DatosAutorizacion(
                    null,
                    null,
                    null
            );
        }

        DocumentSnapshot autorizacion = firestore
                .collection("autorizacionesSalida")
                .document(idAutorizacion)
                .get()
                .get();

        if (!autorizacion.exists()) {
            return new DatosAutorizacion(
                    null,
                    null,
                    null
            );
        }

        String motivo =
                autorizacion.getString("motivo");

        String nombreAutoriza = null;

        String idUsuario =
                autorizacion.getString(
                        "idUsuarioAutoriza"
                );

        if (idUsuario != null
                && !idUsuario.isBlank()) {

            QuerySnapshot usuarioQuery = firestore
                    .collection("usuariosSistema")
                    .whereEqualTo(
                            "idUsuario",
                            idUsuario
                    )
                    .limit(1)
                    .get()
                    .get();

            if (!usuarioQuery.isEmpty()) {

                nombreAutoriza =
                        usuarioQuery
                                .getDocuments()
                                .get(0)
                                .getString("nombre");
            }
        }

        return new DatosAutorizacion(
                nombreAutoriza,
                motivo,
                autorizacion.getString("tipo")
        );
    }

    private record DatosAutorizacion(
            String nombreAutoriza,
            String motivo,
            String tipo
    ) {
    }
}
