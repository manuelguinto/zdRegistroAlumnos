package mx.edu.cetis117.asistencia.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class PingController {

    @GetMapping("/api/ping")
    public Map<String, String> ping() {
        return Map.of(
                "status", "OK",
                "service", "CETIS117 Asistencia"
        );
    }
}
