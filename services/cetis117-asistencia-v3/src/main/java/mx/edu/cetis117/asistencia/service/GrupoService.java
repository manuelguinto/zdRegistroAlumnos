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
                usuarioSistemaService.buscarPorCodigo(
                        codigo
                );

        if (usuario == null) {
            throw new IllegalArgumentException(
                    "Código de usuario de sistema inválido"
            );
        }

        EstadoAutorizaciones estadoDia =
                obtenerEstadoAutorizacionesHoy();

        QuerySnapshot query = firestore
                .collection("grupo")
                .get()
                .get();

        List<GrupoResponse> grupos =
                new ArrayList<>();

        LocalTime horaActual =
                LocalTime.now(ZONE_ID);

        for (DocumentSnapshot documento
                : query.getDocuments()) {

            Boolean activo =
                    documento.getBoolean("activo");

            if (!Boolean.TRUE.equals(activo)) {
                continue;
            }

            String idGrupo =
                    String.valueOf(
                            documento.get("idGrupo")
                    );

            String nombre =
                    documento.getString("nombre");

            String horaSalida =
                    documento.getString("horaSalida");

            String estadoSalida =
                    "PENDIENTE";

            boolean salidaHabilitada =
                    false;

            String horaSalidaEfectiva =
                    null;

            /*
             * Prioridad:
             * GENERAL > GRUPO > HORARIO > PENDIENTE
             */
            if (estadoDia.salidaGeneral() != null) {

                estadoSalida =
                        "SALIDA_GENERAL";

                salidaHabilitada =
                        true;

                horaSalidaEfectiva =
                        estadoDia
                                .salidaGeneral()
                                .getString(
                                        "horaSalidaGeneral"
                                );

            } else if (estadoDia
                    .gruposAutorizados()
                    .contains(idGrupo)) {

                estadoSalida =
                        "SALIDA_POR_AUTORIZACION";

                salidaHabilitada =
                        true;

            } else if (horaSalida != null
                    && !horaSalida.isBlank()) {

                LocalTime horarioConfigurado =
                        LocalTime.parse(
                                horaSalida
                        );

                if (!horaActual.isBefore(
                        horarioConfigurado
                )) {
                    estadoSalida =
                            "SALIDA_POR_HORARIO";

                    salidaHabilitada =
                            true;

                    horaSalidaEfectiva =
                            horaSalida;
                }
            }

            grupos.add(
                    new GrupoResponse(
                            idGrupo,
                            nombre,
                            horaSalida,
                            activo,
                            estadoSalida,
                            salidaHabilitada,
                            horaSalidaEfectiva
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

    private EstadoAutorizaciones
    obtenerEstadoAutorizacionesHoy()
            throws Exception {

        String hoy =
                LocalDate.now(ZONE_ID)
                        .toString();

        QuerySnapshot autorizaciones = firestore
                .collection("autorizacionesSalida")
                .whereEqualTo("fecha", hoy)
                .get()
                .get();

        Set<String> grupos =
                new HashSet<>();

        DocumentSnapshot salidaGeneral =
                null;

        for (DocumentSnapshot autorizacion
                : autorizaciones.getDocuments()) {

            if (!Boolean.TRUE.equals(
                    autorizacion.getBoolean("activa")
            )) {
                continue;
            }

            String tipo =
                    autorizacion.getString("tipo");

            if ("GENERAL".equals(tipo)) {

                salidaGeneral =
                        autorizacion;

                continue;
            }

            if ("GRUPO".equals(tipo)) {

                Object idGrupo =
                        autorizacion.get("idGrupo");

                if (idGrupo != null) {
                    grupos.add(
                            String.valueOf(idGrupo)
                    );
                }
            }
        }

        return new EstadoAutorizaciones(
                grupos,
                salidaGeneral
        );
    }

    private record EstadoAutorizaciones(
            Set<String> gruposAutorizados,
            DocumentSnapshot salidaGeneral
    ) {
    }
}
