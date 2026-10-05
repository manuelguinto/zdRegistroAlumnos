package mx.edu.cetis117.asistencia.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import mx.edu.cetis117.asistencia.dto.GrupoResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GrupoService {

    private static final ZoneId ZONE_ID =
            ZoneId.of("America/Mexico_City");

    private final Firestore firestore;
    private final UsuarioSistemaService usuarioSistemaService;

    public GrupoService(
            Firestore firestore,
            UsuarioSistemaService usuarioSistemaService
    ) {
        this.firestore = firestore;
        this.usuarioSistemaService = usuarioSistemaService;
    }

    public List<GrupoResponse> listar(
            String codigo
    ) throws Exception {

        DocumentSnapshot usuario =
                usuarioSistemaService.buscarPorCodigo(codigo);

        if (usuario == null) {
            throw new IllegalArgumentException(
                    "Código de usuario de sistema inválido"
            );
        }

        Set<String> gruposAutorizadosHoy =
                obtenerGruposAutorizadosHoy();

        QuerySnapshot query = firestore
                .collection("grupo")
                .get()
                .get();

        List<GrupoResponse> grupos = new ArrayList<>();
        LocalTime horaActual = LocalTime.now(ZONE_ID);

        for (DocumentSnapshot documento : query.getDocuments()) {

            Boolean activo = documento.getBoolean("activo");

            if (!Boolean.TRUE.equals(activo)) {
                continue;
            }

            String idGrupo =
                    String.valueOf(documento.get("idGrupo"));

            String nombre =
                    documento.getString("nombre");

            String horaSalida =
                    documento.getString("horaSalida");

            String estadoSalida = "PENDIENTE";
            boolean salidaHabilitada = false;

            /*
             * La autorización de grupo tiene prioridad para identificar
             * la razón por la cual ya puede retirarse.
             */
            if (gruposAutorizadosHoy.contains(idGrupo)) {

                estadoSalida = "SALIDA_POR_AUTORIZACION";
                salidaHabilitada = true;

            } else if (horaSalida != null
                    && !horaSalida.isBlank()) {

                LocalTime horarioConfigurado =
                        LocalTime.parse(horaSalida);

                /*
                 * Para el estado general del grupo usamos la hora oficial
                 * configurada del grupo, no los minutos de tolerancia.
                 */
                if (!horaActual.isBefore(horarioConfigurado)) {
                    estadoSalida = "SALIDA_POR_HORARIO";
                    salidaHabilitada = true;
                }
            }

            grupos.add(
                    new GrupoResponse(
                            idGrupo,
                            nombre,
                            horaSalida,
                            activo,
                            estadoSalida,
                            salidaHabilitada
                    )
            );
        }

        grupos.sort(
                Comparator.comparing(
                        GrupoResponse::nombre,
                        Comparator.nullsLast(
                                String.CASE_INSENSITIVE_ORDER
                        )
                )
        );

        return grupos;
    }

    private Set<String> obtenerGruposAutorizadosHoy()
            throws Exception {

        String hoy =
                LocalDate.now(ZONE_ID).toString();

        QuerySnapshot autorizaciones = firestore
                .collection("autorizacionesSalida")
                .whereEqualTo("fecha", hoy)
                .get()
                .get();

        Set<String> grupos = new HashSet<>();

        for (DocumentSnapshot autorizacion
                : autorizaciones.getDocuments()) {

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

            Object idGrupo =
                    autorizacion.get("idGrupo");

            if (idGrupo != null) {
                grupos.add(
                        String.valueOf(idGrupo)
                );
            }
        }

        return grupos;
    }
}
