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

    private static final ZoneId ZONE_ID = ZoneId.of("America/Mexico_City");

    private final Firestore firestore;
    private final AlumnoService alumnos;
    private final UsuarioSistemaService usuarios;

    public ConsultaService(
            Firestore firestore,
            AlumnoService alumnos,
            UsuarioSistemaService usuarios
    ) {
        this.firestore = firestore;
        this.alumnos = alumnos;
        this.usuarios = usuarios;
    }

    public List<ConsultaAlumnoResponse> consultar(
            String codigo,
            String nombre
    ) throws Exception {

        DocumentSnapshot superUsuario = usuarios.buscarPorCodigo(codigo);

        List<DocumentSnapshot> lista = new ArrayList<>();

        if (superUsuario != null) {

            if (nombre == null || nombre.isBlank()) {

                QuerySnapshot qs = firestore
                        .collection("alumnos")
                        .whereEqualTo("activo", true)
                        .get()
                        .get();

                for (QueryDocumentSnapshot doc : qs.getDocuments()) {
                    lista.add(doc);
                }

            } else {
                lista.addAll(alumnos.buscarPorNombre(nombre));
            }

        } else {

            DocumentSnapshot alumno = alumnos.buscarPorCodigoTutor(codigo);

            if (alumno == null) {
                return List.of();
            }

            lista.add(alumno);
        }

        List<ConsultaAlumnoResponse> salida = new ArrayList<>();

        for (DocumentSnapshot alumno : lista) {
            salida.add(mapAlumno(alumno));
        }

        return salida;
    }

    private ConsultaAlumnoResponse mapAlumno(
            DocumentSnapshot alumno
    ) throws Exception {

        Long fingerprintId = alumno.getLong("fingerprintId");

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

                    String idUsuario = autorizacion
                            .getString("idUsuarioAutoriza");

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
                alumnos.obtenerNombreCompleto(alumno),
                String.valueOf(alumno.get("idGrupo")),
                tutor,
                filas
        );
    }
}
