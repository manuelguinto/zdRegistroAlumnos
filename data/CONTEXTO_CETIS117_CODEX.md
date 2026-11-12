# Contexto del proyecto CETis 117 — Control de Asistencia

## 1. Objetivo general

Proyecto de control de asistencia para alumnos del CETis 117.

El sistema debe permitir:
- Identificar alumnos mediante huella digital.
- Registrar entrada y salida.
- Consultar registros por tutor.
- Permitir administración por usuarios del sistema.
- Autorizar salidas anticipadas por alumno o por grupo.
- Permitir salida general.
- Mostrar información y mensajes en un dispositivo físico con ESP32.
- Mantener los datos en Firestore.
- Tener frontend web en React.
- Tener backend REST en Spring Boot.
- Importar alumnos y grupos de forma masiva desde CSV generado por una plantilla Excel.

## 2. Repositorio local

Ruta principal aproximada:

```text
C:\ZONAD\zdRegistroAlumnos
```

Estructura:

```text
zdRegistroAlumnos/
├─ fe/
├─ firestore/
└─ services/
```

Frontend activo local:

```text
C:\ZONAD\zdRegistroAlumnos\fe\cetis117-registro-alumnos-react-v1
```

Backend:
- Spring Boot
- Java 21
- Spring Boot 3.5.6
- artifact: `mx.edu.cetis117:asistencia:0.0.1-SNAPSHOT`

Antes de modificar algo:
1. Revisar la estructura real del repositorio.
2. No asumir nombres de archivo.
3. Mantener compatibilidad con las reglas ya implementadas.
4. Evitar reescribir módulos completos si un cambio puntual es suficiente.

## 3. Google Cloud / Firestore

Proyecto Google Cloud: `zona-d`
Base de datos Firestore: `zona-d`

Configuración explícita usada en backend:

```java
return FirestoreOptions.newBuilder()
        .setProjectId("zona-d")
        .setDatabaseId("zona-d")
        .build()
        .getService();
```

Cloud Run:

```text
Servicio: zdregistroalumnos
Región: us-central1
URL: https://zdregistroalumnos-482614074090.us-central1.run.app
```

Deploy:

```powershell
gcloud run deploy zdregistroalumnos `
  --source . `
  --region us-central1 `
  --project zona-d `
  --allow-unauthenticated
```

`application.properties`:

```properties
server.port=${PORT:8080}
server.address=0.0.0.0
```

## 4. Firebase Hosting

Proyecto:
```text
Project ID: zdcontrolasistencia
Nombre: zdControlAsistencia
Project number: 938892389655
```

Frontend activo:

```text
fe/cetis117-registro-alumnos-react-v1
```

`firebase.json` esperado:

```json
{
  "hosting": {
    "public": "fe/cetis117-registro-alumnos-react-v1/dist",
    "ignore": ["firebase.json","**/.*","**/node_modules/**"],
    "rewrites": [{"source":"**","destination":"/index.html"}]
  }
}
```

## 5. Colecciones Firestore

```text
alumnos
grupo
registro
autorizacionesSalida
usuariosSistema
systemParams
```

## 6. Modelo de alumno

Ejemplo:

```json
{
  "idAlumno": "1",
  "nombre": "Manuel Damian",
  "apellidoPaterno": "Guinto",
  "apellidoMaterno": "Méndez",
  "nombreCompleto": "Manuel Damian Guinto Méndez",
  "idGrupo": "3",
  "fingerprintId": 1,
  "iniciales": "MDGM",
  "codigoTutorHash": "123456",
  "tutor": {
    "nombre": "Manuel Salvador Guinto",
    "parentesco": "Padre",
    "telefono": "7814520659",
    "telefonoAlternativo": "7814521225",
    "correo": "manuel.guinto@gmail.com"
  },
  "activo": true
}
```

Reglas:
- `idAlumno`: numérico del 1 al 1000.
- `fingerprintId = idAlumno`.
- `iniciales`: todos los nombres + apellido paterno + apellido materno.
- Ejemplo: `Manuel Damian Guinto Méndez -> MDGM`.
- `codigoTutorHash`: numérico, aleatorio, único, 6 dígitos.
- `idGrupo` en Firestore debe ser la clave numérica del grupo.

## 7. Modelo de grupo

```json
{
  "idGrupo": "3",
  "nombre": "1° B",
  "activo": true,
  "horaSalida": "14:00"
}
```

Reglas:
- `idGrupo`: numérico del 1 al 1000.
- `horaSalida` por default: `14:00`.
- Si ya tiene horario, no modificar.
- `activo = true`.

## 8. Plantilla Excel / CSV

Hojas:
```text
Alumnos
Grupos
```

### Grupos

Columnas:
```text
A idGrupo
B nombre
C horaSalida
D activo
E fechaCreacion
```

Botón `COMPLETAR DATOS`:
- Procesa SOLO filas donde exista `nombre`.
- Genera `idGrupo` si está vacío.
- Asigna `horaSalida = 14:00` si está vacía.
- Marca `activo = true`.
- Asigna `fechaCreacion = Now` solo si está vacía.
- Si ya existe, no modificar.

### Alumnos

Columnas:
```text
A idAlumno
B nombre
C apellidoPaterno
D apellidoMaterno
E grupo
F fingerprintId
G iniciales
H codigoTutorHash
I tutor.nombre
J tutor.parentesco
K tutor.telefono
L tutor.telefonoAlternativo
M tutor.correo
N activo
O fechaRegistro
P fechaActualizacion
```

Fila completa cuando tiene al menos:
```text
nombre
apellidoPaterno
apellidoMaterno
grupo
tutor.nombre
tutor.parentesco
tutor.telefono
tutor.correo
```

Botón `COMPLETAR DATOS`:
- Procesa SOLO alumnos completos.
- Genera `idAlumno` del 1 al 1000.
- Asigna `fingerprintId = idAlumno`.
- Genera iniciales.
- Genera `codigoTutorHash` de 6 dígitos si está vacío.
- Marca `activo = true`.
- Si `fechaRegistro` está vacía, asigna `Now`.
- Siempre actualiza `fechaActualizacion = Now`.

CSV grupos:

```csv
idGrupo,nombre,horaSalida,activo,fechaCreacion
```

CSV alumnos:

```csv
idAlumno,nombre,apellidoPaterno,apellidoMaterno,idGrupo,fingerprintId,iniciales,codigoTutorHash,tutor.nombre,tutor.parentesco,tutor.telefono,tutor.telefonoAlternativo,tutor.correo,activo,fechaRegistro,fechaActualizacion
```

Reglas:
- Solo exportar alumnos completos.
- En Excel se muestra nombre del grupo.
- En CSV debe salir el `idGrupo` real.
- UTF-8.

## 9. Reglas de asistencia

Documento diario:

```text
fingerprintId_yyyy-MM-dd
```

Estados:
```text
EntradaRegistrada
SalidaRegistrada
SalidaAutorizada
Completado
SalidaAnticipada
SalidaGeneral
```

Salida normal permitida a partir de:

```text
horaSalida - minutosToleranciaSalida
```

Salida anticipada:
- ALUMNO
- GRUPO

Salida general:
- una por día.
- no actualizar masivamente todos los registros.
- si hubo entrada y no salida real, derivar salida virtual.
- conservar salida real.
- si no hubo entrada, no inventar salida.
- bloquear nuevas autorizaciones ALUMNO/GRUPO.

Prioridad de estatus:

```text
1. SALIDA_GENERAL
2. SALIDA_POR_AUTORIZACION
3. SALIDA_POR_HORARIO
4. PENDIENTE
```

## 10. Endpoints principales

```text
POST /api/asistencia/registrar
POST /api/autorizaciones
POST /api/alumnos/importar
GET  /api/alumnos/consulta
DELETE /api/registros/limpieza
GET  /api/acceso/validar?codigo=...
GET  /api/grupos?codigo=...
GET  /api/ping
```

`/api/ping` debe ser ligero y no tocar Firestore.

## 11. Frontend React

Título:
```text
Control de Asistencia
```

No cambiarlo.

Tutor:
- información del alumno
- entradas
- salidas
- último registro
- bitácora
- datos del tutor

Fechas visibles:
```text
DD/MM/YYYY
```

Estados visibles:
```text
SalidaAnticipada -> "Salida anticipada"
SalidaGeneral -> "Salida General"
```

Administración:
- buscar alumnos
- filtrar por grupo
- filtrar por nombre
- filtrar por iniciales
- autorizar salida alumno
- autorizar salida grupo
- salida general
- consulta por grupo
- consulta individual
- cerrar sesión

## 12. Hardware biométrico

Lector:
```text
R307S
```

Librería:
```cpp
#include <Adafruit_Fingerprint.h>
```

UART:
```text
Serial2
57600 baudios
RX2 = GPIO16
TX2 = GPIO17
```

Cableado confirmado:
```text
R307S amarillo -> ESP32 GPIO16 / RX2
R307S verde    -> ESP32 GPIO17 / TX2
R307S rojo     -> VIN / 5V
R307S negro    -> GND
```

Inicialización:

```cpp
Adafruit_Fingerprint finger = Adafruit_Fingerprint(&Serial2);
Serial2.begin(57600, SERIAL_8N1, 16, 17);
```

## 13. Registro de huella funcional

Flujo:
```text
1. Escribir "registrar"
2. Introducir ID
3. Primera captura
4. Retirar dedo
5. Segunda captura
6. image2Tz(1)
7. image2Tz(2)
8. createModel()
9. storeModel(id)
```

Búsqueda:
```cpp
finger.getImage();
finger.image2Tz();
finger.fingerFastSearch();
```

Regla:
```text
fingerprintId = idAlumno
```

## 14. LCD actual

```text
20x4 I2C
Dirección 0x27
SDA = GPIO21
SCL = GPIO22
```

Pantalla inicial aproximada:

```text
CETIS 117       08:15

REGISTRA TU ENTRADA
```

## 15. Funcionalidad que Codex debe revisar/completar

### Prioridad 1 — ESP32 + backend

Después de reconocer una huella, el ESP32 debe enviar:

```text
finger.fingerID
```

a:

```text
POST /api/asistencia/registrar
```

El backend decide:
- entrada
- salida
- salida anticipada
- salida general
- bloqueado hasta mañana
- alumno no encontrado

El ESP32 debe:
1. detectar huella
2. obtener `fingerID`
3. hacer REST
4. interpretar JSON
5. mostrar resultado
6. evitar lecturas repetidas
7. volver a pantalla inicial

### Prioridad 2 — Enrolamiento

Crear mecanismo práctico de registro:
```text
fingerprintId = idAlumno
```

Ideal:
- consultar alumno pendiente de enrolar
- usar `idAlumno` como ID en R307S
- registrar huella
- confirmar en backend

### Prioridad 3 — Importación CSV

Revisar:
```text
POST /api/alumnos/importar
```

Si falta:
- importación CSV de grupos
- validaciones
- evitar duplicados
- resumen de insertados/actualizados/rechazados/errores

### Prioridad 4 — Validaciones

```text
idAlumno: 1..1000
fingerprintId: 1..1000
fingerprintId = idAlumno
codigoTutorHash: 6 dígitos
idGrupo: grupo existente
activo: boolean
```

No almacenar imagen biométrica.

### Prioridad 5 — Frontend administrador

Revisar si existe:
- importar alumnos CSV
- importar grupos CSV
- mostrar resultado de importación
- estado de enrolamiento

## 16. Restricciones

- No cambiar `Control de Asistencia`.
- Firestore guarda `idGrupo`.
- No guardar imágenes biométricas.
- No duplicar lógica de negocio en ESP32.
- Mantener DD/MM/YYYY en UI.
- Mantener compatibilidad con datos existentes.
- Evitar cambios destructivos.
- Antes de modificar, listar archivos afectados.
- Mostrar resumen/diff.
- Compilar/probar cuando sea posible.

## 17. Prompt inicial recomendado para Codex

Usa este archivo como contexto funcional del proyecto.

Primero inspecciona el repositorio y dime:
1. qué funcionalidad ya existe;
2. qué falta;
3. qué archivos modificarías;
4. qué riesgos de compatibilidad hay.

Después implementa por etapas empezando por:

```text
R307S -> ESP32 -> REST backend -> Firestore -> respuesta -> LCD
```

No cambies reglas de negocio existentes sin indicarlo explícitamente.
