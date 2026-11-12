# Importacion CSV y limpieza

Base local: `http://localhost:8080`.
Base Cloud Run: `https://zdregistroalumnos-482614074090.us-central1.run.app`.

Las importaciones y la limpieza requieren el encabezado `X-Codigo-Sistema`, con
el `codigoHash` de un usuario activo de `usuariosSistema`. Un codigo de tutor no sirve.
Esto agrega autenticacion al endpoint existente de importacion de alumnos;
los clientes anteriores deben enviar este encabezado.

## Importar

Enviar multipart/form-data con un archivo en el campo `file`. Importar primero
grupos y despues alumnos. CSV UTF-8 con separador coma; admite BOM, comillas y
comas dentro de campos entrecomillados.

```powershell
$base = 'http://localhost:8080'
$codigo = Read-Host 'Codigo de sistema'
curl.exe -X POST "$base/api/grupos/importar" -H "X-Codigo-Sistema: $codigo" -F 'file=@data/grupos_CETis117.csv'
curl.exe -X POST "$base/api/alumnos/importar" -H "X-Codigo-Sistema: $codigo" -F 'file=@data/alumnos_CETis117.csv'
```

Ejecutar estos ejemplos desde la raiz del repositorio.

Grupos:

```csv
idGrupo,nombre,horaSalida,activo,fechaCreacion
1,Primero A,14:00,true,
```

Alumnos:

```csv
idAlumno,nombre,apellidoPaterno,apellidoMaterno,idGrupo,fingerprintId,iniciales,codigoTutorHash,tutor.nombre,tutor.parentesco,tutor.telefono,tutor.telefonoAlternativo,tutor.correo,activo,fechaRegistro,fechaActualizacion
1,Ana,Lopez,Perez,1,1,ALP,012345,Tutor,Madre,5551234567,,tutor@example.com,true,,
```

- IDs entre 1 y 1000; fingerprintId igual a idAlumno; grupo existente.
- codigoTutorHash debe ser de seis digitos y no pertenecer a otro alumno.
- Requiere nombre, ambos apellidos, iniciales y datos del tutor salvo telefono alternativo.
- Acepta tambien tutorNombre, tutorParentesco, tutorTelefono,
  tutorTelefonoAlternativo y tutorCorreo para compatibilidad.
- activo admite true/false, vacio significa true.
- horaSalida admite HH:mm o fraccion de dia de Excel (por ejemplo,
  `"0,583333333333333"` equivale a 14:00). Vacio significa 14:00.
- Actualiza por ID de documento mediante merge: conserva campos adicionales,
  fecha de registro/creacion y horario de grupos existentes.
- Las fechas del CSV se ignoran: Firestore usa timestamps del servidor de la aplicacion.
- No se generan IDs ni codigos de tutor: deben venir completos desde la plantilla.
- Respuesta: registrosImportados, insertados, actualizados, rechazados y errores
  con numero de fila y campo (incluye encabezado). Si existe cualquier error de validacion, se cancela toda la importacion y no se escribe ningun documento.
- Con datos validos, inserta los IDs nuevos y actualiza los existentes. Si falla
  la validacion devuelve HTTP 400 con registrosImportados, insertados y actualizados
  en cero, totalErrores y la lista completa de errores. Puede haber varios errores
  por fila. Corrige el archivo completo y vuelve a enviarlo al mismo endpoint.
- Analiza el archivo antes de escribir y confirma lotes de hasta 400 operaciones.
  La validacion es previa y completa, pero una falla de infraestructura entre lotes puede dejar una importacion parcial;
  se puede reenviar el CSV. Evitar importaciones o limpiezas simultaneas: la
  validacion de unicidad del codigo se hace sobre una lectura previa, sin bloqueo global.

## Limpiar alumnos y grupos

Elimina TODOS los documentos de alumnos y grupo, en lotes de 400, y devuelve
el conteo por coleccion. No borra registro, autorizacionesSalida, usuariosSistema,
systemParams ni las huellas almacenadas en el lector fisico. Los historicos
pueden conservar referencias a alumnos/grupos eliminados; reutilizar IDs puede
asociarlos con nuevas personas. Usar para reinicio de catalogos con ese efecto previsto.
No ejecutar junto con importaciones o registros de asistencia.

```powershell
curl.exe -X DELETE "$base/api/datos/limpieza?confirmacion=ELIMINAR_ALUMNOS_Y_GRUPOS" -H "X-Codigo-Sistema: $codigo"
```

No hay transaccion global: una falla entre lotes puede dejar limpieza parcial;
puede repetirse la peticion para terminarla.

## Ping

El endpoint existente no accede a Firestore ni modifica datos y no requiere codigo:

```powershell
Invoke-RestMethod "$base/api/ping"
```

Devuelve `{"status":"OK","service":"CETIS117 Asistencia"}`.
Puede consumirse periodicamente. Las llamadas no garantizan que una instancia
permanezca activa. Para mantener capacidad disponible configurar el minimo de
instancias del servicio en Cloud Run, considerando su costo:
https://docs.cloud.google.com/run/docs/configuring/min-instances

Este cambio no despliega servicios ni cambia configuracion en Google Cloud.

