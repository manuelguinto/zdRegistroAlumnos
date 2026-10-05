package mx.edu.cetis117.asistencia.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AlumnoService {

    private final Firestore firestore;

    public AlumnoService(Firestore firestore) {
        this.firestore = firestore;
    }

    public DocumentSnapshot buscarPorFingerprint(
            Long fingerprintId
    ) throws Exception {

        QuerySnapshot query = firestore
                .collection("alumnos")
                .whereEqualTo("fingerprintId", fingerprintId)
                .whereEqualTo("activo", true)
                .limit(1)
                .get()
                .get();

        return query.isEmpty()
                ? null
                : query.getDocuments().get(0);
    }

    public DocumentSnapshot buscarPorCodigoTutor(
            String codigoTutorHash
    ) throws Exception {

        QuerySnapshot query = firestore
                .collection("alumnos")
                .whereEqualTo("codigoTutorHash", codigoTutorHash)
                .whereEqualTo("activo", true)
                .limit(1)
                .get()
                .get();

        return query.isEmpty()
                ? null
                : query.getDocuments().get(0);
    }

    public List<DocumentSnapshot> buscarPorNombre(
            String nombre
    ) throws Exception {

        String filtro = nombre == null
                ? ""
                : nombre.trim().toLowerCase();

        QuerySnapshot query = firestore
                .collection("alumnos")
                .whereEqualTo("activo", true)
                .get()
                .get();

        List<DocumentSnapshot> resultado = new ArrayList<>();

        for (DocumentSnapshot documento : query.getDocuments()) {
            String nombreCompleto = obtenerNombreCompleto(documento);

            if (nombreCompleto
                    .toLowerCase()
                    .contains(filtro)) {

                resultado.add(documento);
            }
        }

        return resultado;
    }


public DocumentSnapshot buscarPorIniciales(
        String iniciales
) throws Exception {

    String inicialesNormalizadas =
            normalizarClaveIniciales(iniciales);

    if (inicialesNormalizadas.isBlank()) {
        return null;
    }

    QuerySnapshot query = firestore
            .collection("alumnos")
            .whereEqualTo("iniciales", inicialesNormalizadas)
            .whereEqualTo("activo", true)
            .get()
            .get();

    if (query.size() > 1) {
        throw new IllegalArgumentException(
                "Las iniciales corresponden a más de un alumno"
        );
    }

    return query.isEmpty()
            ? null
            : query.getDocuments().get(0);
}

    public String obtenerNombreCompleto(
            DocumentSnapshot alumno
    ) {
        String nombreCompleto = alumno.getString("nombreCompleto");

        if (nombreCompleto != null
                && !nombreCompleto.isBlank()) {

            return nombreCompleto;
        }

        return Stream.of(
                        alumno.getString("nombre"),
                        alumno.getString("apellidoPaterno"),
                        alumno.getString("apellidoMaterno")
                )
                .filter(
                        valor ->
                                valor != null
                                        && !valor.isBlank()
                )
                .collect(Collectors.joining(" "));
    }


public String obtenerIniciales(
        DocumentSnapshot alumno
) {
    String iniciales = alumno.getString("iniciales");

    if (iniciales == null || iniciales.isBlank()) {
        return "";
    }

    return normalizarClaveIniciales(iniciales);
}

public String normalizarClaveIniciales(
        String texto
) {
    if (texto == null || texto.isBlank()) {
        return "";
    }

    return Normalizer
            .normalize(texto, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replaceAll("[^A-Za-z0-9]", "")
            .trim()
            .toUpperCase();
}

    public int importarCsv(
            MultipartFile file
    ) throws Exception {

        CSVFormat format = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build();

        int count = 0;
        WriteBatch batch = firestore.batch();

        try (
                InputStreamReader reader =
                        new InputStreamReader(
                                file.getInputStream(),
                                StandardCharsets.UTF_8
                        );
                var parser = format.parse(reader)
        ) {
            for (CSVRecord record : parser) {
                String idAlumno = required(record, "idAlumno");

                Map<String, Object> documento = new HashMap<>();

                documento.put("idAlumno", idAlumno);
                documento.put("nombre", value(record, "nombre"));
                documento.put(
                        "apellidoPaterno",
                        value(record, "apellidoPaterno")
                );
                documento.put(
                        "apellidoMaterno",
                        value(record, "apellidoMaterno")
                );
                documento.put(
                        "nombreCompleto",
                        value(record, "nombreCompleto")
                );
                documento.put(
                        "idGrupo",
                        value(record, "idGrupo")
                );
                documento.put(
                        "iniciales",
                        normalizarClaveIniciales(
                                value(record, "iniciales")
                        )
                );
                documento.put(
                        "fingerprintId",
                        Long.parseLong(
                                value(record, "fingerprintId")
                        )
                );
                documento.put(
                        "codigoTutorHash",
                        value(record, "codigoTutorHash")
                );

                Map<String, Object> tutor = new HashMap<>();

                tutor.put(
                        "nombre",
                        value(record, "tutorNombre")
                );
                tutor.put(
                        "parentesco",
                        value(record, "tutorParentesco")
                );
                tutor.put(
                        "telefono",
                        value(record, "tutorTelefono")
                );
                tutor.put(
                        "telefonoAlternativo",
                        value(
                                record,
                                "tutorTelefonoAlternativo"
                        )
                );
                tutor.put(
                        "correo",
                        value(record, "tutorCorreo")
                );

                documento.put("tutor", tutor);
                documento.put("activo", true);
                documento.put(
                        "fechaRegistro",
                        Timestamp.now()
                );
                documento.put(
                        "fechaActualizacion",
                        Timestamp.now()
                );

                batch.set(
                        firestore
                                .collection("alumnos")
                                .document(idAlumno),
                        documento
                );

                count++;

                if (count % 400 == 0) {
                    batch.commit().get();
                    batch = firestore.batch();
                }
            }
        }

        if (count % 400 != 0) {
            batch.commit().get();
        }

        return count;
    }

    private static String value(
            CSVRecord record,
            String name
    ) {
        try {
            String value = record.get(name);

            return value == null
                    ? ""
                    : value.trim();

        } catch (Exception exception) {
            return "";
        }
    }

    private static String required(
            CSVRecord record,
            String name
    ) {
        String value = value(record, name);

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "Falta valor obligatorio: " + name
            );
        }

        return value;
    }
}
