# CONTEXTO_PROYECTO

## Proyecto
**CETIS 117 - Registro de alumnos por huella digital**

Este documento resume el contexto funcional, técnico y de arquitectura acordado hasta la versión v9 del backend, para poder retomar el proyecto rápidamente en conversaciones futuras.

---

## 1. Objetivo general

El proyecto busca registrar la entrada y salida de aproximadamente 400 alumnos mediante huella digital.

### Hardware principal
- ESP32
- Lector de huellas GM60
- Capacidad aproximada del GM60: 1000 plantillas
- Una huella por alumno
- Módulo DC-DC para regulación de voltaje
- Pantalla LCD para mostrar mensajes al alumno

### Flujo general
1. El alumno coloca su huella.
2. El GM60 identifica la plantilla y devuelve un `fingerprintId`.
3. El ESP32 consume un servicio REST del backend.
4. El backend consulta Firestore.
5. El backend decide si corresponde:
   - registrar entrada,
   - registrar salida normal,
   - registrar salida autorizada,
   - pedir autorización,
   - rechazar porque el alumno no está registrado,
   - impedir un nuevo registro porque el día ya quedó completado.
6. El ESP32 muestra el mensaje correspondiente en el LCD.
7. Los padres/tutores consultan el historial desde un portal React.
8. Director / Control Escolar / Administrador pueden consultar alumnos y registrar autorizaciones.

---

## 2. Arquitectura

```text
GM60
  ↓
ESP32
  ↓
REST Spring Boot
  ↓
Firestore
  ↓
React + Tailwind
```

### Backend
- Java 21
- Spring Boot
- REST
- Firestore
- Cloud Run

### Frontend
- React
- Tailwind
- Responsive
- Principalmente orientado a teléfonos celulares

---

## 3. Google Cloud / Firestore

### Proyecto
```text
zona-d
```

### Base Firestore
```text
zona-d
```

### Configuración usada

Se decidió usar el mismo patrón que el proyecto anterior de Zona D, sin depender de variables de entorno para project ID ni database ID:

```java
return FirestoreOptions.newBuilder()
        .setProjectId("zona-d")
        .setDatabaseId("zona-d")
        .build()
        .getService();
```

### Cloud Run

Servicio:

```text
zdregistroalumnos
```

Región:

```text
us-central1
```

URL usada durante pruebas:

```text
https://zdregistroalumnos-482614074090.us-central1.run.app
```

### Despliegue manual correcto

El despliegue se debe ejecutar desde la carpeta que contiene directamente:

```text
pom.xml
src/
```

Ejemplo:

```powershell
cd C:\ZONAD\zdRegistroAlumnos\services\cetis117-asistencia-vX
```

Luego:

```powershell
gcloud run deploy zdregistroalumnos `
  --source . `
  --region us-central1 `
  --project zona-d `
  --allow-unauthenticated
```

La raíz del repositorio es:

```text
zdRegistroAlumnos
├── .git
├── fe
├── firestore
└── services
```

Por eso un despliegue continuo desde Git debe apuntar al subdirectorio correcto del backend.

---

## 4. Colecciones Firestore

Las colecciones principales son:

```text
alumnos
grupo
registro
autorizacionesSalida
usuariosSistema
systemParams
```

---

## 5. Colección `alumnos`

Contiene la información principal del alumno.

Ejemplo conceptual:

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
  "tutor": {
    "nombre": "Manuel Salvador Guinto",
    "parentesco": "Padre",
    "telefono": "7814520659",
    "telefonoAlternativo": "7814521225",
    "correo": "manuel.guinto@gmail.com"
  },
  "activo": true,
  "fechaRegistro": "...",
  "fechaActualizacion": "..."
}
```

### Consideraciones
- `fingerprintId` relaciona la plantilla del GM60 con el alumno.
- `iniciales` se almacenan explícitamente en Firestore.
- **No se calculan las iniciales a partir del nombre.**
- Control Escolar utiliza `iniciales` para registrar autorizaciones.
- Si `nombreCompleto` no existe, el backend debe construirlo como:

```text
nombre + apellidoPaterno + apellidoMaterno
```

---

## 6. Colección `grupo`

Ejemplo:

```json
{
  "idGrupo": "3",
  "nombre": "1° B",
  "activo": true,
  "fechaCreacion": "...",
  "horaSalida": "14:00"
}
```

### Regla
La hora normal de salida del alumno se obtiene desde el grupo.

---

## 7. Colección `systemParams`

Se usa para reglas configurables sin redesplegar.

Actualmente contiene:

```json
{
  "minutosToleranciaSalida": 20,
  "requiereAutorizacionSalida": true
}
```

### `minutosToleranciaSalida`

Ejemplo:

```text
horaSalida = 14:00
minutosToleranciaSalida = 20

hora permitida sin autorización = 13:40
```

A partir de las 13:40 puede salir normalmente.

### `requiereAutorizacionSalida`

Si es:

```text
true
```

se aplica la lógica de horario y autorizaciones.

Si es:

```text
false
```

el sistema funciona únicamente como registro simple:

```text
primera lectura → entrada
segunda lectura → salida
```

sin validar autorización.

No existe un endpoint aparte para esto; el backend consulta directamente la colección `systemParams`.

---

## 8. Colección `registro`

Se usa un documento por alumno por día.

### ID del documento

```text
fingerprintId_yyyy-MM-dd
```

Ejemplo:

```text
1_2026-10-04
```

Esto permite que al siguiente día se genere un documento nuevo automáticamente.

### Ejemplo conceptual

```json
{
  "fingerprintId": 1,
  "fechaHoraEntrada": "...",
  "fechaHoraSalida": "...",
  "estado": "Completado",
  "idAutorizacion": "AUT-XXXXXXXX"
}
```

### Estado

Durante el día puede existir:

```text
EntradaRegistrada
```

Una vez que ya existe entrada y salida:

```text
Completado
```

### Registro sellado

Una vez que el alumno ya registró entrada y salida:

```text
estado = Completado
```

ese registro queda sellado.

Si vuelve a colocar la huella el mismo día:

```text
Debe esperar al día de mañana para poder registrar su entrada nuevamente
```

No se debe modificar:
- entrada,
- salida,
- estado,
- autorización.

Al día siguiente sí se crea un registro nuevo.

---

## 9. Lógica de asistencia

### Caso 1: alumno no registrado

Si el `fingerprintId` no existe:

```json
{
  "resultado": "ALUMNO_NO_REGISTRADO",
  "mensaje": "Alumno no registrado"
}
```

---

### Caso 2: primera lectura del día

```text
No existe registro diario
→ crear registro
→ fechaHoraEntrada = ahora
→ estado = EntradaRegistrada
```

Respuesta conceptual:

```json
{
  "resultado": "ENTRADA_REGISTRADA",
  "mensaje": "Registro de entrada exitoso, bienvenido",
  "estado": "EntradaRegistrada"
}
```

---

### Caso 3: autorización deshabilitada

Si:

```text
requiereAutorizacionSalida = false
```

entonces la segunda lectura del día:

```text
→ registra salida
→ estado final = Completado
```

---

### Caso 4: autorización habilitada

Si:

```text
requiereAutorizacionSalida = true
```

se obtiene:

```text
horaSalida del grupo
minutosToleranciaSalida
```

Se calcula:

```text
horaPermitidaSalida = horaSalida - minutosToleranciaSalida
```

#### Si horaActual >= horaPermitidaSalida

Salida normal:

```text
→ registrar salida
→ estado final = Completado
→ idAutorizacion = null
```

#### Si horaActual < horaPermitidaSalida

Se requiere autorización.

Orden de búsqueda:

```text
1. autorización por alumno
2. si no existe, autorización por grupo
```

Si no existe:

```text
Requiere autorización de salida
```

Si existe:

```text
→ registrar salida
→ estado final = Completado
→ idAutorizacion = AUT-XXXXXXXX
```

Mensaje LCD:

```text
Registro de salida exitosa, buen regreso a casa
```

---

## 10. Colección `autorizacionesSalida`

Puede contener autorizaciones de tipo:

```text
ALUMNO
GRUPO
```

---

## 11. Autorización por alumno

Control Escolar no conoce el `fingerprintId`.

Por eso el request utiliza:

```text
iniciales
```

Ejemplo:

```json
{
  "tipo": "ALUMNO",
  "iniciales": "MSGG",
  "idUsuarioAutoriza": "TMARTINEZ",
  "motivo": "Solicitud del tutor"
}
```

El backend busca:

```text
alumnos.iniciales == "MSGG"
```

y obtiene internamente el alumno correspondiente.

### Regla de unicidad

Solo puede existir **una autorización por alumno por día**.

Si ya existe:

```json
{
  "resultado": "AUTORIZACION_EXISTENTE",
  "mensaje": "Ya ha sido autorizada su salida."
}
```

No se debe crear un segundo documento.

---

## 12. Autorización por grupo

Request conceptual:

```json
{
  "tipo": "GRUPO",
  "idGrupo": "3",
  "idUsuarioAutoriza": "TMARTINEZ",
  "motivo": "Salida anticipada del grupo"
}
```

### Regla de unicidad

Solo puede existir **una autorización por grupo por día**.

Si ya existe:

```json
{
  "resultado": "AUTORIZACION_GRUPO_EXISTENTE",
  "mensaje": "Grupo ya fue autorizado previamente"
}
```

No se crea un segundo documento.

---

## 13. Estructura conceptual de autorización

### Por alumno

```json
{
  "idAutorizacion": "AUT-XXXXXXXX",
  "tipo": "ALUMNO",
  "iniciales": "MSGG",
  "idGrupo": null,
  "idUsuarioAutoriza": "TMARTINEZ",
  "fechaHoraAutorizacion": "...",
  "motivo": "Solicitud del tutor",
  "activa": true,
  "fecha": "2026-10-04"
}
```

### Por grupo

```json
{
  "idAutorizacion": "AUT-XXXXXXXX",
  "tipo": "GRUPO",
  "iniciales": null,
  "idGrupo": "3",
  "idUsuarioAutoriza": "TMARTINEZ",
  "fechaHoraAutorizacion": "...",
  "motivo": "Salida anticipada del grupo",
  "activa": true,
  "fecha": "2026-10-04"
}
```

---

## 14. Colección `usuariosSistema`

Ejemplo:

```json
{
  "idUsuario": "TMARTINEZ",
  "nombre": "Tania Martinez",
  "rol": "ControlEscolar",
  "codigoHash": "147896325",
  "activo": true,
  "fechaCreacion": "..."
}
```

Roles conceptuales:

```text
DIRECTOR
CONTROL_ESCOLAR
ADMINISTRADOR
```

---

## 15. Consulta para padres y personal

Servicio:

```text
GET /api/alumnos/consulta
```

### Tutor

```text
GET /api/alumnos/consulta?codigo=CODIGO_TUTOR
```

Solo devuelve el alumno asociado.

### Superusuario

```text
GET /api/alumnos/consulta?codigo=SUPERCODIGO
```

Puede devolver todos los alumnos.

Con filtro:

```text
GET /api/alumnos/consulta?codigo=SUPERCODIGO&nombre=Manuel
```

---

## 16. Datos que debe mostrar el portal

Por cada registro:

```text
Fecha
Hora de entrada
Hora de salida
Estado
Nombre de quien autorizó
Motivo
```

### Salida normal

```json
{
  "nombreAutoriza": null,
  "motivo": null
}
```

### Salida autorizada

El backend resuelve:

```text
registro.idAutorizacion
→ autorizacionesSalida.idUsuarioAutoriza
→ usuariosSistema.nombre
```

Y devuelve:

```json
{
  "nombreAutoriza": "Tania Martinez",
  "motivo": "Solicitud del tutor"
}
```

---

## 17. Servicios REST actuales

```text
POST   /api/asistencia/registrar
POST   /api/autorizaciones
POST   /api/alumnos/importar
GET    /api/alumnos/consulta
DELETE /api/registros/limpieza
```

---

## 18. Servicio de asistencia

### Endpoint

```text
POST /api/asistencia/registrar
```

### Request

```json
{
  "fingerprintId": 1
}
```

### Respuestas posibles

#### Entrada

```json
{
  "resultado": "ENTRADA_REGISTRADA",
  "mensaje": "Registro de entrada exitoso, bienvenido"
}
```

#### Requiere autorización

```json
{
  "resultado": "REQUIERE_AUTORIZACION",
  "mensaje": "Requiere autorización de salida"
}
```

#### Alumno no registrado

```json
{
  "resultado": "ALUMNO_NO_REGISTRADO",
  "mensaje": "Alumno no registrado"
}
```

#### Día ya completado

```json
{
  "resultado": "REGISTRO_COMPLETO",
  "mensaje": "Debe esperar al día de mañana para poder registrar su entrada nuevamente"
}
```

---

## 19. Servicio de autorizaciones

### Endpoint

```text
POST /api/autorizaciones
```

### Por alumno

```json
{
  "tipo": "ALUMNO",
  "iniciales": "MSGG",
  "idUsuarioAutoriza": "TMARTINEZ",
  "motivo": "Solicitud del tutor"
}
```

### Por grupo

```json
{
  "tipo": "GRUPO",
  "idGrupo": "3",
  "idUsuarioAutoriza": "TMARTINEZ",
  "motivo": "Salida anticipada del grupo"
}
```

---

## 20. Importación masiva de alumnos

### Endpoint

```text
POST /api/alumnos/importar
```

Formato:

```text
multipart/form-data
```

Parte:

```text
file
```

El CSV debe manejar los campos del alumno, incluyendo:

```text
idAlumno
nombre
apellidoPaterno
apellidoMaterno
nombreCompleto
idGrupo
fingerprintId
iniciales
codigoTutorHash
tutorNombre
tutorParentesco
tutorTelefono
tutorTelefonoAlternativo
tutorCorreo
```

---

## 21. Limpieza de registros

### Endpoint

```text
DELETE /api/registros/limpieza?dias=7
```

Elimina registros anteriores al número de días indicado.

---

## 22. Portal React

El portal debe ser responsive con Tailwind.

### Vista de padres

El padre ingresa:

```text
código de seguridad
```

y visualiza:
- nombre del alumno,
- grupo,
- matrícula / identificador,
- estado,
- entradas registradas,
- salidas registradas,
- último registro,
- bitácora,
- información del tutor.

### Bitácora

Debe mostrar:
- fecha,
- entrada,
- salida,
- estado,
- autorizó,
- motivo.

Cuando no hubo autorización:
- autorizó = vacío,
- motivo = vacío.

---

## 23. Diseño visual

El portal debe usar una estética consistente con el proyecto Zona D / CETIS 117:

- guinda,
- dorado,
- crema,
- tarjetas redondeadas,
- diseño limpio,
- interfaz responsiva,
- buena visualización en celular.

---

## 24. Mensajes del LCD

### Entrada exitosa

```text
Registro de entrada exitoso, bienvenido
```

### Requiere autorización

```text
Requiere autorización de salida
```

### Salida exitosa

```text
Registro de salida exitosa, buen regreso a casa
```

### Alumno no registrado

```text
Alumno no registrado
```

### Día ya terminado

```text
Debe esperar al día de mañana para poder registrar su entrada nuevamente
```

---

## 25. Estado actual del backend

La última versión generada hasta este contexto es:

```text
v9
```

Cambios acumulados hasta v9:
- autorización por iniciales,
- una autorización por alumno por día,
- una autorización por grupo por día,
- parámetro `requiereAutorizacionSalida`,
- parámetro `minutosToleranciaSalida`,
- alumno no registrado,
- registro sellado al completar entrada/salida,
- salida autorizada relacionada mediante `idAutorizacion`,
- consulta con nombre de quien autorizó y motivo,
- `nombreCompleto` con fallback si no existe,
- despliegue funcional en Cloud Run.

---

## 26. Comando local de compilación

```powershell
mvn clean compile
```

Para ejecutar:

```powershell
mvn spring-boot:run
```

Servidor local:

```text
http://localhost:8080
```

---

## 27. Comando de despliegue

```powershell
gcloud run deploy zdregistroalumnos `
  --source . `
  --region us-central1 `
  --project zona-d `
  --allow-unauthenticated
```

---

## 28. Punto actual para continuar

El backend ya está desplegado y se están validando los servicios contra Firestore real.

La última mejora aplicada fue:

```text
solo una autorización por grupo por día
```

Respuesta esperada si ya existe:

```text
Grupo ya fue autorizado previamente
```

La siguiente conversación puede continuar a partir de aquí con:
- ajustes de backend,
- pruebas de endpoints,
- integración ESP32 + GM60,
- LCD,
- portal React,
- seguridad,
- reglas Firestore,
- despliegue frontend,
- o generación de documentación adicional.
