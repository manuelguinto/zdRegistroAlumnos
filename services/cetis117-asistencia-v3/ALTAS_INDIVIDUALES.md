# Altas individuales y panel administrativo

Ambas operaciones requieren `X-Codigo-Sistema`, con codigoHash de un usuario
activo de usuariosSistema. Los codigos de tutor reciben HTTP 403.

## Grupo: POST /api/grupos

Content-Type: application/json

```json
{"nombre":"Primero C","horaSalida":"14:00"}
```

Nombre obligatorio. Si no se envia ID, asigna el mayor ID numerico registrado + 1 (empieza en 1, limite 1000). Hora opcional, por defecto 14:00.
Si existe el ID, actualiza el grupo conservando su horario y fecha de creacion.

## Alumno: POST /api/alumnos

Content-Type: application/json

```json
{
  "nombre":"Ana Maria",
  "apellidoPaterno":"Lopez",
  "apellidoMaterno":"Perez",
  "idGrupo":"4",
  "tutor": {
    "nombre":"Nombre del tutor",
    "parentesco":"Madre",
    "telefono":"5551234567",
    "telefonoAlternativo":"",
    "correo":"tutor@example.com"
  }
}
```

Todos los campos del ejemplo son obligatorios salvo telefonoAlternativo. idAlumno se asigna automaticamente con mayor ID + 1, hasta 1000.
El grupo debe existir. El backend asigna fingerprintId igual al idAlumno,
genera iniciales con nombres y apellidos y genera un codigoTutorHash aleatorio
de seis digitos. Devuelve el codigo para entregarlo al tutor. Al actualizar
un alumno, conserva su codigo existente si no se envia uno explicitamente.
El enrolamiento de la huella fisica sigue realizandose en el lector.

Respuesta: resumen con registrosImportados, insertados, actualizados,
rechazados y errores; incluye idGrupo o idAlumno y, para alumnos, codigoTutorHash.
Las validaciones reutilizan las reglas del importador CSV. Sus errores se
devuelven con HTTP 400 y lista de campos; fila 2 representa el registro individual.
La generacion y comprobacion de unicidad de codigos mantiene la limitacion
del importador: evitar altas/importaciones simultaneas, no hay bloqueo global.

## React

El modulo Altas de grupos y alumnos se integra solo en AdminDashboard,
renderizado por App para tipoAcceso SISTEMA. No se integra en TutorDashboard.
La autorizacion tambien se verifica en backend; ocultar botones no es la
unica restriccion.

La carga masiva usa POST /api/grupos/importar o /api/alumnos/importar con
FormData, campo file y el mismo encabezado. No establece Content-Type manual
para conservar el boundary generado por el navegador. Muestra errores por
fila/campo y resumen de resultados. Al guardar refresca grupos y alumnos.

Las altas usan la misma URL base VITE_API_BASE_URL del resto de la aplicacion.
Para trabajar contra backend local iniciar Vite con esa variable apuntando
a http://localhost:8080. Por defecto sigue usando Cloud Run; desplegar los
nuevos endpoints antes de publicar el frontend en Firebase Hosting.


Las altas automaticas usan creacion exclusiva para evitar sobrescribir registros si dos usuarios calculan el mismo ID. En conflicto devuelve HTTP 409 y solicita volver a guardar. El CSV conserva IDs explicitos y actualizacion por ID.

