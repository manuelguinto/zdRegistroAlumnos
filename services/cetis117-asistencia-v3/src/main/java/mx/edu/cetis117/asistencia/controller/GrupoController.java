package mx.edu.cetis117.asistencia.controller;

import mx.edu.cetis117.asistencia.dto.GrupoResponse;
import mx.edu.cetis117.asistencia.service.GrupoService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/grupos")
@CrossOrigin(origins = "*")
public class GrupoController {

    private final GrupoService grupoService;

    public GrupoController(
            GrupoService grupoService
    ) {
        this.grupoService = grupoService;
    }

    @GetMapping
    public List<GrupoResponse> listar(
            @RequestParam String codigo
    ) throws Exception {

        return grupoService.listar(codigo);
    }
}
