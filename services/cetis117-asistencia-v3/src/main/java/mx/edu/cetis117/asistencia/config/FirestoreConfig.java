package mx.edu.cetis117.asistencia.config;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FirestoreConfig {

    @Bean
    public Firestore firestore() {
        return FirestoreOptions.newBuilder()
                .setProjectId("zona-d")
                .setDatabaseId("zona-d")
                .build()
                .getService();
    }
}
