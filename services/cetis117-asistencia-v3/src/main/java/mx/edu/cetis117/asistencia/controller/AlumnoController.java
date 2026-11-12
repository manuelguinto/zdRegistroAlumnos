package mx.edu.cetis117.asistencia.controller;

import mx.edu.cetis117.asistencia.dto.ConsultaAlumnoResponse;
import mx.edu.cetis117.asistencia.service.AlumnoService;
import mx.edu.cetis117.asistencia.service.ConsultaService;
import mx.edu.cetis117.asistencia.service.ImportacionService;
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
    private final ImportacionService importacionService;

    public AlumnoController(
            AlumnoService alumnoService,
            ConsultaService consultaService,
            ImportacionService importacionService
    ) {
        this.alumnoService = alumnoService;
        this.consultaService = consultaService;
        this.importacionService = importacionService;
    }

    @PostMapping(
            value = "/importar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Map<String, Object> importar(
            @RequestPart("file") MultipartFile file,
            @org.springframework.web.bind.annotation.RequestHeader("X-Codigo-Sistema") String codigo
    ) throws Exception {

        return importacionService.importar(file, "alumnos", codigo);
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
