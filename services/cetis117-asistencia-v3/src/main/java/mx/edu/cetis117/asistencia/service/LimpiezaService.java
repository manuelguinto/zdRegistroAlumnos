package mx.edu.cetis117.asistencia.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import mx.edu.cetis117.asistencia.util.FirestoreUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class LimpiezaService {

    private static final ZoneId ZONE_ID = ZoneId.of("America/Mexico_City");

    private final Firestore firestore;

    public LimpiezaService(Firestore firestore) {
        this.firestore = firestore;
    }

    public int limpiar(int dias) throws Exception {

        LocalDate limite =
                LocalDate.now(ZONE_ID).minusDays(dias);

        QuerySnapshot snapshot = firestore
                .collection("registro")
                .get()
                .get();

        int eliminados = 0;
        WriteBatch batch = firestore.batch();

        for (DocumentSnapshot doc : snapshot.getDocuments()) {

            Object entrada = doc.get("fechaHoraEntrada");

            if (entrada == null) {
                continue;
            }

            LocalDate fechaRegistro = FirestoreUtil
                    .toInstant(entrada)
                    .atZone(ZONE_ID)
                    .toLocalDate();

            if (fechaRegistro.isBefore(limite)) {

                batch.delete(doc.getReference());
                eliminados++;

                if (eliminados % 400 == 0) {
                    batch.commit().get();
                    batch = firestore.batch();
                }
            }
        }

        if (eliminados % 400 != 0) {
            batch.commit().get();
        }

        return eliminados;
    }
}
