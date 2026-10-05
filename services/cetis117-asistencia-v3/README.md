# CETIS 117 - Asistencia por huella v5

Versión corregida para el mismo proyecto/base Firestore de Zona D.

## Firestore

La conexión usa directamente:

- Project ID: `zona-d`
- Database ID: `zona-d`

No necesita `GOOGLE_CLOUD_PROJECT`, `FIRESTORE_DATABASE_ID`,
`SYSTEM_PARAMS_DOCUMENT_ID` ni `APP_ZONE_ID`.

`systemParams` sigue siendo una colección de Firestore de negocio y se consulta
directamente para obtener `minutosToleranciaSalida`.

Si no existe el parámetro o falla la lectura, se usa 20 minutos como respaldo.

## Ejecutar localmente

### 1. Autenticación local con Google

```powershell
gcloud auth application-default login
gcloud config set project zona-d
```

### 2. Compilar

```powershell
mvn clean compile
```

### 3. Ejecutar

```powershell
mvn spring-boot:run
```

Servidor:
`http://localhost:8080`

## Prueba de asistencia

```powershell
Invoke-RestMethod `
  -Method POST `
  -Uri "http://localhost:8080/api/asistencia/registrar" `
  -ContentType "application/json" `
  -Body '{"fingerprintId":1}'
```

## Crear autorización por alumno

```powershell
Invoke-RestMethod `
  -Method POST `
  -Uri "http://localhost:8080/api/autorizaciones" `
  -ContentType "application/json" `
  -Body '{
    "tipo":"ALUMNO",
    "fingerprintId":1,
    "idUsuarioAutoriza":"TMARTINEZ",
    "motivo":"Solicitud del tutor"
  }'
```

## Crear autorización por grupo

```powershell
Invoke-RestMethod `
  -Method POST `
  -Uri "http://localhost:8080/api/autorizaciones" `
  -ContentType "application/json" `
  -Body '{
    "tipo":"GRUPO",
    "idGrupo":"3",
    "idUsuarioAutoriza":"TMARTINEZ",
    "motivo":"Salida anticipada del grupo"
  }'
```

## Correcciones v5

- Corregido error `QueryDocumentSnapshot` vs `DocumentSnapshot`.
- Corregida variable `zone` no definida.
- Firestore queda configurado directamente como en el proyecto de fichas.
- Eliminadas dependencias de variables de entorno para Firestore.
- `systemParams` se lee directamente desde Firestore.

## Versión v5

- Clases Java reformateadas para mejor lectura y mantenimiento.
- Sin cambios funcionales respecto a la lógica de la versión v4.


## Cambios v6

- Nuevo parámetro Firestore: `requiereAutorizacionSalida`.
- Si `requiereAutorizacionSalida = false`, la segunda lectura del día registra salida sin validar autorización.
- Si `requiereAutorizacionSalida = true`, se conserva:
  - salida normal por horario,
  - autorización por alumno,
  - autorización por grupo.
- El valor `minutosToleranciaSalida` sigue siendo parametrizable.
- Si no existe el `fingerprintId`, se responde `Alumno no registrado`.
- La consulta construye `nombreCompleto` con nombre + apellidos cuando el campo no exista.
- Los datos de autorización en la consulta se muestran únicamente cuando el registro contiene `idAutorizacion`.

## Cambios v7

- Autorización por alumno ahora recibe `iniciales` en lugar de `fingerprintId`.
- El backend busca al alumno activo que corresponde a esas iniciales.
- Si las iniciales corresponden a más de un alumno, se devuelve un error para evitar autorizar al alumno incorrecto.
- Solo puede existir una autorización de ALUMNO por día. Si ya existe, responde: `Ya ha sido autorizada su salida.`
- Al registrar una salida, el estado del registro cambia a `Completado`.
- Un registro `Completado` queda sellado: nuevas lecturas ese mismo día no cambian entrada ni salida y responden: `Debe esperar al día de mañana para poder registrar su entrada nuevamente`.
- Al día siguiente se crea un nuevo documento de registro automáticamente porque el ID incluye la fecha.
- Las salidas autorizadas conservan `idAutorizacion`, por lo que la consulta sigue mostrando quién autorizó y el motivo.

### Request de autorización por alumno

```json
{
  "tipo": "ALUMNO",
  "iniciales": "MDGM",
  "idUsuarioAutoriza": "TMARTINEZ",
  "motivo": "Solicitud del tutor"
}
```


## Cambios v8

- La autorización por alumno ahora busca directamente el campo `iniciales` guardado en la colección `alumnos`.
- Ya no se calculan las iniciales a partir del nombre completo.
- Esto permite usar claves definidas por Control Escolar, por ejemplo `MSGG`.
- El request sigue usando:
  `"iniciales": "MSGG"`
- El registro de salida busca la autorización usando exactamente las iniciales almacenadas en el alumno.
- La importación CSV acepta la columna `iniciales`.
