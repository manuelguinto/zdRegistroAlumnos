# CONTEXTO ACTUALIZADO — CETIS 117 / CONTROL DE ASISTENCIA

**Fecha:** 2026-10-05

Este archivo resume lo trabajado en esta conversación para continuar el proyecto sin perder decisiones técnicas, estructura, versiones ni pendientes.

## 1. Objetivo del proyecto

Sistema escolar de **Control de Asistencia** para CETIS 117 con:

- Registro de entrada y salida por huella digital.
- ESP32 + lector GM60.
- Backend REST Java 21 + Spring Boot.
- Google Cloud Run.
- Firestore.
- Frontend React + Tailwind.
- Firebase Hosting.
- Consulta para padres/tutores.
- Módulo administrativo para Dirección / Control Escolar.
- Salidas normales, anticipadas y generales.

El título visible del portal debe conservarse como:

**Control de Asistencia**

## 2. Hardware y flujo

Hardware previsto:

- ESP32.
- Sensor GM60 (capacidad aproximada 1000 huellas).
- Aproximadamente 400 alumnos.
- Una huella por alumno.
- Pantalla LCD.
- Regulador DC-DC.

Flujo:

1. Alumno coloca huella.
2. GM60 devuelve `fingerprintId`.
3. ESP32 envía el ID al backend.
4. Backend valida alumno y reglas de asistencia.
5. Se registra entrada/salida en Firestore.
6. Se devuelve mensaje para LCD.

Importante: Firestore no guarda una fotografía de la huella; guarda el `fingerprintId`. La plantilla biométrica queda en el lector.

## 3. Google Cloud / Firestore

- Project ID backend: `zona-d`
- Firestore database ID: `zona-d`
- Cloud Run service: `zdregistroalumnos`
- Región: `us-central1`
- URL: `https://zdregistroalumnos-482614074090.us-central1.run.app`

Configuración explícita de Firestore:

```java
return FirestoreOptions.newBuilder()
        .setProjectId("zona-d")
        .setDatabaseId("zona-d")
        .build()
        .getService();
```

No se desean variables de entorno para Project ID / Database ID.

`application.properties`:

```properties
server.port=${PORT:8080}
server.address=0.0.0.0
```

## 4. Colecciones Firestore

Principales:

- `alumnos`
- `grupo`
- `registro`
- `autorizacionesSalida`
- `usuariosSistema`
- `systemParams`

### alumnos

Campos conceptuales:

```json
{
  "idAlumno": "AL-0001",
  "nombre": "Manuel Damian",
  "apellidoPaterno": "Guinto",
  "apellidoMaterno": "Méndez",
  "nombreCompleto": "Manuel Damian Guinto Méndez",
  "idGrupo": "3",
  "fingerprintId": 1,
  "iniciales": "MSGG",
  "codigoTutorHash": "1234",
  "activo": true
}
```

Reglas:

- Usar directamente `iniciales`.
- No calcular iniciales desde el nombre.
- Si falta `nombreCompleto`, construirlo con nombre + apellidos.

### grupo

```json
{
  "idGrupo": "3",
  "nombre": "1° B",
  "activo": true,
  "horaSalida": "14:00"
}
```

### systemParams

```json
{
  "minutosToleranciaSalida": 20,
  "requiereAutorizacionSalida": true
}
```

## 5. Lógica de asistencia

Endpoint:

```text
POST /api/asistencia/registrar
```

Body:

```json
{"fingerprintId": 1}
```

Reglas:

- Primer escaneo: entrada (`EntradaRegistrada`).
- Segundo escaneo: salida si está permitida.
- Cuando entrada y salida ya existen, el registro queda sellado.
- Un escaneo adicional el mismo día devuelve: `Debe esperar al día de mañana para poder registrar su entrada nuevamente`.
- Al día siguiente se crea un nuevo documento.
- ID conceptual: `fingerprintId_yyyy-MM-dd`.

## 6. Tipos de salida y etiquetas del portal

### Salida normal

Cuando se cumple la hora del grupo con tolerancia.

Etiqueta visible:

```text
Completado
```

### Salida anticipada

Cuando la salida fue autorizada por:

- `ALUMNO`
- `GRUPO`

Etiqueta visible:

```text
Salida anticipada
```

No debe mostrarse como “Completado”.

### Salida general

Autorización para toda la escuela.

Etiqueta visible:

```text
Salida General
```

## 7. Autorizaciones

Endpoint:

```text
POST /api/autorizaciones
```

### ALUMNO

```json
{
  "tipo": "ALUMNO",
  "iniciales": "MSGG",
  "idUsuarioAutoriza": "TMARTINEZ",
  "motivo": "Solicitud del tutor"
}
```

Solo una por alumno por día.

### GRUPO

```json
{
  "tipo": "GRUPO",
  "idGrupo": "3",
  "idUsuarioAutoriza": "TMARTINEZ",
  "motivo": "Salida anticipada del grupo"
}
```

Solo una por grupo por día.

### GENERAL

```json
{
  "tipo": "GENERAL",
  "horaSalidaGeneral": "12:30",
  "idUsuarioAutoriza": "TMARTINEZ",
  "motivo": "Reunión docente"
}
```

Reglas:

- Solo una salida general activa por día.
- No actualizar masivamente todos los registros.
- Si hubo entrada y no salida real, mostrar virtualmente `horaSalidaGeneral`.
- Estado visual: `SalidaGeneral` / “Salida General”.
- Mostrar quién autorizó y motivo.
- Si ya existe salida real, conservarla.
- Si no hubo entrada, no inventar salida.
- Si el alumno escanea después de una salida general, no modificar `registro`.
- Si ya existe GENERAL, bloquear nuevas autorizaciones ALUMNO y GRUPO.

## 8. Estados de grupo

Prioridad:

1. `SALIDA_GENERAL`
2. `SALIDA_POR_AUTORIZACION`
3. `SALIDA_POR_HORARIO`
4. `PENDIENTE`

Respuesta conceptual:

```java
public record GrupoResponse(
    String idGrupo,
    String nombre,
    String horaSalida,
    Boolean activo,
    String estadoSalida,
    Boolean salidaHabilitada,
    String horaSalidaEfectiva
) {}
```

## 9. Endpoints principales

```text
POST /api/asistencia/registrar
POST /api/autorizaciones
POST /api/alumnos/importar
GET  /api/alumnos/consulta
DELETE /api/registros/limpieza
GET /api/acceso/validar?codigo=...
GET /api/grupos?codigo=...
GET /api/ping
```

## 10. Frontend React

Stack:

- React.
- Vite.
- Tailwind.
- Responsive.

Estilo:

- CETIS 117.
- Guinda brillante (`#941F32`).
- Guinda oscuro (`#741827`).
- Dorado.
- Crema.
- Blanco.

### Vista tutor

Muestra:

- datos del alumno.
- entradas.
- salidas.
- último registro.
- bitácora.
- tutor.
- fecha.
- entrada.
- salida.
- estado.
- autorizado por.
- motivo.

Estados visibles:

- `Completado`
- `Salida anticipada`
- `Salida General`
- `EntradaRegistrada`

### Vista administrativa

Incluye:

- listado de alumnos.
- filtros.
- consulta de registro.
- autorización ALUMNO.
- autorización GRUPO.
- salida GENERAL.
- estado de grupos.

Columnas principales:

- Alumno.
- Iniciales.
- Grupo.
- Acciones.

Mantener botón **Cerrar sesión**.

## 11. Versiones trabajadas

### Backend

- v11: estados de grupos.
- v12: salida GENERAL.
- v13: estado `SalidaGeneral` en consulta.
- v14: distinción de `SalidaAnticipada`.

Archivo generado recientemente:

```text
cetis117-asistencia-v14.zip
```

### Frontend

- v10: botón cerrar sesión restaurado.
- v11: salida general.
- v12: salida general + bloqueo de autorizaciones.
- v13: etiqueta `Salida anticipada`.

Archivo generado recientemente:

```text
cetis117-registro-alumnos-react-v13.zip
```

IMPORTANTE: el proyecto frontend real usado localmente está en:

```text
C:\ZONAD\zdRegistroAlumnos\fe\cetis117-registro-alumnos-react-v1
```

Aunque los ZIP generados tengan números mayores, Firebase debe apuntar al folder real elegido por el usuario.

## 12. Despliegue backend a Cloud Run

Desde la carpeta que contiene `pom.xml` y `src`:

```powershell
gcloud run deploy zdregistroalumnos `
  --source . `
  --region us-central1 `
  --project zona-d `
  --allow-unauthenticated
```

## 13. Firebase Hosting

Proyecto Firebase Hosting:

- Project ID: `zdcontrolasistencia`
- Nombre: `zdControlAsistencia`
- Project Number: `938892389655`

El frontend local real está en:

```text
C:\ZONAD\zdRegistroAlumnos\fe\cetis117-registro-alumnos-react-v1
```

`firebase.json` debe apuntar a:

```text
fe/cetis117-registro-alumnos-react-v1/dist
```

Build:

```powershell
cd C:/ZONAD/zdRegistroAlumnos/fe/cetis117-registro-alumnos-react-v1
npm run build
```

Volver a raíz:

```powershell
cd ../..
```

Deploy:

```powershell
firebase deploy --only hosting --project zdcontrolasistencia
```

No es necesario repetir `firebase init` si `firebase.json` ya está correcto.

## 14. Cold start de Cloud Run

Problema observado:

- Cuando Cloud Run escala a cero, la primera petición puede tardar alrededor de 19 segundos por el arranque de Spring Boot.

Estrategia elegida:

- Usar Google Cloud Scheduler.
- Hacer ping cada 5 minutos.
- Horario: 06:00 a 15:00.

## 15. Endpoint /api/ping

Se decidió crear un endpoint controlado que no toque Firestore:

```java
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
```

URL:

```text
https://zdregistroalumnos-482614074090.us-central1.run.app/api/ping
```

Respuesta:

```json
{
  "status": "OK",
  "service": "CETIS117 Asistencia"
}
```

Este endpoint:

- no consulta alumnos.
- no consulta grupos.
- no consulta autorizaciones.
- no consulta parámetros.
- no escribe en Firestore.
- responde solo en memoria.

## 16. Cloud Scheduler

Habilitar servicio:

```powershell
gcloud services enable cloudscheduler.googleapis.com --project zona-d
```

### Cada 5 minutos de 06:00 a 14:55

```powershell
gcloud scheduler jobs create http mantener-zdregistroalumnos-activo `
  --location=us-central1 `
  --schedule="*/5 6-14 * * *" `
  --time-zone="America/Mexico_City" `
  --uri="https://zdregistroalumnos-482614074090.us-central1.run.app/api/ping" `
  --http-method=GET `
  --project=zona-d
```

### Último ping a las 15:00

```powershell
gcloud scheduler jobs create http mantener-zdregistroalumnos-1500 `
  --location=us-central1 `
  --schedule="0 15 * * *" `
  --time-zone="America/Mexico_City" `
  --uri="https://zdregistroalumnos-482614074090.us-central1.run.app/api/ping" `
  --http-method=GET `
  --project=zona-d
```

Probar manualmente:

```powershell
gcloud scheduler jobs run mantener-zdregistroalumnos-activo `
  --location=us-central1 `
  --project=zona-d
```

Consultar job:

```powershell
gcloud scheduler jobs describe mantener-zdregistroalumnos-activo `
  --location=us-central1 `
  --project=zona-d
```

Este mecanismo reduce la probabilidad de cold start, pero Cloud Run aún puede reciclar instancias.

Alternativa más fuerte:

```powershell
gcloud run services update zdregistroalumnos `
  --min=1 `
  --region=us-central1 `
  --project=zona-d
```

Pero `min-instances=1` puede costar más al mantener una instancia activa.

## 17. Actuator health

Endpoint existente:

```text
/actuator/health
```

Sirve para indicar si Spring Boot está vivo.

Normalmente devuelve:

```json
{"status":"UP"}
```

Se prefirió `/api/ping` porque sabemos exactamente qué código ejecuta y garantiza que no se toca Firestore.

## 18. Presentación para Dirección

Se generó una presentación HTML:

```text
Presentacion_Control_Asistencia_CETIS117.html
```

Incluye:

- objetivo.
- flujo con huella.
- portal de acceso.
- vista para padres.
- módulo administrativo.
- salidas normales.
- salidas anticipadas.
- salida general.
- arquitectura.
- privacidad.
- beneficios.
- prueba piloto.

## 19. Decisiones que deben conservarse

1. Título: **Control de Asistencia**.
2. Usar `iniciales` almacenadas; no calcularlas.
3. Salida autorizada ALUMNO/GRUPO: **Salida anticipada**.
4. Salida general: **Salida General**.
5. Salida normal: **Completado**.
6. GENERAL no actualiza masivamente todos los registros.
7. Una salida real tiene prioridad sobre una salida general virtual.
8. `/api/ping` no toca Firestore.
9. Scheduler en proyecto `zona-d`.
10. Firebase Hosting en `zdcontrolasistencia`.
11. Frontend local actual: `fe/cetis117-registro-alumnos-react-v1`.
12. Backend se despliega desde la carpeta con `pom.xml`.
13. Mantener botón **Cerrar sesión**.
14. Mantener diseño guinda/dorado/crema CETIS 117.

## 20. Siguientes pasos

1. Incorporar `PingController.java` al backend actual.
2. Redesplegar Cloud Run.
3. Probar `/api/ping`.
4. Crear Cloud Scheduler.
5. Verificar ejecuciones cada 5 minutos.
6. Medir si disminuye el cold start de ~19 segundos.
7. Validar en frontend:
   - `Completado`
   - `Salida anticipada`
   - `Salida General`
8. Continuar integración GM60 + ESP32 + backend + Firestore + portal.

## 21. Comandos rápidos

### Backend Cloud Run

```powershell
gcloud run deploy zdregistroalumnos `
  --source . `
  --region us-central1 `
  --project zona-d `
  --allow-unauthenticated
```

### Frontend build

```powershell
cd C:/ZONAD/zdRegistroAlumnos/fe/cetis117-registro-alumnos-react-v1
npm run build
```

### Firebase deploy

```powershell
cd ../..
firebase deploy --only hosting --project zdcontrolasistencia
```

### Ping

```powershell
curl.exe "https://zdregistroalumnos-482614074090.us-central1.run.app/api/ping"
```

### Scheduler manual

```powershell
gcloud scheduler jobs run mantener-zdregistroalumnos-activo `
  --location=us-central1 `
  --project=zona-d
```

---

**Fin del contexto actualizado.**
