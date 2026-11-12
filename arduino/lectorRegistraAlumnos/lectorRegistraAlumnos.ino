/***************************************************
  Código Unificado (Lectura y Registro) para R307S y ESP32
  - Por defecto busca huellas continuamente.
  - Escribe "registrar" en el Monitor Serial para guardar una nueva.
 ****************************************************/

#include <Adafruit_Fingerprint.h>
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <time.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include <vector>
#include "config.h"
#include "certificado.h"

bool esperarRetiro = false;
unsigned long ultimaLectura = 0;
bool huboLectura = false;
const unsigned long PAUSA_ENTRE_LECTURAS_MS = 5000;
LiquidCrystal_I2C lcd(0x27, 20, 4);
bool lcdDisponible = false;
bool lcdTemporal = false;
unsigned long lcdDesde = 0;
String ultimaCabecera;
const unsigned long REVISION_WIFI_MS = 10UL * 60UL * 1000UL;
const unsigned long INTERVALO_PING_MS = 5UL * 60UL * 1000UL;
unsigned long ultimaRevisionWiFi = 0;
unsigned long ultimoPing = 0;
bool mostrandoSinWiFi = false;

std::vector<String> lineasLcd(String texto) {
  std::vector<String> lineas;
  String linea;
  texto.trim();
  while (texto.length()) {
    int espacio = texto.indexOf(' ');
    String palabra = espacio < 0 ? texto : texto.substring(0, espacio);
    texto = espacio < 0 ? "" : texto.substring(espacio + 1);
    texto.trim();
    if (palabra.length() > 20) palabra = "[texto extenso]";
    if (linea.length() && linea.length() + 1 + palabra.length() > 20) {
      lineas.push_back(linea); linea = "";
    }
    if (linea.length()) linea += ' ';
    linea += palabra;
  }
  if (linea.length()) lineas.push_back(linea);
  return lineas;
}

void actualizarCabecera() {
  if (!lcdDisponible) return;
  char hora[6] = "--:--";
  time_t ahora = time(nullptr);
  if (ahora >= 1700000000) {
    struct tm local;
    localtime_r(&ahora, &local);
    strftime(hora, sizeof(hora), "%H:%M", &local);
  }
  String cabecera = String("Cetis 117      ") + hora; // 9 + 6 + 5 = 20 columnas.
  if (cabecera != ultimaCabecera) {
    lcd.setCursor(0, 0);
    lcd.print(cabecera);
    ultimaCabecera = cabecera;
  }
}

String textoLcd(String texto) {
  texto.replace("á", "a"); texto.replace("é", "e");
  texto.replace("í", "i"); texto.replace("ó", "o");
  texto.replace("ú", "u"); texto.replace("ñ", "n");
  texto.replace("Á", "A"); texto.replace("É", "E");
  texto.replace("Í", "I"); texto.replace("Ó", "O");
  texto.replace("Ú", "U"); texto.replace("Ñ", "N");
  texto.replace("ü", "u"); texto.replace("\n", " ");
  texto.replace("\r", " ");
  return texto;
}

void pantalla(String titulo, String mensaje) {
  if (!lcdDisponible) return;
  actualizarCabecera();
  // La primera fila siempre queda reservada para escuela y hora.
  for (int fila = 1; fila < 4; fila++) {
    lcd.setCursor(0, fila); lcd.print("                    ");
  }
  // Segunda linea vacia; contenido solo en las filas tercera y cuarta.
  String texto = textoLcd(titulo);
  if (texto.length() && mensaje.length()) texto += ' ';
  texto += textoLcd(mensaje);
  auto lineas = lineasLcd(texto);
  if (lineas.size() > 2) {
    // Nunca cortar ni paginar: conservar un titulo corto y orientar al portal.
    String resumen = textoLcd(titulo);
    if (!resumen.length() || resumen.length() > 20) resumen = "Consulta el portal";
    lineas = {resumen, "Solicita soporte"};
  }
  for (size_t i = 0; i < lineas.size(); i++) {
    lcd.setCursor(0, i + 2);
    lcd.print(lineas[i]);
  }
  lcdTemporal = true;
  lcdDesde = millis();
}

void pantallaInicial() {
  if (WiFi.status() != WL_CONNECTED) {
    pantalla("No disponible", "temporalmente...");
    lcdTemporal = false;
    mostrandoSinWiFi = true;
    return;
  }
  mostrandoSinWiFi = false;
  pantalla("Coloca tu dedo", "Registra asistencia");
  lcdTemporal = false;
}

void mostrarRespuestaLcd(String resultado) {
  // Resumen del resultado: el texto completo y el nombre siguen por Serial.
  if (resultado == "ENTRADA_REGISTRADA") pantalla("Entrada registrada", "Bienvenido");
  else if (resultado == "SALIDA_REGISTRADA") pantalla("Salida registrada", "Buen regreso");
  else if (resultado == "SALIDA_AUTORIZADA") pantalla("Salida autorizada", "Buen regreso");
  else if (resultado == "SALIDA_GENERAL") pantalla("Salida general", "autorizada");
  else if (resultado == "REGISTRO_COMPLETADO") pantalla("Alumno ya registró", "Salida anteriormente");
  else if (resultado == "REQUIERE_AUTORIZACION") pantalla("Salida no permitida", "Pide autorizacion");
  else if (resultado == "GRUPO_NO_ENCONTRADO") pantalla("Grupo no encontrado", "Consulta al personal");
  else if (resultado == "HORARIO_NO_CONFIGURADO") pantalla("Horario sin definir", "Consulta al personal");
  else pantalla("Respuesta recibida", "Consulta el portal");
}

bool conectarWiFi() {
  if (WiFi.status() == WL_CONNECTED) return true;
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.println("Conectando a WiFi...");
  pantalla("No disponible", "temporalmente...");
  unsigned long inicio = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - inicio < 15000) delay(100);
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("Sin WiFi. No se envio ni confirmo asistencia.");
    pantallaInicial();
    return false;
  }
  return true;
}

void consumirPing() {
  if (WiFi.status() != WL_CONNECTED || time(nullptr) < 1700000000) return;
  WiFiClientSecure cliente;
  cliente.setCACert(GOOGLE_ROOT_CA);
  cliente.setHandshakeTimeout(5);
  HTTPClient http;
  http.setConnectTimeout(5000);
  http.setTimeout(5000);
  String url = ASISTENCIA_URL;
  int api = url.indexOf("/api/");
  if (api < 0) return;
  url = url.substring(0, api) + "/api/ping";
  if (!http.begin(cliente, url)) return;
  int codigo = http.GET();
  http.end();
  Serial.print("Ping HTTP: "); Serial.println(codigo);
}

void mantenerConexion() {
  unsigned long ahora = millis();
  if (ahora - ultimaRevisionWiFi >= REVISION_WIFI_MS) {
    ultimaRevisionWiFi = ahora;
    if (WiFi.status() != WL_CONNECTED) {
      conectarWiFi();
      pantallaInicial();
    }
  }
  if (WiFi.status() != WL_CONNECTED && !mostrandoSinWiFi) pantallaInicial();
  if (WiFi.status() == WL_CONNECTED && mostrandoSinWiFi) pantallaInicial();
  if (millis() - ultimoPing >= INTERVALO_PING_MS) {
    ultimoPing = millis();
    consumirPing();
  }
}

void registrarAsistencia(uint16_t fingerprintId) {
  if (!conectarWiFi()) return;
  // TLS necesita una fecha correcta para validar el certificado.
  unsigned long inicio = millis();
  while (time(nullptr) < 1700000000 && millis() - inicio < 15000) delay(100);
  if (time(nullptr) < 1700000000) {
    Serial.println("No se pudo sincronizar la hora. Asistencia no enviada.");
    pantalla("Error de hora", "Asistencia no enviada");
    return;
  }
  WiFiClientSecure cliente;
  cliente.setCACert(GOOGLE_ROOT_CA);
  cliente.setHandshakeTimeout(20);
  HTTPClient http;
  http.setConnectTimeout(15000);
  http.setTimeout(60000); // Cloud Run puede necesitar tiempo para arrancar.
  if (!http.begin(cliente, ASISTENCIA_URL)) {
    Serial.println("No se pudo preparar la conexion HTTPS.");
    pantalla("Error HTTPS", "No se pudo conectar");
    return;
  }
  http.addHeader("Content-Type", "application/json");
  String cuerpo = "{\"fingerprintId\":" + String(fingerprintId) + "}";
  Serial.println("Consultando alumno y registrando asistencia...");
  pantalla("", "Verificando alumno...");
  int codigo = http.POST(cuerpo);
  String respuesta = codigo > 0 ? http.getString() : "";
  http.end();
  if (codigo < 200 || codigo >= 300) {
    Serial.print("No se pudo confirmar la asistencia. Codigo HTTP/transporte: ");
    Serial.println(codigo);
    Serial.println("No se reintenta automaticamente: el servidor pudo haber procesado la lectura.");
    pantalla("Error de conexion", "Sin confirmacion. Consulta el portal.");
    return;
  }
  JsonDocument datos;
  DeserializationError error = deserializeJson(datos, respuesta);
  if (error || !datos["resultado"].is<const char*>() || !datos["mensaje"].is<const char*>()) {
    Serial.println("Respuesta del servidor invalida. No se pudo confirmar el resultado.");
    pantalla("Respuesta invalida", "Sin confirmacion. Consulta el portal.");
    return;
  }
  const char* resultado = datos["resultado"];
  if (strcmp(resultado, "ALUMNO_NO_REGISTRADO") == 0) {
    Serial.println("No existe el alumno registrado (o esta inactivo).");
    pantalla("Alumno no registrado", "Consulta al personal");
    return;
  }
  Serial.print("Resultado: "); Serial.println(resultado);
  Serial.println(datos["mensaje"].as<const char*>());
  mostrarRespuestaLcd(String(resultado));
  if (datos["nombreAlumno"].is<const char*>()) {
    Serial.print("Alumno: "); Serial.println(datos["nombreAlumno"].as<const char*>());
  }
}

// Conexión física obligatoria: Amarillo a RX2 (16), Verde a TX2 (17)
Adafruit_Fingerprint finger = Adafruit_Fingerprint(&Serial2);

void setup() {
  Serial.begin(115200);
  while (!Serial);
  delay(500);
  Wire.begin(21, 22);
  Wire.beginTransmission(0x27);
  lcdDisponible = Wire.endTransmission() == 0;
  if (lcdDisponible) {
    lcd.init();
    lcd.backlight();
    pantalla("", "Iniciando sistema...");
  } else {
    Serial.println("LCD no detectada en 0x27. Continuando por Serial.");
  }
  
  Serial.println("\n=== SISTEMA BIOMÉTRICO UNIFICADO (R307S + ESP32) ===");

  // Inicialización de Serial2 (RX2=16, TX2=17) a 57600 baudios
  Serial2.begin(57600, SERIAL_8N1, 16, 17);

  if (finger.verifyPassword()) {
    Serial.println("¡Sensor de huella detectado correctamente!");
  } else {
    Serial.println("Error: No se encuentra el sensor. Verifica la energía (5V/VIN) y cables.");
    pantalla("Error del lector", "Revisa cables y alimentacion.");
    while (1) { delay(1); }
  }

  mostrarContadorHuellas();
  WiFi.mode(WIFI_STA);
  configTime(-6 * 3600, 0, "time.google.com", "pool.ntp.org"); // Hora CETIS: UTC-6.
  conectarWiFi();
  ultimaRevisionWiFi = millis();
  ultimoPing = millis();
  imprimirMenu();
  pantallaInicial();
}

void loop() {
  actualizarCabecera();
  mantenerConexion();
  if (lcdTemporal && millis() - lcdDesde >= 5000) pantallaInicial();
  // 1. Verificar si el usuario quiere registrar una huella desde la computadora
  if (Serial.available() > 0) {
    String comando = Serial.readStringUntil('\n');
    comando.trim(); // Quita espacios o saltos de línea
    
    if (comando.equalsIgnoreCase("registrar")) {
      pantalla("Enrolar huella", "Sigue los pasos en el Monitor Serial.");
      modoRegistro();
      pantallaInicial();
      esperarRetiro = true; // No registrar asistencia al terminar el enrolamiento.
      imprimirMenu(); // Al terminar, regresa al menú
    }
  }

  // 2. Modo Lectura Continuo (Busca huellas en el sensor)
  if (WiFi.status() == WL_CONNECTED && !lcdTemporal) verificarHuella();
  delay(100); // Pausa corta para estabilidad eléctrica
}

// ==========================================
//          FUNCIONES DE ASISTENCIA
// ==========================================

void imprimirMenu() {
  Serial.println("\n-------------------------------------------------------");
  Serial.println("[MODO LECTURA ACTIVO] Coloca tu dedo para verificar acceso.");
  Serial.println("👉 Para registrar una nueva huella, escribe: registrar");
  Serial.println("-------------------------------------------------------");
}

void mostrarContadorHuellas() {
  finger.getTemplateCount();
  Serial.print("Huellas actualmente registradas en memoria: ");
  Serial.println(finger.templateCount);
}

// Buscador/Consultor de Huellas
void verificarHuella() {
  int p = finger.getImage();
  if (esperarRetiro) {
    if (p == FINGERPRINT_NOFINGER) esperarRetiro = false;
    return;
  }
  if (p != FINGERPRINT_OK) return; // Si no hay dedo puesto, no hace nada

  if (huboLectura && millis() - ultimaLectura < PAUSA_ENTRE_LECTURAS_MS) return;
  esperarRetiro = true;
  huboLectura = true;
  ultimaLectura = millis();
  pantalla("", "Verificando alumno...");

  p = finger.image2Tz();
  if (p != FINGERPRINT_OK) return;

  p = finger.fingerFastSearch();
  if (p == FINGERPRINT_OK) {
    registrarAsistencia(finger.fingerID);
    Serial.println("Retira el dedo antes de la siguiente lectura.");
    return; // El resultado de acceso lo decide el backend.
  } else if (p == FINGERPRINT_NOTFOUND) {
    pantalla("Huella desconocida", "No registrada en el lector.");
    Serial.println("\n❌ ACCESO DENEGADO: Huella no registrada.");
    delay(2000);
  }
}

// Proceso guiado de Registro
void modoRegistro() {
  Serial.println("\n--- [MODO REGISTRO INICIADO] ---");
  Serial.println("Escribe el número de ID (del 1 al 1000) para esta huella y presiona Enter:");
  
  while (!Serial.available());
  int id = Serial.parseInt();
  
  if (id <= 0 || id > 1000) {
    Serial.println("❌ ID inválida. Cancelando registro.");
    return;
  }
  
  Serial.print("Registrando en la posición ID #"); Serial.println(id);
  
  // PRIMERA TOMA
  int p = -1;
  Serial.println("Coloca el dedo firmemente en el sensor...");
  while (p != FINGERPRINT_OK) {
    p = finger.getImage();
    if (p == FINGERPRINT_NOFINGER) {
      Serial.print(".");
      delay(200);
    }
  }
  
  p = finger.image2Tz(1);
  if (p != FINGERPRINT_OK) {
    Serial.println("\n❌ Error al procesar los rasgos de la huella. Cancelando.");
    return;
  }
  
  // SEGUNDA TOMA
  Serial.println("\n¡Primera imagen exitosa! Retira el dedo...");
  delay(2000);
  p = 0;
  while (p != FINGERPRINT_NOFINGER) { p = finger.getImage(); }
  
  Serial.println("Coloca el MISMO dedo otra vez para confirmar...");
  p = -1;
  while (p != FINGERPRINT_OK) {
    p = finger.getImage();
    if (p == FINGERPRINT_NOFINGER) {
      Serial.print(".");
      delay(200);
    }
  }
  
  p = finger.image2Tz(2);
  if (p != FINGERPRINT_OK) {
    Serial.println("\n❌ Error en la conversión de confirmación. Cancelando.");
    return;
  }
  
  // CREAR Y GUARDAR MODELO
  p = finger.createModel();
  if (p == FINGERPRINT_ENROLLMISMATCH) {
    Serial.println("\n❌ Las huellas no coinciden. Inténtalo de nuevo.");
    return;
  } else if (p != FINGERPRINT_OK) {
    Serial.println("\n❌ Error al estructurar el modelo binario.");
    return;
  }
  
  p = finger.storeModel(id);
  if (p == FINGERPRINT_OK) {
    Serial.println("\n✅ ¡Guardado exitosamente en la memoria interna!");
    mostrarContadorHuellas();
  } else {
    Serial.println("\n❌ Error de almacenamiento físico en la memoria Flash.");
  }
}

