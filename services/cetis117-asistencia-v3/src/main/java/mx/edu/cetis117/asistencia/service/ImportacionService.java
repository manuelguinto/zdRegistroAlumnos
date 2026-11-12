package mx.edu.cetis117.asistencia.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import org.apache.commons.csv.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class ImportacionService {
    private final Firestore firestore;
    private final UsuarioSistemaService usuarios;

    public ImportacionService(Firestore firestore, UsuarioSistemaService usuarios) {
        this.firestore = firestore;
        this.usuarios = usuarios;
    }

    public void autorizar(String codigo) throws Exception {
        if (codigo == null || codigo.isBlank() || usuarios.buscarPorCodigo(codigo) == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Codigo de sistema invalido");
        }
    }

    public Map<String, Object> importar(MultipartFile file, String coleccion, String codigo) throws Exception {
        return importarDatos(file.getInputStream(), file.isEmpty(), coleccion, codigo);
    }

    public Map<String, Object> guardarIndividual(Map<String, Object> entrada, String coleccion, String codigo) throws Exception {
        autorizar(codigo);
        Map<String, Object> datos = new LinkedHashMap<>(entrada);
        String clave = coleccion.equals("alumnos") ? "idAlumno" : "idGrupo";
        boolean automatico = Objects.toString(datos.get(clave), "").isBlank();
        String identificador = automatico ? siguienteId(firestore.collection(coleccion).get().get().getDocuments(), clave)
                : id(Objects.toString(datos.get(clave), "").trim());
        datos.put(clave, identificador);
        boolean existe = firestore.collection(coleccion).document(identificador).get().get().exists();
        if (coleccion.equals("alumnos")) {
            datos.put("fingerprintId", identificador);
            String nombre = String.join(" ", Objects.toString(datos.get("nombre"), ""),
                    Objects.toString(datos.get("apellidoPaterno"), ""), Objects.toString(datos.get("apellidoMaterno"), ""));
            StringBuilder iniciales = new StringBuilder();
            for (String palabra : nombre.trim().split("\\s+")) if (!palabra.isBlank()) iniciales.append(palabra.charAt(0));
            datos.put("iniciales", iniciales.toString());
            // En actualizaciones conserva el codigo del tutor salvo envio explicito.
            if (Objects.toString(datos.get("codigoTutorHash"), "").isBlank()) {
                String anterior = existe ? firestore.collection(coleccion).document(identificador).get().get().getString("codigoTutorHash") : null;
                String nuevo = anterior;
                java.security.SecureRandom random = new java.security.SecureRandom();
                while (nuevo == null || nuevo.isBlank()) {
                    String candidato = Integer.toString(100000 + random.nextInt(900000));
                    if (firestore.collection("alumnos").whereEqualTo("codigoTutorHash", candidato).limit(1).get().get().isEmpty()) nuevo = candidato;
                }
                datos.put("codigoTutorHash", nuevo);
            }
            Object tutor = datos.remove("tutor");
            if (tutor instanceof Map<?, ?> campos) campos.forEach((k, v) -> datos.put("tutor." + k, v));
        }
        StringWriter texto = new StringWriter();
        try (CSVPrinter csv = new CSVPrinter(texto, CSVFormat.DEFAULT)) {
            csv.printRecord(datos.keySet());
            csv.printRecord(datos.values().stream().map(v -> Objects.toString(v, "")).toList());
        }
        Map<String, Object> resultado = new LinkedHashMap<>(importarDatos(
                new ByteArrayInputStream(texto.toString().getBytes(StandardCharsets.UTF_8)), false, coleccion, codigo, automatico));
        resultado.put(clave, identificador);
        if (coleccion.equals("alumnos")) resultado.put("codigoTutorHash", datos.get("codigoTutorHash"));
        return resultado;
    }

    private Map<String, Object> importarDatos(InputStream contenido, boolean vacio, String coleccion, String codigo) throws Exception {
        return importarDatos(contenido, vacio, coleccion, codigo, false);
    }

    private Map<String, Object> importarDatos(InputStream contenido, boolean vacio, String coleccion, String codigo, boolean soloCrear) throws Exception {
        try (InputStream stream = contenido) {
            return procesarImportacion(stream, vacio, coleccion, codigo, soloCrear);
        } catch (java.util.concurrent.ExecutionException ex) {
            if (soloCrear && ex.getCause() instanceof com.google.api.gax.rpc.AlreadyExistsException)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Otro usuario registro el mismo ID. Vuelve a guardar para asignar el siguiente.");
            throw ex;
        }
    }

    static String siguienteId(List<? extends DocumentSnapshot> documentos, String clave) {
        int maximo = 0;
        for (DocumentSnapshot doc : documentos) {
            for (String valor : List.of(doc.getId(), Objects.toString(doc.get(clave), ""))) {
                if (valor.matches("[0-9]{1,4}")) maximo = Math.max(maximo, Integer.parseInt(valor));
            }
        }
        if (maximo >= 1000) throw new IllegalArgumentException("Se alcanzo el limite de IDs (1000)");
        return Integer.toString(maximo + 1);
    }

    private Map<String, Object> procesarImportacion(InputStream contenido, boolean vacio, String coleccion, String codigo, boolean soloCrear) throws Exception {
        autorizar(codigo);
        if (vacio) throw new IllegalArgumentException("El CSV esta vacio");
        boolean alumnos = coleccion.equals("alumnos");
        List<Map<String, Object>> errores = new ArrayList<>();
        Map<String, Map<String, Object>> pendientes = new LinkedHashMap<>();
        Map<String, DocumentSnapshot> existentes = new HashMap<>();
        Map<String, String> codigos = new HashMap<>();
        for (DocumentSnapshot doc : firestore.collection(coleccion).get().get().getDocuments()) {
            existentes.put(doc.getId(), doc);
            if (alumnos && doc.getString("codigoTutorHash") != null)
                codigos.put(doc.getString("codigoTutorHash"), doc.getId());
        }
        Set<String> grupos = new HashSet<>();
        if (alumnos) for (DocumentSnapshot doc : firestore.collection("grupo").get().get().getDocuments())
            grupos.add(doc.getId());

        Set<String> ids = new HashSet<>();
        try (PushbackReader reader = new PushbackReader(new InputStreamReader(contenido, StandardCharsets.UTF_8), 1)) {
            int primero = reader.read();
            if (primero != -1 && primero != '\uFEFF') reader.unread(primero);
            try (CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true).setTrim(true).get().parse(reader)) {
                String clave = alumnos ? "idAlumno" : "idGrupo";
                boolean hayFilas = false;
                for (CSVRecord fila : parser) {
                    hayFilas = true;
                    int erroresPrevios = errores.size();
                    validarFila(fila, alumnos, grupos, ids, codigos, errores);
                    if (errores.size() != erroresPrevios) continue;
                    try {
                        if (!fila.isConsistent()) throw new IllegalArgumentException("Cantidad de columnas incorrecta");
                        String id = id(required(fila, clave));
                        DocumentSnapshot anterior = existentes.get(id);
                        Map<String, Object> datos = alumnos ? alumno(fila, grupos) : grupo(fila);
                        datos.put(clave, id);
                        if (alumnos) {
                            String tutorCodigo = (String) datos.get("codigoTutorHash");
                            String propietario = codigos.get(tutorCodigo);
                            if (propietario != null && !propietario.equals(id))
                                throw new IllegalArgumentException("codigoTutorHash ya pertenece a otro alumno");
                            codigos.put(tutorCodigo, id);
                            if (anterior == null || anterior.get("fechaRegistro") == null)
                                datos.put("fechaRegistro", Timestamp.now());
                            datos.put("fechaActualizacion", Timestamp.now());
                        } else {
                            if (anterior != null && anterior.get("horaSalida") != null)
                                datos.remove("horaSalida");
                            if (anterior == null || anterior.get("fechaCreacion") == null)
                                datos.put("fechaCreacion", Timestamp.now());
                        }
                        pendientes.put(id, datos);
                    } catch (IllegalArgumentException ex) {
                        errores.add(Map.of("fila", fila.getRecordNumber() + 1, "error", ex.getMessage()));
                    }
                }
                if (!hayFilas) errores.add(Map.of("fila", 1, "campo", "archivo", "error", "El CSV no contiene filas de datos"));
            }
        } catch (IOException | UncheckedIOException ex) {
            errores.add(Map.of("fila", 0, "campo", "archivo", "error", "CSV invalido: " + ex.getMessage()));
        }
        if (!errores.isEmpty()) throw new ValidacionImportacionException(errores);
        int insertados = 0, actualizados = 0, operaciones = 0;
        WriteBatch batch = firestore.batch();
        for (var item : pendientes.entrySet()) {
            if (soloCrear) {
                if (existentes.containsKey(item.getKey()))
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "El ID acaba de ser registrado. Vuelve a guardar.");
                batch.create(firestore.collection(coleccion).document(item.getKey()), item.getValue());
            } else batch.set(firestore.collection(coleccion).document(item.getKey()), item.getValue(), SetOptions.merge());
            if (existentes.containsKey(item.getKey())) actualizados++; else insertados++;
            if (++operaciones % 400 == 0) { batch.commit().get(); batch = firestore.batch(); }
        }
        if (operaciones % 400 != 0) batch.commit().get();
        return Map.of("registrosImportados", operaciones, "insertados", insertados,
                "actualizados", actualizados, "rechazados", errores.size(), "errores", errores);
    }

    private static void comprobar(CSVRecord fila, String campo, Runnable validacion,
            List<Map<String, Object>> errores) {
        try { validacion.run(); }
        catch (IllegalArgumentException ex) {
            errores.add(Map.of("fila", fila.getRecordNumber() + 1, "campo", campo, "error", ex.getMessage()));
        }
    }

    static void validarFila(CSVRecord fila, boolean alumnos, Set<String> grupos,
            Set<String> ids, Map<String, String> codigos, List<Map<String, Object>> errores) {
        if (!fila.isConsistent())
            errores.add(Map.of("fila", fila.getRecordNumber() + 1, "campo", "fila", "error", "Cantidad de columnas incorrecta"));
        String clave = alumnos ? "idAlumno" : "idGrupo";
        comprobar(fila, clave, () -> {
            String identificador = id(required(fila, clave));
            if (!ids.add(identificador)) throw new IllegalArgumentException("ID duplicado en CSV: " + identificador);
        }, errores);
        for (String campo : alumnos ? List.of("nombre", "apellidoPaterno", "apellidoMaterno", "iniciales") : List.of("nombre")) {
            comprobar(fila, campo, () -> {
                String valor = required(fila, campo);
                if (campo.equals("iniciales") && java.text.Normalizer.normalize(valor, java.text.Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "").replaceAll("[^A-Za-z0-9]", "").isBlank())
                    throw new IllegalArgumentException("Iniciales invalidas");
            }, errores);
        }
        comprobar(fila, "activo", () -> activo(fila), errores);
        if (!alumnos) {
            comprobar(fila, "horaSalida", () -> hora(value(fila, "horaSalida")), errores);
            return;
        }
        comprobar(fila, "fingerprintId", () -> {
            String huella = id(required(fila, "fingerprintId"));
            String identificador = value(fila, clave);
            if (identificador.matches("[0-9]{1,4}") && !huella.equals(Integer.toString(Integer.parseInt(identificador))))
                throw new IllegalArgumentException("fingerprintId debe ser igual a idAlumno");
        }, errores);
        comprobar(fila, "idGrupo", () -> {
            String grupo = id(required(fila, "idGrupo"));
            if (!grupos.contains(grupo)) throw new IllegalArgumentException("Grupo inexistente: " + grupo);
        }, errores);
        comprobar(fila, "codigoTutorHash", () -> {
            String codigo = required(fila, "codigoTutorHash");
            if (!codigo.matches("[0-9]{6}")) throw new IllegalArgumentException("codigoTutorHash debe tener 6 digitos");
            String identificador = value(fila, clave);
            if (identificador.matches("[0-9]{1,4}")) identificador = Integer.toString(Integer.parseInt(identificador));
            String propietario = codigos.putIfAbsent(codigo, identificador);
            if (propietario != null && !propietario.equals(identificador))
                throw new IllegalArgumentException("codigoTutorHash ya pertenece a otro alumno");
        }, errores);
        for (String campo : List.of("nombre", "parentesco", "telefono", "correo")) {
            comprobar(fila, "tutor." + campo, () -> {
                if (value(fila, "tutor." + campo).isBlank()
                        && value(fila, "tutor" + Character.toUpperCase(campo.charAt(0)) + campo.substring(1)).isBlank())
                    throw new IllegalArgumentException("Falta tutor." + campo);
            }, errores);
        }
    }

    static Map<String, Object> grupo(CSVRecord fila) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", required(fila, "nombre"));
        datos.put("activo", activo(fila));
        datos.put("horaSalida", hora(value(fila, "horaSalida")));
        return datos;
    }

    static Map<String, Object> alumno(CSVRecord fila, Set<String> grupos) {
        String alumnoId = id(required(fila, "idAlumno"));
        String fingerprint = id(required(fila, "fingerprintId"));
        if (!alumnoId.equals(fingerprint)) throw new IllegalArgumentException("fingerprintId debe ser igual a idAlumno");
        String grupoId = id(required(fila, "idGrupo"));
        if (!grupos.contains(grupoId)) throw new IllegalArgumentException("Grupo inexistente: " + grupoId);
        String codigo = required(fila, "codigoTutorHash");
        if (!codigo.matches("[0-9]{6}")) throw new IllegalArgumentException("codigoTutorHash debe tener 6 digitos");
        Map<String, Object> datos = new HashMap<>();
        for (String campo : List.of("nombre", "apellidoPaterno", "apellidoMaterno", "iniciales"))
            datos.put(campo, required(fila, campo));
        datos.put("iniciales", java.text.Normalizer.normalize((String) datos.get("iniciales"), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^A-Za-z0-9]", "").toUpperCase(java.util.Locale.ROOT));
        if (((String) datos.get("iniciales")).isBlank()) throw new IllegalArgumentException("Iniciales invalidas");
        datos.put("nombreCompleto", datos.get("nombre") + " " + datos.get("apellidoPaterno") + " " + datos.get("apellidoMaterno"));
        datos.put("idGrupo", grupoId);
        datos.put("fingerprintId", Long.parseLong(fingerprint));
        datos.put("codigoTutorHash", codigo);
        datos.put("activo", activo(fila));
        Map<String, Object> tutor = new HashMap<>();
        for (String campo : List.of("nombre", "parentesco", "telefono", "telefonoAlternativo", "correo")) {
            String valor = value(fila, "tutor." + campo);
            if (valor.isBlank()) valor = value(fila, "tutor" + Character.toUpperCase(campo.charAt(0)) + campo.substring(1));
            if (valor.isBlank() && !campo.equals("telefonoAlternativo"))
                throw new IllegalArgumentException("Falta tutor." + campo);
            tutor.put(campo, valor);
        }
        datos.put("tutor", tutor);
        return datos;
    }

    static String id(String valor) {
        if (!valor.matches("[0-9]{1,4}") || Integer.parseInt(valor) < 1 || Integer.parseInt(valor) > 1000)
            throw new IllegalArgumentException("ID fuera del rango 1..1000: " + valor);
        return Integer.toString(Integer.parseInt(valor));
    }

    static String hora(String valor) {
        if (valor.isBlank()) return "14:00";
        try {
            if (valor.matches("[0-9]{2}:[0-9]{2}")) return LocalTime.parse(valor).toString();
            BigDecimal fraccion = new BigDecimal(valor.replace(',', '.'));
            if (fraccion.signum() < 0 || fraccion.compareTo(BigDecimal.ONE) >= 0) throw new NumberFormatException();
            int minutos = fraccion.multiply(BigDecimal.valueOf(1440)).setScale(0, RoundingMode.HALF_UP).intValueExact();
            if (minutos >= 1440) throw new NumberFormatException();
            return LocalTime.of(minutos / 60, minutos % 60).toString();
        } catch (RuntimeException ex) { throw new IllegalArgumentException("horaSalida invalida; use HH:mm", ex); }
    }

    private static boolean activo(CSVRecord fila) {
        String valor = value(fila, "activo");
        if (valor.isBlank() || valor.equalsIgnoreCase("true")) return true;
        if (valor.equalsIgnoreCase("false")) return false;
        throw new IllegalArgumentException("activo debe ser true o false");
    }

    private static String value(CSVRecord fila, String campo) {
        return fila.isMapped(campo) && fila.isSet(campo) ? fila.get(campo).trim() : "";
    }

    private static String required(CSVRecord fila, String campo) {
        String valor = value(fila, campo);
        if (valor.isBlank()) throw new IllegalArgumentException("Falta " + campo);
        return valor;
    }

    public Map<String, Object> limpiar(String codigo, String confirmacion) throws Exception {
        autorizar(codigo);
        if (!"ELIMINAR_ALUMNOS_Y_GRUPOS".equals(confirmacion))
            throw new IllegalArgumentException("Confirme con ELIMINAR_ALUMNOS_Y_GRUPOS");
        Map<String, Object> resultado = new LinkedHashMap<>();
        for (String coleccion : List.of("alumnos", "grupo")) {
            int eliminados = 0;
            while (true) {
                var docs = firestore.collection(coleccion).limit(400).get().get().getDocuments();
                if (docs.isEmpty()) break;
                WriteBatch batch = firestore.batch();
                for (var doc : docs) batch.delete(doc.getReference());
                batch.commit().get();
                eliminados += docs.size();
            }
            resultado.put(coleccion, eliminados);
        }
        return resultado;
    }
}
