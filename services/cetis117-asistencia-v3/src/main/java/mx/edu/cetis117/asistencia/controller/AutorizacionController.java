package mx.edu.cetis117.asistencia.controller;

import jakarta.validation.Valid;
import mx.edu.cetis117.asistencia.dto.CrearAutorizacionRequest;
import mx.edu.cetis117.asistencia.service.AutorizacionService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/autorizaciones")
@CrossOrigin(origins = "*")
public class AutorizacionController {

    private final AutorizacionService autorizacionService;

    public AutorizacionController(
            AutorizacionService autorizacionService
    ) {
        this.autorizacionService = autorizacionService;
    }

    @PostMapping
    public Map<String, Object> crear(
            @Valid @RequestBody CrearAutorizacionRequest request
    ) throws Exception {

        return autorizacionService.crear(request);
    }
}
