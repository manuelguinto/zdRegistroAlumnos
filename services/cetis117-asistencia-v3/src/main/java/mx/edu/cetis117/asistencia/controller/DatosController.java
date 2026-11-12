package mx.edu.cetis117.asistencia.controller;

import mx.edu.cetis117.asistencia.service.ImportacionService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class DatosController {
    private final ImportacionService service;

    public DatosController(ImportacionService service) { this.service = service; }

    @PostMapping(value = "/api/grupos/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> grupos(@RequestPart("file") MultipartFile file,
            @RequestHeader("X-Codigo-Sistema") String codigo) throws Exception {
        return service.importar(file, "grupo", codigo);
    }

    @DeleteMapping("/api/datos/limpieza")
    public Map<String, Object> limpiar(@RequestHeader("X-Codigo-Sistema") String codigo,
            @RequestParam String confirmacion) throws Exception {
        return service.limpiar(codigo, confirmacion);
    }
}
