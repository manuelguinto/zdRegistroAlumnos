# ESP32 + R307S: huella y asistencia REST

Abrir `lectorRegistraAlumnos.ino` en Arduino IDE. Seleccionar la placa ESP32
correspondiente y su puerto. Configurar SSID y contrasena en `config.h`.
La red debe permitir salida HTTPS y sincronizacion NTP.

Dependencias:
- Paquete de placas ESP32 de Espressif (incluye WiFi, HTTPClient y WiFiClientSecure).
- Adafruit Fingerprint Sensor Library.
- ArduinoJson **7.x**.

Monitor Serial: 115200 baudios, salto de linea. El lector sigue usando
Serial2 a 57600, RX GPIO16 y TX GPIO17. Este programa muestra los mensajes
por Serial; el sketch original no incluye control de LCD.

## Flujo

1. Reconoce una huella local y obtiene `finger.fingerID`.
2. Envia `POST` a la URL de Cloud Run configurada, con JSON
   `{"fingerprintId":1}` y Content-Type application/json.
3. Muestra resultado, mensaje y nombre devueltos por el backend.
4. Si el backend devuelve ALUMNO_NO_REGISTRADO, indica que no existe el
   alumno registrado o esta inactivo. Si la huella no existe en el lector,
   muestra huella no registrada y no llama al backend: no dispone de un ID.
5. Espera que se retire el dedo antes de aceptar otra lectura. Ademas aplica
   una pausa minima de cinco segundos desde la deteccion anterior.

La primera lectura puede registrar entrada; las siguientes pueden registrar
salida o responder con restricciones. El backend decide segun sus reglas.
El ESP32 no concede acceso solo por reconocer la huella.

Ante error de red, HTTP o respuesta invalida, no confirma asistencia ni
reintenta automaticamente: el POST pudo procesarse aunque no llegara la respuesta.
No hay cola de asistencia sin conexion. Si se necesita repetir, comprobar
antes el registro en el portal para evitar que otra lectura registre salida.

## Enrolar

Se conserva el comando `registrar` y las dos capturas. Utilizar como ID
el idAlumno de Firestore. Se conserva el limite original de 1..127 del
enrolamiento; no se ha ampliado sin verificar la capacidad real del lector.
Al terminar exige retirar el dedo para evitar enviar asistencia por accidente.

## HTTPS

`certificado.h` contiene las raices publicas GTS R1 y GTS R4 descargadas de
https://pki.goog/repo/certs/gtsr1.pem y https://pki.goog/repo/certs/gtsr4.pem.
Los PEM originales se incluyen para referencia. Se valida TLS y se sincroniza
la hora por NTP; no se usa setInsecure. Si Google cambia la cadena de confianza,
actualizar las raices. No se envia imagen biometrica: solo el ID del lector.

No se ha compilado ni cargado en placa desde este entorno: no se encontro
arduino-cli. Verificar compilacion en Arduino IDE con estas dependencias y
probar con el lector fisico y una red configurada.

## LCD I2C 20x4

Se agrego LiquidCrystal_I2C (API constructor direccion/columnas/filas,
init y backlight). Instalar esta biblioteca desde Arduino IDE.
Direccion configurada 0x27, SDA GPIO21 y SCL GPIO22.
Si no esta conectada al arrancar, continua por Serial. Conectar con el equipo
apagado y reiniciar despues. Si la direccion real es distinta, cambiar tanto
el constructor como beginTransmission.

Para un modulo LCD de 5V con pull-ups I2C a 5V, usar conversor bidireccional
I2C 3.3V/5V (no conectar estas senales de 5V directo al ESP32):
- LCD GND -> GND comun.
- LCD VCC -> 5V de fuente regulada (o pin 5V/VIN de la placa si entrega 5V).
- LCD SDA -> HV1 del conversor; LV1 -> GPIO21.
- LCD SCL -> HV2 del conversor; LV2 -> GPIO22.
- Conversor HV -> 5V; LV -> 3V3 del ESP32; GND -> GND comun.
Si el modulo ya garantiza niveles I2C de 3.3V, SDA/SCL pueden ir directamente
a GPIO21/GPIO22. Revisar especificacion del modulo antes de conectarlo.
Los 16/17 siguen reservados para el R307S.

Muestra nombre (primeros 20 caracteres) y mensaje del backend en las otras
3 filas. Los mensajes largos se paginan, con 3.5 segundos por pagina previa,
y la ultima permanece 7 segundos antes de volver al inicio. Quita acentos
para la tabla de caracteres del LCD. Tambien muestra fallas WiFi/HTTPS,
alumno inexistente y huella desconocida. Ajustar el potenciometro de contraste
si se ilumina pero no muestra caracteres. No se ha compilado ni probado en
la placa desde este entorno.

## Cabecera fija

La fila 1 muestra siempre `Cetis 117` a la izquierda y HH:mm a la derecha.
Se sincroniza por NTP con desfase UTC-6. Mientras no haya hora valida muestra
--:--. Se actualiza durante el ciclo principal y al cambiar de pantalla;
una llamada HTTP bloqueante puede demorar la actualizacion hasta terminar.
Al detectar el dedo muestra `Verificando alumno` y `...` debajo (la frase
completa tiene 21 caracteres y el LCD tiene 20 columnas).
El nombre queda en fila 2 y el mensaje en filas 3 y 4, paginado cada 40
caracteres. Esta disposicion sustituye la distribucion anterior de la LCD.

## Pantalla y mantenimiento de conexion (configuracion vigente)

Esta seccion sustituye la distribucion y tiempos anteriores:
- Fila 1: Cetis 117 y hora. Fila 2 siempre vacia.
- Filas 3 y 4: mensajes distribuidos por palabras, sin cortar palabras.
  Los mensajes largos se paginan cada 3 segundos. Palabras individuales de
  mas de 20 caracteres se sustituyen por [texto extenso].
- Al detectar el dedo: Verificando alumno... (se ajusta por palabras).
- Resultados y errores se limpian 10 segundos despues de su ultima pagina,
  regresando a Coloca tu dedo. Durante ese tiempo no acepta otra lectura.
- Sin WiFi: No disponible temporalmente... y no registra asistencia.
- Revisa conexion cada 10 minutos y, si falta, intenta reconectar durante
  hasta 15 segundos. Detecta la desconexion para mostrar el aviso en el ciclo
  principal; al recuperarse vuelve automaticamente al inicio.
- Cada 5 minutos intenta GET /api/ping por HTTPS, si hay WiFi y hora valida.
  No modifica datos ni reenvia lecturas de asistencia. Solo imprime el estado
  HTTP del ping por Serial. Las llamadas no garantizan una instancia activa.
- Los intervalos usan millis, sin esperar 5 o 10 minutos con delay. El POST,
  ping y enrolamiento siguen siendo operaciones sincronas: pueden retrasar
  la revision programada hasta que terminen. El ping tiene timeout de conexion
  y lectura de 5 segundos; puede pausar brevemente la lectura de huellas.

Pendiente compilar y probar en el ESP32: no hay arduino-cli en este entorno.

## Resultados en una sola pantalla (vigente)

Se elimino la paginacion. Cada resultado del backend se resume en un mensaje
breve que cabe completo en las filas 3 y 4. No se muestra el nombre completo
en LCD para reservar espacio al resultado; nombre y mensaje original siguen
en Monitor Serial. Los resultados no reconocidos remiten al portal.
Los textos se ajustan por palabras; si otro mensaje excede dos filas se usa
un resumen y la indicacion Consulta el portal. No hay cambio a una segunda
pagina ni espera entre paginas. Se mantienen cabecera, segunda fila vacia,
retorno a inicio a los 10 segundos y temporizadores WiFi/ping.
