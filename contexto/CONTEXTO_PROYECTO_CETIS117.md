# Contexto del proyecto CETIS 117 — Control de Asistencia

Actualizado: 12/11/2026 (America/Mexico_City).

Este documento sustituye el contexto del 05/10/2026 y resume las decisiones,
el codigo y las verificaciones realizadas en esta conversacion. Los despliegues
se reportan como ultimo estado confirmado; comprobarlos si se retoma mas adelante.
No incluye contrasenas, codigos de acceso reales ni credenciales.

## 1. Objetivo y estructura

Sistema de entradas y salidas de alumnos mediante huella digital, consulta de
tutores y administracion escolar. Titulo del portal: **Control de Asistencia**.

Raiz: `C:\ZONAD\zdRegistroAlumnos`.

| Carpeta | Contenido |
| --- | --- |
| fe/cetis117-registro-alumnos-react-v1 | React 18 + Vite 5 + Tailwind |
| services/cetis117-asistencia-v3 | Java 21 + Spring Boot 3.5.6 |
| firestore/colecctions | Definiciones de colecciones (conservar nombre existente) |
| arduino/lectorRegistraAlumnos | Sketch ESP32 + R307S, configuracion y certificados |
| data | Plantilla XLSM, CSV de ejemplo y guia HTML REST |
| contexto | Este archivo de continuidad |

Antes de modificar: revisar los archivos reales, anunciar los afectados,
respetar cambios del usuario y preferir cambios puntuales. No reescribir reglas
de negocio sin indicarlo. Probar/compilar cuando sea posible y mostrar resumen.

## 2. Infraestructura y ultimo despliegue confirmado

### Backend / Firestore

- Google Cloud project: `zona-d`.
- Firestore database: `zona-d` (no la base default).
- Configuracion explicita en `FirestoreConfig.java`, sin variables para esos IDs.
- Servicio Cloud Run: `zdregistroalumnos`, region `us-central1`.
- URL: https://zdregistroalumnos-482614074090.us-central1.run.app
- Ultima revision desplegada: `zdregistroalumnos-00021-skg`, 100 % del trafico.
- Incluye altas individuales con IDs automaticos e importacion CSV.
- GET /api/ping publico verificado con status OK tras desplegar.
- Se conservaron permisos de acceso existentes; no agregar cambios de IAM.
- Configuracion HTTP: server.port=${PORT:8080}, server.address=0.0.0.0.

### Frontend / Firebase Hosting

- Proyecto: `zdcontrolasistencia`, nombre `zdControlAsistencia`.
- Project number: `938892389655`.
- URL publicada: https://zdcontrolasistencia.web.app
- Publicado correctamente el 12/11/2026, con altas individuales y masivas.
- Version Firebase Hosting: `dceedf5c9bff19a7`.
- firebase.json publica `fe/cetis117-registro-alumnos-react-v1/dist`.
- Rewrite de todas las rutas a /index.html.
- No ejecutar firebase init nuevamente.

### URL de API en React

`src/api.js` usa VITE_API_BASE_URL cuando esta definida; por defecto usa la URL
HTTPS de Cloud Run tanto en desarrollo como en produccion. Se retiro el proxy
local de Vite. No existe .env local segun la ultima inspeccion, solo .env.example.
Por tanto las altas desde localhost tambien modifican Firestore real.
Para pruebas locales del backend se puede definir VITE_API_BASE_URL=http://localhost:8080
antes de iniciar Vite; el comportamiento predeterminado debe seguir siendo Cloud Run.

## 3. Colecciones y modelo vigente

Colecciones: alumnos, grupo, registro, autorizacionesSalida, usuariosSistema,
systemParams. La coleccion de grupos se llama **grupo**, singular.

### Alumno

- idAlumno: string numerico 1..1000; normalmente tambien ID del documento.
- fingerprintId: numerico, igual a idAlumno.
- nombre, apellidoPaterno, apellidoMaterno, nombreCompleto.
- idGrupo: ID numerico como string, no nombre del grupo.
- iniciales: clave normalizada guardada, usada en busquedas/autorizaciones.
- codigoTutorHash: string de seis digitos; no convertir a numero.
- tutor: mapa con nombre, parentesco, telefono, telefonoAlternativo y correo.
- activo: boolean; fechaRegistro y fechaActualizacion: timestamps.

Las consultas y autorizaciones usan las iniciales almacenadas; no recalcularlas
al consultar. Las altas individuales nuevas SI generan iniciales a partir de
todos los nombres y apellidos. CSV recibe iniciales de la plantilla y normaliza.
No almacenar imagenes biometricas. Las plantillas de huella quedan en el lector.

### Grupo

idGrupo (string 1..1000), nombre, horaSalida (HH:mm), activo, fechaCreacion.
Horario predeterminado 14:00; conservar horario existente al actualizar/importar.

### Parametros

systemParams contiene minutosToleranciaSalida y requiereAutorizacionSalida.
Cuando no se obtiene tolerancia se usa respaldo de 20 minutos. Revisar el servicio
para el comportamiento exacto de cada parametro antes de cambiarlo.

## 4. Reglas de asistencia que se conservan

- Documento diario: fingerprintId_yyyy-MM-dd, zona America/Mexico_City.
- Primera lectura crea entrada. Siguiente lectura puede registrar salida o
  devolver restriccion segun reglas del backend; Arduino no decide entrada/salida.
- Si ya existe salida o registro completado, no cambiarlo por nuevas lecturas.
- Salida normal: horario del grupo menos tolerancia cuando aplica autorizacion.
- requiereAutorizacionSalida=false permite la segunda lectura como salida.
- Autorizaciones ALUMNO por iniciales, GRUPO por idGrupo y GENERAL por hora.
- Rechazar iniciales ambiguas para no autorizar al alumno equivocado.
- Una autorizacion de alumno por dia y una por grupo por dia.
- Solo una GENERAL activa al dia; bloquea nuevas ALUMNO/GRUPO.
- GENERAL no modifica masivamente registros. Consulta deriva salida virtual
  solo si hubo entrada sin salida real. Nunca sustituir una salida real.
- No inventar salida para alumnos sin entrada.
- Etiquetas: Completado para salida normal; SalidaAnticipada / Salida anticipada
  para salida autorizada; SalidaGeneral / Salida General para general.
- Prioridad visual de grupo: SALIDA_GENERAL > SALIDA_POR_AUTORIZACION >
  SALIDA_POR_HORARIO > PENDIENTE.
- El usuario modifico mensajes en AsistenciaService.java; conservarlos.

## 5. Servicios REST disponibles

| Metodo | Ruta | Uso / acceso actual |
| --- | --- | --- |
| GET | /api/ping | Sin Firestore ni escrituras; publico |
| POST | /api/asistencia/registrar | JSON fingerprintId; Arduino, sin codigo en controlador |
| GET | /api/acceso/validar?codigo=... | Identifica SISTEMA o TUTOR |
| GET | /api/grupos?codigo=... | Grupos activos, horario y estado; sistema |
| GET | /api/alumnos/consulta?codigo=... | Tutor: su alumno; sistema: filtros nombre/idGrupo/iniciales |
| POST | /api/grupos | Alta individual / actualizacion explicita por ID; sistema |
| POST | /api/alumnos | Alta individual / actualizacion explicita por ID; sistema |
| POST | /api/grupos/importar | Multipart file; sistema |
| POST | /api/alumnos/importar | Multipart file; sistema |
| POST | /api/autorizaciones | ALUMNO/GRUPO/GENERAL; idUsuarioAutoriza activo en JSON |
| DELETE | /api/datos/limpieza | Borra alumnos y grupo; sistema y confirmacion |
| DELETE | /api/registros/limpieza?dias=7 | Borra registros antiguos; controlador existente sin codigo |

Altas, importaciones y limpieza de catalogos requieren encabezado
`X-Codigo-Sistema` con codigoHash de usuario activo de usuariosSistema.
Codigo de tutor no autoriza: HTTP 403. La restriccion existe tambien en backend.

Autorizacion JSON: tipo, iniciales o idGrupo o horaSalidaGeneral segun tipo,
idUsuarioAutoriza y motivo. Este endpoint existente no exige X-Codigo-Sistema.
No afirmar que todos los endpoints tienen el mismo mecanismo de autenticacion.
No dar por existente /actuator/health: no se verifico y el pom no incluye starter actuator.

## 6. Altas individuales e IDs automaticos

Implementadas en DatosController y ImportacionService, reutilizan validaciones CSV.

### Grupo

POST /api/grupos, JSON minimo:

```json
{"nombre":"Primero C","horaSalida":"14:00"}
```

Nombre obligatorio, horario opcional. Al no enviar idGrupo calcula mayor ID
numerico registrado + 1. Coleccion vacia empieza en 1, limite 1000.

### Alumno

POST /api/alumnos, JSON de ejemplo ficticio:

```json
{
  "nombre":"Ana Maria",
  "apellidoPaterno":"Lopez",
  "apellidoMaterno":"Perez",
  "idGrupo":"4",
  "tutor":{
    "nombre":"Nombre del tutor",
    "parentesco":"Madre",
    "telefono":"5551234567",
    "telefonoAlternativo":"",
    "correo":"tutor@example.com"
  }
}
```

Todos obligatorios salvo telefonoAlternativo. Grupo debe existir.
Calcula mayor idAlumno + 1, asigna fingerprintId igual al ID, genera iniciales
y codigo aleatorio de seis digitos libre en la lectura actual de Firestore.
Devuelve ID y codigoTutorHash para informar al operador/tutor.
Enrolar luego la huella fisica usando ese ID.

- Mayor ID se calcula numericamente considerando ID del documento y campo ID;
  no usar orden lexicografico ni cantidad de documentos.
- Alta automatica usa create exclusivo. Si existe conflicto concurrente, HTTP 409
  y pedir volver a guardar: no sobrescribe el registro de otra persona.
- Se mantiene compatibilidad para clientes que envian ID explicito: actualiza
  mediante merge; conserva fechas de creacion y horario de grupo existente.
- En actualizaciones de alumno conserva codigo de tutor si no viene uno explicito.
- El formulario React ya NO pide ID de alumno ni ID de grupo al crear un grupo.
  Seleccionar el grupo del alumno sigue siendo obligatorio.
- Si se borra el ID mas alto, puede reutilizarse: el algoritmo usa el maximo
  actualmente existente, no una secuencia historica persistente.

## 7. Importacion CSV

- CSV UTF-8 separado por comas, admite BOM, comillas y comas dentro de comillas.
- POST multipart/form-data, nombre exacto del campo: file, tipo File en Postman.
- No fijar Content-Type manualmente en navegador/Postman; conservar boundary.
- Limite configurado 10 MB por archivo y por peticion multipart completa.
- Importar grupos antes de alumnos. CSV sigue requiriendo IDs explicitos;
  sirve para insertar y actualizar por ID. No se cambio a ID automatico en CSV.
- Validar TODAS las filas antes de escribir; acumula campos faltantes y fallas.
- Cualquier error de validacion: HTTP 400, cero escrituras y todos los errores
  por fila/campo. La primera fila de datos es 2 (encabezado es 1).
- Respuesta de fallo: error, registrosImportados=0, insertados=0,
  actualizados=0, totalErrores y errores[].
- Exito: registrosImportados, insertados, actualizados, rechazados=0, errores=[].
- Conserva campos adicionales por merge, fechas iniciales existentes y horarios
  de grupos existentes. Ignora fechas del CSV y usa timestamps de la aplicacion.
- activo: true/false; vacio predetermina true.
- Horario admite HH:mm o fraccion Excel; 0,583333333333333 entre comillas = 14:00.
- Tutor admite columnas tutor.nombre, tutor.parentesco, etc., y nombres anteriores
  tutorNombre, tutorParentesco, tutorTelefono, tutorTelefonoAlternativo, tutorCorreo.
- Nombre, ambos apellidos, iniciales, codigo de tutor y grupo requeridos para alumno;
  todos los datos de tutor salvo telefono alternativo son obligatorios.
- Lotes de hasta 400 escrituras. No hay transaccion global: un fallo de infraestructura
  entre lotes puede dejar cambios parciales aunque la validacion previa sea completa.
- Unicidad de codigo de tutor se valida contra lectura previa, sin bloqueo global;
  evitar altas/importaciones simultaneas hasta reforzar esta garantia si se requiere.

## 8. Limpieza de datos

DELETE /api/datos/limpieza?confirmacion=ELIMINAR_ALUMNOS_Y_GRUPOS
requiere X-Codigo-Sistema. Elimina TODOS los documentos de alumnos y grupo,
por lotes de 400. No elimina registro, autorizacionesSalida, usuariosSistema,
systemParams ni huellas fisicas. Puede dejar referencias historicas huerfanas;
reutilizar IDs puede asociar registros antiguos con otras personas.
No ejecutar junto con altas, importaciones o registro de asistencia.
No se ejecutaron borrados reales en esta conversacion.

/api/registros/limpieza conserva el mecanismo anterior por dias. Usa fecha de
entrada anterior al limite, predetermina 7 dias. Falta validar rango positivo.
El controlador actual no exige codigo; pendiente revisar si se endurece acceso.

## 9. React: experiencia y restricciones

App distingue session.tipoAcceso SISTEMA y TUTOR.
AdminDashboard contiene CatalogRegistration con opciones:
- Alta de grupo: nombre y hora.
- Alta de alumno: nombre/apellidos, grupo y datos del tutor.
- Alta masiva: lista Registrar grupos / Registrar alumnos y selector CSV.
- Confirma ID asignado, resumen insertados/actualizados/rechazados y codigo tutor.
- Muestra lista de errores por fila/campo; bloquea doble envio mientras guarda.
- Al guardar refresca consultas de alumnos y grupos.

CatalogRegistration solo se integra en AdminDashboard, no en TutorDashboard.
Mantener consulta de tutores, filtros, autorizaciones, salida general, boton
Cerrar sesion, titulo Control de Asistencia y estilo guinda/dorado/crema.
Fechas visibles: DD/MM/YYYY.
api.js soporta JSON y FormData y conserva detalles de validacion del backend.

## 10. ESP32 y R307S (reemplaza la referencia antigua al GM60)

Archivo: arduino/lectorRegistraAlumnos/lectorRegistraAlumnos.ino.

- Lector usado en las pruebas: R307S; libreria Adafruit_Fingerprint.
- Serial2, 57600 baudios; amarillo -> GPIO16 RX2, verde -> GPIO17 TX2.
- Rojo -> alimentacion 5V apropiada; negro -> GND comun.
- Monitor Serial a 115200, comando registrar, ID, dos capturas y storeModel.
- Enrolamiento aun conserva limite original 1..127: confirmar capacidad real
  del lector antes de ampliarlo, aunque backend permita IDs 1..1000.
- Dependencias adicionales: paquete ESP32, ArduinoJson 7.x y LiquidCrystal_I2C
  con API init()/backlight(). Error ArduinoJson.h ausente se resuelve instalando
  ArduinoJson de Benoit Blanchon, no Adafruit GFX.
- Configuracion de red en config.h; SSID configurado Zona-D. Contrasena solo en
  configuracion local, no copiar a contexto ni publicar credenciales.
- HTTPS usa GTS R1 y R4 en certificado.h, fuentes PEM de pki.goog; no setInsecure.
- NTP time.google.com/pool.ntp.org, desfase UTC-6 para LCD. Sin hora valida: --:--.

Al reconocer huella: POST a la URL Cloud Run /api/asistencia/registrar con
finger.fingerID. Interpreta resultado/mensaje/nombre. ALUMNO_NO_REGISTRADO indica
alumno inexistente o inactivo. Huella no reconocida localmente no invoca REST.
No retransmitir automaticamente el POST ante fallo: pudo procesarse y otra
lectura podria convertirse en salida. No hay cola offline.

### LCD actual

20 columnas x 4 filas, I2C 0x27. SDA GPIO21, SCL GPIO22.
- Fila 1 siempre: Cetis 117 a izquierda y HH:mm a derecha.
- Fila 2 siempre vacia.
- Filas 3/4: mensajes cortos completos, ajuste por palabras, SIN paginacion.
- Verificando alumno... al detectar dedo.
- Resume resultado (Entrada registrada/Bienvenido, Salida registrada/Buen regreso,
  Alumno no registrado, etc.). Nombre y mensaje originales siguen por Serial.
- Resultados/errores duran 10 segundos y vuelven a Coloca tu dedo.
- Exige retirar dedo y pausa minima entre lecturas; no acepta nuevas durante resultado.
- Sin WiFi: No disponible temporalmente...; no registra asistencia.
- Revisa WiFi cada 10 minutos y reconecta si falta; detecta desconexion para aviso.
- GET /api/ping cada 5 minutos si hay red y hora valida. No toca Firestore.
- HTTP, ping y enrolamiento aun bloquean: pueden retrasar reloj y temporizadores.
- Si LCD ausente en 0x27, continua por Serial. Reiniciar despues de conectarla.
- Modulo LCD I2C de 5V puede requerir conversor bidireccional 3.3V/5V para SDA/SCL.
  No conectar pull-ups a 5V directo a GPIO ESP32.

### Problema de alimentacion observado

El usuario reporto apagado de LCD/lector al alimentar ambos desde 5V de la
placa por USB de laptop. Log repite Brownout detector was triggered despues
que detecta lector y cuenta 2 huellas, cerca de activar WiFi.
Esto confirma caida de voltaje/reinicios, no demora de deteccion del sensor.
No desactivar detector como solucion. Probar cable/puerto y modulos por separado.
Si usa fuente externa regulada de 5V para perifericos: tierras comunes y no unir
5V externo a VIN/5V de ESP32 alimentada por USB sin verificar la placa.
La falta de LCD en 0x27 puede ser alimentacion, conexion o direccion; no esta
confirmada una unica causa. Falta prueba fisica de estabilidad y escaneo I2C.

No hay arduino-cli disponible: cambios del sketch no fueron compilados ni
cargados por el agente; usuario los compila/carga con Arduino IDE.

## 11. Verificacion realizada

- npm run build exitoso despues de los cambios de React.
- mvn test: 9 pruebas, cero fallos/errores al finalizar asignacion automatica.
- Pruebas incluyen IDs numericos (vacio, 9/10, maximo y limite 1000), alta automatica
  de alumno, tutor anidado, rechazo de tutor como operador, CSV sin escrituras
  ante errores, campos faltantes multiples y actualizacion de grupo.
- No se realizaron altas/borrados reales como pruebas del agente.
- Se verificaron ping local y publico; no se hizo QA visual automatizado del portal.
- Ultimos servidores locales iniciados: Vite http://localhost:5173 y Spring Boot
  http://localhost:8080. No asumir que continuan vivos al retomar: comprobar.

## 12. Git y estado de trabajo

- Remoto: https://github.com/manuelguinto/zdRegistroAlumnos.git
- Rama: main.
- Ultimo commit subido: bc70a9a, Agrega altas administrativas con IDs automaticos y carga CSV.
- Push a origin/main exitoso, desde 54233bd hasta bc70a9a.
- Se subieron 8 archivos de codigo, pruebas y documentacion; no compilados/caches.
- Quedaron cambios locales generados en dist, target, node_modules/.vite y un
  temporal ~$ de Excel. Algunos compilados/dependencias ya estaban versionados;
  no confundirlos con cambios nuevos de fuente ni borrarlos sin revisar.
- Esta actualizacion de contexto se crea DESPUES de ese push; aun no tiene commit/push.
- Revisar manejo de credenciales en archivos de configuracion antes de futuros pushes.

## 13. Guias disponibles

- data/Servicios_REST_CETIS117.html: guia autonoma con endpoints y ejemplos PowerShell.
- services/cetis117-asistencia-v3/IMPORTACION_CSV.md.
- services/cetis117-asistencia-v3/ALTAS_INDIVIDUALES.md.
- arduino/lectorRegistraAlumnos/README.md: contiene historia de disposiciones LCD;
  prevalece la ultima seccion Resultados en una sola pantalla, no las paginas anteriores.
- data/CONTEXTO_CETIS117_CODEX.md: referencia funcional anterior; este contexto
  actualiza estado y decisiones posteriores.

## 14. Comandos para continuar

Backend local, desde su carpeta:

```powershell
mvn test
mvn spring-boot:run
```

Backend Cloud Run, desde services/cetis117-asistencia-v3:

```powershell
gcloud run deploy zdregistroalumnos --source . --region us-central1 --project zona-d --quiet
```

Conservar permisos actuales. No agregar --allow-unauthenticated para cada deploy:
la revision automatica rechazo previamente ese cambio potencial de seguridad;
el despliegue sin ese parametro funciono.

Frontend, desde fe/cetis117-registro-alumnos-react-v1:

```powershell
npm run dev
npm run build
```

Hosting, desde la raiz:

```powershell
firebase deploy --only hosting --project zdcontrolasistencia --non-interactive
```

Ping publico:

```powershell
Invoke-RestMethod 'https://zdregistroalumnos-482614074090.us-central1.run.app/api/ping'
```

En el entorno del agente, mvn/npm/gcloud/firebase pueden requerir escalacion
por accesos a dependencias, procesos auxiliares o red. No eludir restricciones.

## 15. Pendientes y limites

1. Validar en ESP32 fisico alimentacion estable, direccion LCD y operacion conjunta.
2. Compilar el sketch con dependencias indicadas y confirmar capacidad de huellas
   antes de usar IDs mayores que 127 en enrolamiento.
3. Probar en navegador las altas, mensajes de error y acceso exclusivo de sistema.
4. Si se requieren escrituras concurrentes fuertes, reforzar unicidad del codigo
   de tutor y atomicidad global de importaciones; no prometer esas garantias hoy.
5. Revisar limpieza de historicos/reutilizacion de IDs y acceso al borrado antiguo
   antes de usar reinicio de catalogos en produccion.
6. Cloud Scheduler fue propuesto antes (ping cada 5 min, 06:00-15:00), pero no se
   creo ni verifico en esta conversacion. No marcarlo como existente.
7. Ping de ESP32 y Scheduler no garantizan que Cloud Run conserve una instancia;
   no se configuro minimo de instancias en esta conversacion.
8. Ordenar en una tarea separada los artefactos generados que ya estan versionados.
9. Actualizar el contexto y hacer commit cuando se solicite; no subir automaticamente
   contrasenas ni datos personales de alumnos/tutores.
