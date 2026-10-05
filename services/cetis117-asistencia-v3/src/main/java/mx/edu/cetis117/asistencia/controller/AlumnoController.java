package mx.edu.cetis117.asistencia.controller;

import mx.edu.cetis117.asistencia.dto.ConsultaAlumnoResponse;
import mx.edu.cetis117.asistencia.service.AlumnoService;
import mx.edu.cetis117.asistencia.service.ConsultaService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alumnos")
@CrossOrigin(origins = "*")
public class AlumnoController {

    private final AlumnoService alumnoService;
    private final ConsultaService consultaService;

    public AlumnoController(
            AlumnoService alumnoService,
            ConsultaService consultaService
    ) {
        this.alumnoService = alumnoService;
        this.consultaService = consultaService;
    }

    @PostMapping(
            value = "/importar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Map<String, Object> importar(
            @RequestPart("file") MultipartFile file
    ) throws Exception {

        int registrosImportados =
                alumnoService.importarCsv(file);

        return Map.of(
                "registrosImportados",
                registrosImportados
        );
    }

    @GetMapping("/consulta")
    public List<ConsultaAlumnoResponse> consulta(
            @RequestParam String codigo,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String idGrupo,
            @RequestParam(required = false) String iniciales
    ) throws Exception {

        return consultaService.consultar(
                codigo,
                nombre,
                idGrupo,
                iniciales
        );
    }
}
