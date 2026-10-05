package mx.edu.cetis117.asistencia.util;

import com.google.cloud.Timestamp;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public final class FirestoreUtil {

    private static final DateTimeFormatter HORA_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    private FirestoreUtil() {
    }

    public static Instant toInstant(
            Object value
    ) {
        if (value == null) {
            return null;
        }

        if (value instanceof Timestamp timestamp) {
            return timestamp
                    .toDate()
                    .toInstant();
        }

        if (value instanceof Date date) {
            return date.toInstant();
        }

        if (value instanceof String text) {
            return OffsetDateTime
                    .parse(text)
                    .toInstant();
        }

        throw new IllegalArgumentException(
                "Tipo de fecha no soportado: "
                        + value.getClass()
        );
    }

    public static String hora(
            Object value,
            ZoneId zoneId
    ) {
        Instant instant = toInstant(value);

        if (instant == null) {
            return null;
        }

        return instant
                .atZone(zoneId)
                .format(HORA_FORMATTER);
    }

    public static String fecha(
            Object value,
            ZoneId zoneId
    ) {
        Instant instant = toInstant(value);

        if (instant == null) {
            return null;
        }

        return instant
                .atZone(zoneId)
                .toLocalDate()
                .toString();
    }
}
