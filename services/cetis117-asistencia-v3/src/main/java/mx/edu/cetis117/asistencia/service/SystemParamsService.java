package mx.edu.cetis117.asistencia.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import org.springframework.stereotype.Service;

@Service
public class SystemParamsService {

    private static final int DEFAULT_MINUTOS_TOLERANCIA = 20;

    private final Firestore firestore;

    public SystemParamsService(Firestore firestore) {
        this.firestore = firestore;
    }

    public int getMinutosToleranciaSalida() {
        try {
            QuerySnapshot snapshot = firestore
                    .collection("systemParams")
                    .limit(1)
                    .get()
                    .get();

            if (snapshot.isEmpty()) {
                return DEFAULT_MINUTOS_TOLERANCIA;
            }

            DocumentSnapshot doc = snapshot.getDocuments().get(0);
            Long minutos = doc.getLong("minutosToleranciaSalida");

            if (minutos == null || minutos < 0 || minutos > 1440) {
                return DEFAULT_MINUTOS_TOLERANCIA;
            }

            return minutos.intValue();

        } catch (Exception e) {
            return DEFAULT_MINUTOS_TOLERANCIA;
        }
    }
}
