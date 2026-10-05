package mx.edu.cetis117.asistencia.controller;

import jakarta.validation.Valid;
import mx.edu.cetis117.asistencia.dto.AsistenciaResponse;
import mx.edu.cetis117.asistencia.dto.RegistroHuellaRequest;
import mx.edu.cetis117.asistencia.service.AsistenciaService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/asistencia")
@CrossOrigin(origins = "*")
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    public AsistenciaController(
            AsistenciaService asistenciaService
    ) {
        this.asistenciaService = asistenciaService;
    }

    @PostMapping("/registrar")
    public AsistenciaResponse registrar(
            @Valid @RequestBody RegistroHuellaRequest request
    ) throws Exception {

        return asistenciaService.registrar(
                request.fingerprintId()
        );
    }
}
