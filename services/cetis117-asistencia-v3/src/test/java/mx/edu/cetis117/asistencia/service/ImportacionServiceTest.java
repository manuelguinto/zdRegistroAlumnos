package mx.edu.cetis117.asistencia.service;

import org.junit.jupiter.api.Test;
import org.apache.commons.csv.*;
import java.util.*;
import java.io.StringReader;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.google.cloud.firestore.*;
import com.google.api.core.ApiFutures;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.charset.StandardCharsets;

class ImportacionServiceTest {
    private CSVRecord fila(String csv) throws Exception {
        try (var parser = CSVFormat.DEFAULT.builder().setHeader().get().parse(new StringReader(csv))) {
            return parser.getRecords().getFirst();
        }
    }

    @Test void interpretaHoraExcelYValidaRangos() {
        assertEquals("14:00", ImportacionService.hora("0,583333333333333"));
        assertEquals("14:00", ImportacionService.hora(""));
        assertEquals("07:30", ImportacionService.hora("07:30"));
        assertThrows(IllegalArgumentException.class, () -> ImportacionService.hora("24:00"));
        assertThrows(IllegalArgumentException.class, () -> ImportacionService.id("1001"));
    }

    @Test void leeTutorDePlantillaYValidaRelacionHuellaGrupo() throws Exception {
        String csv = "idAlumno,nombre,apellidoPaterno,apellidoMaterno,idGrupo,fingerprintId,iniciales,codigoTutorHash,tutor.nombre,tutor.parentesco,tutor.telefono,tutor.correo,activo\n"
                + "1,Ana,Lopez,Perez,2,1,ALP,012345,Tutor,Madre,123,a@b.com,false";
        var datos = ImportacionService.alumno(fila(csv), Set.of("2"));
        assertEquals(false, datos.get("activo"));
        assertEquals("012345", datos.get("codigoTutorHash"));
        assertEquals("Tutor", ((Map<?, ?>) datos.get("tutor")).get("nombre"));
        assertThrows(IllegalArgumentException.class, () -> ImportacionService.alumno(fila(csv), Set.of()));
        assertThrows(IllegalArgumentException.class, () -> ImportacionService.alumno(fila(csv.replace(",2,1,", ",2,3,")), Set.of("2")));
    }

    @Test void importaBomRechazaDuplicadosYConservaHorario() throws Exception {
        Firestore db = mock(Firestore.class);
        UsuarioSistemaService usuarios = mock(UsuarioSistemaService.class);
        when(usuarios.buscarPorCodigo("admin")).thenReturn(mock(DocumentSnapshot.class));
        CollectionReference collection = mock(CollectionReference.class);
        when(db.collection("grupo")).thenReturn(collection);
        QuerySnapshot snapshot = mock(QuerySnapshot.class);
        when(collection.get()).thenReturn(ApiFutures.immediateFuture(snapshot));
        QueryDocumentSnapshot existente = mock(QueryDocumentSnapshot.class);
        when(existente.getId()).thenReturn("1");
        when(existente.get("horaSalida")).thenReturn("13:00");
        when(snapshot.getDocuments()).thenReturn(List.of(existente));
        DocumentReference ref = mock(DocumentReference.class);
        when(collection.document("1")).thenReturn(ref);
        WriteBatch batch = mock(WriteBatch.class);
        when(db.batch()).thenReturn(batch);
        when(batch.commit()).thenReturn(ApiFutures.immediateFuture(List.of()));
        var file = new MockMultipartFile("file", "grupos.csv", "text/csv",
                "\uFEFFidGrupo,nombre,horaSalida\n1,Primero,14:00\n1,Duplicado,14:00\n1001,Invalido,14:00".getBytes(StandardCharsets.UTF_8));
        var service = new ImportacionService(db, usuarios);
        var fallo = assertThrows(ValidacionImportacionException.class, () -> service.importar(file, "grupo", "admin"));
        assertEquals(2, fallo.getErrores().size());
        verify(db, never()).batch();
        var validFile = new MockMultipartFile("file", "grupos.csv", "text/csv",
                "\uFEFFidGrupo,nombre,horaSalida\n1,Primero actualizado,14:00".getBytes(StandardCharsets.UTF_8));
        var resultado = service.importar(validFile, "grupo", "admin");
        assertEquals(1, resultado.get("actualizados"));
        assertEquals(0, resultado.get("rechazados"));
        verify(batch).set(eq(ref), argThat((Map<String, Object> d) -> !d.containsKey("horaSalida")), any(SetOptions.class));
        verify(batch).commit();
    }

    @Test void acumulaTodosLosCamposFaltantesEnTodasLasFilas() throws Exception {
        List<Map<String, Object>> errores = new ArrayList<>();
        String csv = "idAlumno,nombre,apellidoPaterno,apellidoMaterno,idGrupo,fingerprintId,iniciales,codigoTutorHash,tutor.nombre,tutor.parentesco,tutor.telefono,tutor.correo\n"
                + "1,,,,,,,,,,,\n2,,,,,,,,,,,";
        try (var parser = CSVFormat.DEFAULT.builder().setHeader().get().parse(new StringReader(csv))) {
            for (var registro : parser)
                ImportacionService.validarFila(registro, true, Set.of(), new HashSet<>(), new HashMap<>(), errores);
        }
        assertEquals(22, errores.size());
        assertEquals(11, errores.stream().filter(e -> e.get("fila").equals(2L)).count());
        assertEquals(11, errores.stream().filter(e -> e.get("fila").equals(3L)).count());
    }

    @Test void alumnosInvalidosNoEscribenNiLasFilasValidas() throws Exception {
        Firestore db = mock(Firestore.class);
        UsuarioSistemaService usuarios = mock(UsuarioSistemaService.class);
        when(usuarios.buscarPorCodigo("admin")).thenReturn(mock(DocumentSnapshot.class));
        CollectionReference alumnos = mock(CollectionReference.class), grupos = mock(CollectionReference.class);
        when(db.collection("alumnos")).thenReturn(alumnos);
        when(db.collection("grupo")).thenReturn(grupos);
        QuerySnapshot vacio = mock(QuerySnapshot.class), snapshotGrupos = mock(QuerySnapshot.class);
        when(vacio.getDocuments()).thenReturn(List.of());
        when(alumnos.get()).thenReturn(ApiFutures.immediateFuture(vacio));
        when(grupos.get()).thenReturn(ApiFutures.immediateFuture(snapshotGrupos));
        QueryDocumentSnapshot grupo = mock(QueryDocumentSnapshot.class);
        when(grupo.getId()).thenReturn("1");
        when(snapshotGrupos.getDocuments()).thenReturn(List.of(grupo));
        String cabecera = "idAlumno,nombre,apellidoPaterno,apellidoMaterno,idGrupo,fingerprintId,iniciales,codigoTutorHash,tutor.nombre,tutor.parentesco,tutor.telefono,tutor.correo\n";
        var file = new MockMultipartFile("file", (cabecera
                + "1,Ana,Lopez,Perez,1,1,ALP,012345,Tutor,Madre,123,a@b.com\n"
                + "2,,Lopez,Perez,1,2,BLP,012346,,Madre,123,a@b.com").getBytes(StandardCharsets.UTF_8));
        var ex = assertThrows(ValidacionImportacionException.class,
                () -> new ImportacionService(db, usuarios).importar(file, "alumnos", "admin"));
        assertEquals(2, ex.getErrores().size());
        verify(db, never()).batch();
    }

    @Test void limpiezaRequiereAccesoYConfirmacionAntesDeBorrar() throws Exception {
        Firestore db = mock(Firestore.class);
        UsuarioSistemaService usuarios = mock(UsuarioSistemaService.class);
        var service = new ImportacionService(db, usuarios);
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.limpiar("no", "ELIMINAR_ALUMNOS_Y_GRUPOS"));
        when(usuarios.buscarPorCodigo("admin")).thenReturn(mock(DocumentSnapshot.class));
        assertThrows(IllegalArgumentException.class, () -> service.limpiar("admin", "no"));
        verifyNoInteractions(db);
    }
}
