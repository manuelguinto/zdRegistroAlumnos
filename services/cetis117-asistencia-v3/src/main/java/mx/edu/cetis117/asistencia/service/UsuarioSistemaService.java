package mx.edu.cetis117.asistencia.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.stereotype.Service;

@Service
public class UsuarioSistemaService {

    private final Firestore firestore;

    public UsuarioSistemaService(
            Firestore firestore
    ) {
        this.firestore = firestore;
    }

    public DocumentSnapshot buscarPorCodigo(
            String codigoHash
    ) throws Exception {

        QuerySnapshot query = firestore
                .collection("usuariosSistema")
                .whereEqualTo("codigoHash", codigoHash)
                .whereEqualTo("activo", true)
                .limit(1)
                .get()
                .get();

        return query.isEmpty()
                ? null
                : query.getDocuments().get(0);
    }

    public DocumentSnapshot buscarPorIdUsuario(
            String idUsuario
    ) throws Exception {

        QuerySnapshot query = firestore
                .collection("usuariosSistema")
                .whereEqualTo("idUsuario", idUsuario)
                .whereEqualTo("activo", true)
                .limit(1)
                .get()
                .get();

        return query.isEmpty()
                ? null
                : query.getDocuments().get(0);
    }
}
