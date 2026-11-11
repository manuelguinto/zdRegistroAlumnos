/***************************************************
  Código Unificado (Lectura y Registro) para R307S y ESP32
  - Por defecto busca huellas continuamente.
  - Escribe "registrar" en el Monitor Serial para guardar una nueva.
 ****************************************************/

#include <Adafruit_Fingerprint.h>

// Conexión física obligatoria: Amarillo a RX2 (16), Verde a TX2 (17)
Adafruit_Fingerprint finger = Adafruit_Fingerprint(&Serial2);

void setup() {
  Serial.begin(115200);
  while (!Serial);
  delay(500);
  
  Serial.println("\n=== SISTEMA BIOMÉTRICO UNIFICADO (R307S + ESP32) ===");

  // Inicialización de Serial2 (RX2=16, TX2=17) a 57600 baudios
  Serial2.begin(57600, SERIAL_8N1, 16, 17);

  if (finger.verifyPassword()) {
    Serial.println("¡Sensor de huella detectado correctamente!");
  } else {
    Serial.println("Error: No se encuentra el sensor. Verifica la energía (5V/VIN) y cables.");
    while (1) { delay(1); }
  }

  mostrarContadorHuellas();
  imprimirMenu();
}

void loop() {
  // 1. Verificar si el usuario quiere registrar una huella desde la computadora
  if (Serial.available() > 0) {
    String comando = Serial.readStringUntil('\n');
    comando.trim(); // Quita espacios o saltos de línea
    
    if (comando.equalsIgnoreCase("registrar")) {
      modoRegistro();
      imprimirMenu(); // Al terminar, regresa al menú
    }
  }

  // 2. Modo Lectura Continuo (Busca huellas en el sensor)
  verificarHuella();
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
  if (p != FINGERPRINT_OK) return; // Si no hay dedo puesto, no hace nada

  p = finger.image2Tz();
  if (p != FINGERPRINT_OK) return;

  p = finger.fingerFastSearch();
  if (p == FINGERPRINT_OK) {
    Serial.println("\n🔓 ACCESO CONCEDIDO");
    Serial.print("-> ID encontrada: #"); Serial.println(finger.fingerID);
    Serial.print("-> Confianza: "); Serial.println(finger.confidence);
    delay(2000); // Pausa para retirar el dedo
  } else if (p == FINGERPRINT_NOTFOUND) {
    Serial.println("\n❌ ACCESO DENEGADO: Huella no registrada.");
    delay(2000);
  }
}

// Proceso guiado de Registro
void modoRegistro() {
  Serial.println("\n--- [MODO REGISTRO INICIADO] ---");
  Serial.println("Escribe el número de ID (del 1 al 127) para esta huella y presiona Enter:");
  
  while (!Serial.available());
  int id = Serial.parseInt();
  
  if (id <= 0 || id > 127) {
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
