package mx.edu.cetis117.asistencia.controller;

import mx.edu.cetis117.asistencia.dto.AccesoResponse;
import mx.edu.cetis117.asistencia.service.AccesoService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/acceso")
@CrossOrigin(origins = "*")
public class AccesoController {

    private final AccesoService accesoService;

    public AccesoController(
            AccesoService accesoService
    ) {
        this.accesoService = accesoService;
    }

    @GetMapping("/validar")
    public AccesoResponse validar(
            @RequestParam String codigo
    ) throws Exception {

        return accesoService.validar(codigo);
    }
}
