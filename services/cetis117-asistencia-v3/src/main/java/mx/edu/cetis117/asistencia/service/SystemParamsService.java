package mx.edu.cetis117.asistencia.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.stereotype.Service;

@Service
public class SystemParamsService {

    private static final int DEFAULT_MINUTOS_TOLERANCIA = 20;
    private static final boolean DEFAULT_REQUIERE_AUTORIZACION_SALIDA = true;

    private final Firestore firestore;

    public SystemParamsService(Firestore firestore) {
        this.firestore = firestore;
    }

    public int getMinutosToleranciaSalida() {
        try {
            DocumentSnapshot documento = getDocumentoParametros();

            if (documento == null) {
                return DEFAULT_MINUTOS_TOLERANCIA;
            }

            Long minutos = documento.getLong("minutosToleranciaSalida");

            if (minutos == null || minutos < 0 || minutos > 1440) {
                return DEFAULT_MINUTOS_TOLERANCIA;
            }

            return minutos.intValue();

        } catch (Exception exception) {
            return DEFAULT_MINUTOS_TOLERANCIA;
        }
    }

    public boolean getRequiereAutorizacionSalida() {
        try {
            DocumentSnapshot documento = getDocumentoParametros();

            if (documento == null) {
                return DEFAULT_REQUIERE_AUTORIZACION_SALIDA;
            }

            Boolean requiereAutorizacion =
                    documento.getBoolean("requiereAutorizacionSalida");

            return requiereAutorizacion != null
                    ? requiereAutorizacion
                    : DEFAULT_REQUIERE_AUTORIZACION_SALIDA;

        } catch (Exception exception) {
            return DEFAULT_REQUIERE_AUTORIZACION_SALIDA;
        }
    }

    private DocumentSnapshot getDocumentoParametros() throws Exception {
        QuerySnapshot snapshot = firestore
                .collection("systemParams")
                .limit(1)
                .get()
                .get();

        if (snapshot.isEmpty()) {
            return null;
        }

        return snapshot.getDocuments().get(0);
    }
}
