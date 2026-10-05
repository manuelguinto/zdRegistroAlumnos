package mx.edu.cetis117.asistencia.controller;

import mx.edu.cetis117.asistencia.service.LimpiezaService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/registros")
@CrossOrigin(origins = "*")
public class RegistroController {

    private final LimpiezaService limpiezaService;

    public RegistroController(
            LimpiezaService limpiezaService
    ) {
        this.limpiezaService = limpiezaService;
    }

    @DeleteMapping("/limpieza")
    public Map<String, Object> limpiar(
            @RequestParam(defaultValue = "7") int dias
    ) throws Exception {

        int eliminados = limpiezaService.limpiar(dias);

        return Map.of(
                "eliminados", eliminados,
                "diasConservados", dias
        );
    }
}
