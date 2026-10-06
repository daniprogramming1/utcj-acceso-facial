# Acceso UTCJ — Verificación de acceso universitario

App Android nativa para verificar el acceso de alumnos a la **Universidad Tecnológica de Ciudad Juárez (UTCJ)** mediante reconocimiento facial en el dispositivo, con respaldos por huella y QR dinámico, bitácora auditable y panel de guardia.

> Proyecto escolar. El motor de *embeddings* faciales es una implementación pragmática (ver **Limitaciones**), diseñada para reemplazarse por un modelo FaceNet/MobileFaceNet.

## Características

| Área | Qué hace |
|---|---|
| Roles | «Soy alumno» / «Soy guardia de seguridad» |
| Guardia | Contraseña PBKDF2-HMAC-SHA256 (sal aleatoria, 120 000 iteraciones) en EncryptedSharedPreferences; bloqueo de 5 min tras 5 intentos fallidos; salir del kiosco exige contraseña |
| Registro de alumno | Consentimiento de privacidad versionado con fecha/hora; 3–5 muestras faciales (CameraX + ML Kit); **solo embeddings cifrados AES-256-GCM (Android Keystore)**, nunca fotos; estado PENDIENTE hasta aprobación |
| Reconocimiento | Alineación por ojos, recorte con margen, controles de calidad con guía en español, similitud coseno contra todas las muestras, umbral configurable (**0.72** por defecto), liveness opcional (parpadeo / giro) |
| Respaldo | BiometricPrompt (huella) o QR dinámico firmado con HMAC-SHA256 (vigencia 30 s) leído con ML Kit Barcode |
| Kiosco | Pantalla completa inmersiva, orientación fija, pantalla siempre encendida, resultado verde/rojo con nombre y matrícula, reinicio automático a los 30 s |
| Bitácora | Room (solo lectura en la app), filtros, exportación CSV y PDF (`PdfDocument`), cola WorkManager sin duplicados hacia Firestore (opcional) |
| Panel | Búsqueda de alumnos, aprobar/rechazar, entrada manual con motivo obligatorio, visitantes (entrada/salida), incidentes, tablero con gráficas Canvas, alertas (3+ fallos seguidos, fuera de horario), configuración, modo evaluación oculto |
| Sin conexión | Embeddings y estatus en caché local; indicador: *En línea / Sin conexión / Sincronizando / Pendientes: N* |
| Estatus institucional | CSV cargado por el administrador (muestra en `assets/` + importación con selector de documentos); BAJA/SUSPENDIDO = sin acceso |

## Requisitos

- Android Studio Hedgehog (2023.1.1) o más reciente
- JDK 17
- Android SDK 34
- Dispositivo físico con cámara frontal (recomendado; el emulador sirve para la UI, no para el reconocimiento)
- minSdk 26 (Android 8.0)

## Compilar y ejecutar

```bash
git clone https://github.com/daniprogramming1/utcj-acceso-facial.git
cd utcj-acceso-facial
cp local.properties.example local.properties   # ajusta sdk.dir (Android Studio lo crea solo)
./gradlew assembleDebug                        # APK en app/build/outputs/apk/debug/
./gradlew installDebug                         # instala en el dispositivo conectado
./gradlew testDebugUnitTest                    # pruebas unitarias
```

O ábrelo en Android Studio → *Open* → selecciona la carpeta → *Run ▶*.

### Primer uso
1. Abre la app → **Soy guardia de seguridad** → define la contraseña (mínimo 6 caracteres).
2. El CSV de muestra (`app/src/main/assets/students_status.csv`) se importa automáticamente la primera vez.
3. Con otro usuario: **Soy alumno** → acepta el consentimiento → registra el rostro.
4. Guardia → **Aprobaciones pendientes** → *Aprobar*.
5. Guardia → **Modo kiosco** → el alumno se coloca frente a la cámara.

## Firebase (opcional)

La app funciona 100 % sin conexión. Para sincronizar la bitácora con Firestore:

1. Crea un proyecto en [Firebase Console](https://console.firebase.google.com/) y registra la app Android con el paquete `edu.utcj.acceso` (y `edu.utcj.acceso.debug` para debug).
2. Descarga `google-services.json` a `app/` (está en `.gitignore`; **no lo subas**).
3. Descomenta en `build.gradle.kts` (raíz) `alias(libs.plugins.google.services) apply false`.
4. Descomenta en `app/build.gradle.kts` el plugin `alias(libs.plugins.google.services)` y las dependencias `firebase-bom` / `firebase-firestore`.
5. En `FirestoreDataSource.kt` cambia `FIRESTORE_COMPILED_IN = true` e implementa la escritura indicada en el comentario (`collection("access_events").document(syncKey).set(...)`). Usar `syncKey` como ID de documento hace que la escritura sea idempotente.

Mientras Firebase esté desactivado, el worker marca los eventos como sincronizados localmente para vaciar la cola.

## Estructura

```
app/src/main/java/edu/utcj/acceso/
  AccesoApp.kt, MainActivity.kt
  di/            Hilt: AppModule, DatabaseModule, SecurityModule
  data/local     Room: entidades, DAOs, AppDatabase, Converters
  data/remote    FirestoreDataSource, StudentStatusCsvDataSource
  data/repository Student, AccessLog, Auth, Sync, Settings, Visitor, Incident
  data/biometric FaceEmbeddingEngine, FaceQualityChecker, FaceMatcher, LivenessChecker, EmbeddingCrypto, QrTokenManager
  data/security  PasswordHasher, GuardAuthManager, SecurePrefs, KeyValueStore
  data/sync      SyncWorker, SyncQueue
  domain/model   Student, AccessEvent, Visitor, Incident, ConsentRecord, GuardSession
  ui/            theme, navigation, role, student, guard, kiosk, log, panel, components
  util/          WindowSizeClass, TimeUtil, AppResult
```

Más detalles en [ARCHITECTURE.md](ARCHITECTURE.md). Guía de pruebas en [TESTING.md](TESTING.md).

## Limitaciones conocidas

- **Embedding facial escolar**: histograma de intensidad en escala de grises (16×16) del rostro alineado + 12 rasgos geométricos de landmarks de ML Kit = vector de 268 dimensiones normalizado L2. Sirve para demostraciones; **no** tiene la robustez de una red neuronal entrenada (sensible a iluminación, lentes, cubrebocas). Para producción sustituye `FaceEmbeddingEngine.extractEmbedding()` por inferencia TFLite/ONNX (MobileFaceNet) y recalibra el umbral.
- **Huella**: BiometricPrompt valida una huella enrolada **en el dispositivo**, no identifica a un alumno concreto; se registra como respaldo con el guardia en turno.
- **QR dinámico**: el secreto HMAC es local al dispositivo; el QR se valida en el mismo kiosco o en dispositivos que compartan el secreto.
- La app no reemplaza un control de acceso físico certificado.

## Privacidad

- No se guardan fotografías: los frames de cámara existen solo en memoria.
- Los embeddings se cifran con AES-256-GCM usando una clave no exportable del Android Keystore.
- Respaldo en la nube (`allowBackup`) desactivado; preferencias y base de datos excluidas de backup/transferencia.
- «Eliminar mis datos» borra el registro del alumno y todos sus embeddings.
