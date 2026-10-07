# Arquitectura — Acceso UTCJ

## Visión general

MVVM con capas *clean-ish*:

```
UI (Compose + ViewModels Hilt)
   │  StateFlow / suspend
   ▼
Repositorios (data/repository)  ──►  Room (data/local)
   │                                  EncryptedSharedPreferences (data/security)
   ├─► Motor biométrico (data/biometric)   Android Keystore (EmbeddingCrypto)
   ├─► Fuentes remotas (data/remote): CSV de estatus, Firestore (opcional)
   └─► Cola de sincronización (data/sync) ──► WorkManager (SyncWorker)
```

- **Inyección**: Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, `@HiltWorker`).
- **Persistencia**: Room `acceso_utcj.db` (alumnos, embeddings cifrados, eventos de acceso, visitantes, incidentes, cola de sincronización, resultados de evaluación).
- **Navegación**: Navigation Compose, rutas en `ui/navigation/Routes.kt`, grafo en `NavGraph.kt`.
- **MainActivity** extiende `FragmentActivity` para que `BiometricPrompt` funcione.

## Flujo de verificación (kiosco)

```
CameraX (ImageAnalysis, cámara frontal, KEEP_ONLY_LATEST)
  → YUV_420_888 → Bitmap (solo en memoria)
  → ML Kit FaceDetection (landmarks + clasificación ojos)
  → FaceQualityChecker  (1 rostro, tamaño, pose ±20°, brillo, nitidez) → guía en español
  → LivenessChecker     (opcional: parpadeo o giro de cabeza)
  → FaceEmbeddingEngine (alinear por ojos, recortar +25 %)
       · MobileFaceNet TFLite: 112×112 RGB → ~192-d L2  (preferido)
       · Legado histograma: 128×128 → 268-d L2          (si no hay modelo)
  → FaceMatcher         (coseno vs TODAS las muestras de alumnos aprobados; mejor coincidencia)
  → Reglas: estatus (APPROVED/ACTIVO), BAJA/SUSPENDIDO/PENDING ⇒ denegar, horario permitido
  → AccessLogRepository.log()  → Room + SyncQueue + SyncWorker
  → KioskResultScreen (verde/rojo, nombre, matrícula, reinicio a los 30 s)
```

Respaldos: **QR dinámico** (`QrTokenManager`: `base64url(matricula|expira|nonce).base64url(HMAC-SHA256)`, vigencia 30 s, comparación en tiempo constante) y **huella** (`BiometricPrompt`, `BIOMETRIC_WEAK`).

## Motor de embeddings

Contrato: `data/biometric/FaceEmbeddingEngine` (interfaz). Selección en `di/BiometricModule`:

| Motor | Clase | Entrada | Dim | Cuándo |
|---|---|---|---|---|
| **MobileFaceNet** | `MobileFaceNetEmbeddingEngine` | 112×112 RGB, `(p−127.5)/128` | ~192 | `assets/models/mobilefacenet.tflite` presente y legado desactivado |
| **Legado** | `LegacyHistogramEmbeddingEngine` | histograma 16×16 + 12 rasgos geométricos | 268 | sin modelo, error de carga, o flag `use_legacy_face_embedding` |

Alineación compartida: `FaceAlignment.alignAndCrop()` (ojos ML Kit, margen 25 %).

**Obtener el modelo**: `./scripts/download_mobilefacenet.sh` → `app/src/main/assets/models/mobilefacenet.tflite` (ver `assets/models/README.md`). El binario no se versiona (`.gitignore` `*.tflite`).

**Migración**: embeddings de distintos motores **no son compatibles**. `FaceMatcher` omite muestras con dimensión distinta a la sonda. Tras cambiar de modelo **vuelve a registrar** a los alumnos. Ni el cifrado (`EmbeddingCrypto`) ni Room cambian (bytes de longitud variable).

## Dónde configurar

| Parámetro | Dónde | Predeterminado |
|---|---|---|
| **Umbral facial** (coseno) | `FaceMatcher.DEFAULT_THRESHOLD` (MobileFaceNet); `LEGACY_THRESHOLD` = 0.72; runtime `SettingsRepository.getFaceThreshold()` (`face_match_threshold`), **Panel → Configuración** (0.50–0.95) | **0.60** |
| Motor facial legado | `SettingsRepository.useLegacyFaceEmbedding()` (`use_legacy_face_embedding`), switch en Configuración; requiere reiniciar la app | Desactivado |
| Liveness | `SettingsRepository.isLivenessEnabled()` / Configuración | Desactivado |
| **Horario permitido** | `SettingsRepository.getHoursStart()/getHoursEnd()` (`DEFAULT_HOURS_START`/`END`), editable en Configuración; zona `America/Ciudad_Juarez` (`TimeUtil`) | 6:00–22:00 |
| Muestras de registro | `SettingsRepository.getMinSamples()/getMaxSamples()` | 3 / 5 |
| Reinicio del kiosco | `KioskResultScreen(idleMs)` / `SettingsRepository.DEFAULT_KIOSK_IDLE_MS` | 30 s |
| **Contraseña del guardia** | Se define la primera vez (`FirstPasswordSetupScreen`) y se cambia en **Panel → Cambiar contraseña**. Lógica en `GuardAuthManager` | — |
| Política de bloqueo | `GuardAuthManager.MAX_ATTEMPTS` / `LOCKOUT_MS` | 5 intentos / 5 min |
| PBKDF2 | `PasswordHasher.ITERATIONS`, `KEY_LENGTH_BITS`, `SALT_BYTES` | 120 000 / 256 bits / 16 bytes |
| Vigencia QR | `QrTokenManager.VALIDITY_SECONDS` | 30 s |
| Versión de consentimiento | `ConsentRecord.CURRENT_VERSION` | 1.0.0 |
| Firestore | `FirestoreDataSource.FIRESTORE_COMPILED_IN` + Gradle (ver README) | Desactivado |

Las contraseñas olvidadas no se pueden recuperar (solo existe el hash). Restablecer = borrar datos de la app (Ajustes de Android → Apps → Acceso UTCJ → Almacenamiento → Borrar datos), lo que también elimina los registros locales.

## Seguridad

- **Contraseña**: PBKDF2WithHmacSHA256, sal aleatoria por contraseña, hash y sal en hex dentro de `EncryptedSharedPreferences` (AES256-SIV para claves, AES256-GCM para valores). Comparación en tiempo constante. Los `CharArray` se limpian tras usarse.
- **Bloqueo**: contador de fallos + `lock_until_ms` persistidos; el reloj es inyectable (`GuardAuthManager.clock`) para pruebas.
- **Embeddings**: `EmbeddingCrypto` usa una clave AES-256 no exportable en `AndroidKeyStore`, GCM con IV aleatorio por muestra; IV y texto cifrado en Room. Las fotos jamás tocan el disco.
- **Manifest**: `usesCleartextTraffic=false`, `allowBackup=false`, reglas de extracción que excluyen BD y preferencias.
- **Bitácora**: el DAO no expone `UPDATE`/`DELETE` de eventos salvo marcar `synced`; la UI es de solo lectura.

## Sincronización sin conexión

1. Cada evento recibe un `syncKey` UUID (índice único).
2. `SyncQueue.enqueue()` usa `INSERT OR IGNORE` + verificación previa ⇒ **sin duplicados en cola**.
3. `SyncWorker` (único, `ExistingWorkPolicy.KEEP`, requiere red, backoff exponencial, más un periódico cada 15 min) sube cada evento; si ya está `synced`, solo lo saca de la cola ⇒ **sin doble subida**. Con Firestore, usar `syncKey` como ID de documento hace la escritura idempotente.
4. `SyncRepository.observeStatus()` combina red + tamaño de cola → *En línea / Sin conexión / Sincronizando / Pendientes: N*.

## Estatus institucional

`StudentStatusRepository` (interfaz) ← `CsvBackedStudentStatusRepository` (enlazada en `AppModule`). Lee `assets/students_status.csv` en el primer arranque o un CSV importado con el selector de documentos (Configuración).

Formato: `matricula,nombre,status,carrera` con `status ∈ {ACTIVO, BAJA, SUSPENDIDO, ...}`.

**Puerta para API institucional futura**: crea `ApiStudentStatusRepository : StudentStatusRepository` (Retrofit/Ktor) y cambia el `@Binds` en `di/AppModule.kt`. El kiosco ya deniega BAJA/SUSPENDIDO sin importar el origen.

## UI

- Material 3, paleta UTCJ en `ui/theme/Color.kt`: azul institucional `#1565C0`, verde azulado (teal) `#00695C` (inspirado en el logotipo angular), teal claro `#4DB6AC`.
- Logo provisional vectorial `res/drawable/ic_utcj_logo.xml` (forma angular teal, fondo transparente); reemplázalo por el oficial con el mismo nombre.
- `util/WindowSizeClass.kt` → Compact / Medium / Expanded para tabletas tipo kiosco.
- Todos los textos visibles están en español.
