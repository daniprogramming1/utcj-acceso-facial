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
- **Navegación**: Navigation Compose, rutas en `ui/navigation/Routes.kt`, grafo en `NavGraph.kt` (ver [Navegación](#navegación)).
- **MainActivity** extiende `FragmentActivity` para que `BiometricPrompt` funcione; instala el *splash* (`core-splashscreen`), activa *edge-to-edge*, calcula el `WindowSizeClass` y aplica el tema elegido (`SettingsRepository.themeModeFlow`).
- **Pantallas = envoltorio + contenido**: cada pantalla tiene un composable con Hilt/ViewModel (`KioskScreen`, `StudentsSection`…) y un composable **sin estado** (`KioskContent`, `StudentsContent`…) que recibe datos y lambdas. Las pruebas de capturas usan solo los segundos.

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
  → KioskResultOverlay (capa a pantalla completa verde/rojo, nombre, matrícula, motivo,
     alternativas; regreso automático a los 10 s — configurable)
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
| Regreso del kiosco tras un resultado | `SettingsRepository.getKioskIdleMs()` (`DEFAULT_KIOSK_IDLE_MS`, opciones `KIOSK_IDLE_OPTIONS_MS`), **Configuración → Modo kiosco** | **10 s** (antes 30 s) |
| Orientación / sonido del kiosco | `SettingsRepository.getKioskOrientation()` / `isKioskSoundEnabled()` | Horizontal / activado |
| Tema | `SettingsRepository.themeModeFlow` (`ThemeMode`), **Configuración → Apariencia** | Sistema |
| Marca | `brand/BrandConfig.kt` | UTCJ |
| **Contraseña del guardia** | Se define la primera vez (`FirstPasswordSetupScreen`) y se cambia en **Panel → Configuración → Seguridad** (o **Más → Cambiar contraseña** en teléfono). Lógica en `GuardAuthManager` | — |
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
- **Registro**: `StudentRepository.registerWithConsent` conserva un estatus BAJA/SUSPENDIDO previo (volver a registrarse no lo convierte en PENDIENTE). El permiso de cámara se pide en tiempo de ejecución (`CameraPermissionGate`) con explicación de privacidad.
- **Kiosco**: «Llamar al guardia» solo crea una incidencia local (`IncidentRepository`), no concede acceso. La salida del kiosco sigue exigiendo reautenticación.
- **Umbral**: el control deslizante mantiene el rango previo (0.50–0.95) y el predeterminado 0.60; por debajo de 0.60 la UI muestra una advertencia.

## Sincronización sin conexión

1. Cada evento recibe un `syncKey` UUID (índice único).
2. `SyncQueue.enqueue()` usa `INSERT OR IGNORE` + verificación previa ⇒ **sin duplicados en cola**.
3. `SyncWorker` (único, `ExistingWorkPolicy.KEEP`, requiere red, backoff exponencial, más un periódico cada 15 min) sube cada evento; si ya está `synced`, solo lo saca de la cola ⇒ **sin doble subida**. Con Firestore, usar `syncKey` como ID de documento hace la escritura idempotente.
4. `SyncRepository.observeStatus()` combina red + tamaño de cola → *En línea / Sin conexión / Sincronizando / Pendientes: N*.

## Estatus institucional

`StudentStatusRepository` (interfaz) ← `CsvBackedStudentStatusRepository` (enlazada en `AppModule`). Lee `assets/students_status.csv` en el primer arranque o un CSV importado con el selector de documentos (Configuración).

Formato: `matricula,nombre,status,carrera` con `status ∈ {ACTIVO, BAJA, SUSPENDIDO, ...}`.

**Puerta para API institucional futura**: crea `ApiStudentStatusRepository : StudentStatusRepository` (Retrofit/Ktor) y cambia el `@Binds` en `di/AppModule.kt`. El kiosco ya deniega BAJA/SUSPENDIDO sin importar el origen.

## Navegación

```
Splash ─► Onboarding (solo la primera vez) ─► Selección de rol
                                              ├─ Soy alumno ─► (matrícula recordada) Mi acceso ─► Eliminar mis datos
                                              │               └► Registro: Datos → Consentimiento → Captura → Listo
                                              ├─ ¿Ya te registraste? ─► Consultar matrícula ─► Mi acceso
                                              └─ Personal de seguridad ─► Login (o configuración inicial)
                                                                          └► AdminShell
                                                                               ├─ Inicio · Alumnos · Aprobaciones · Bitácora
                                                                               ├─ Visitantes · Incidencias · Configuración
                                                                               ├─ Entrada manual ─ Cambiar contraseña ─ Modo evaluación (7 toques en «Versión»)
                                                                               └─ Modo kiosco ─► (Atrás / candado) Reautenticación ─► Panel
```

- Rutas en `Routes.kt`; transiciones *fade + slide* (solo *fade* con movimiento reducido).
- **AdminShell** (`ui/admin/AdminShell.kt`) cambia de sección por estado (`AdminSection`), no por rutas, con `AnimatedContent`. Según `WindowSizeClass`:
  - **Compact** (teléfono): `NavigationBar` con Inicio, Alumnos, Aprobar (insignia con pendientes), Bitácora y **Más** (Visitantes, Incidencias, Configuración, Entrada manual, Kiosco, Cerrar sesión).
  - **Medium**: `NavigationRail` con todas las secciones y botón flotante de kiosco.
  - **Expanded**: `PermanentDrawerSheet` con marca, botón «Iniciar modo kiosco», secciones, guardia en turno y estado de sincronización.
- Cada sección usa `SectionScaffold` (barra superior con logo, FAB, *snackbar*) y aplica los *insets* de `LocalSectionInsets`. Las acciones globales (kiosco, entrada manual, cerrar sesión…) llegan por `LocalAdminActions`.
- El resultado del kiosco es una **capa** dentro de `KioskScreen` (ya no existe `KioskResultScreen`); mientras se muestra, el ViewModel ignora fotogramas.

## Sistema de diseño

| Pieza | Archivo | Notas |
|---|---|---|
| Marca | `brand/BrandConfig.kt` | Nombre, institución, soporte, zona horaria, `BrandPalette`, `logoRes` |
| Color | `ui/theme/Color.kt` | Esquemas claro/oscuro completos (incl. `surfaceContainer*`), escala neutra, `ExtendedColors` (éxito/aviso/peligro/info + contenedores, colores de gráficas, degradado del héroe) vía `AppTheme.extended` |
| Tipografía | `ui/theme/Type.kt` | Plus Jakarta Sans (TTF en `res/font`, OFL), escala M3 completa y `KioskType` (64/44/26 sp) |
| Formas | `ui/theme/Shape.kt` | `AppShapes`: botón 16, tarjeta 20, campo 14, píldora |
| Espaciado | `ui/theme/Spacing.kt` | `xxs…huge`, márgenes por tamaño de ventana, `contentMaxWidth` 640, `formMaxWidth` 520, altura de botón 56 |
| Movimiento | `ui/theme/Motion.kt` | Duraciones/curvas; `LocalReducedMotion` (escala de animación del sistema = 0) |
| Componentes | `ui/components/*` | `AccesoTopBar`, `PrimaryButton`/`SecondaryButton`/`TonalButton`/`DangerButton`, `KpiCard`, `SectionHeader`, `AlertBanner`, `StatusPill` (Aprobado/Pendiente/Baja/Rechazado…), `EmptyState`, `Modifier.shimmer`/`SkeletonList`, `ConfirmDialog`, `AppSearchField`, `PasswordField` + `PasswordStrengthMeter`, `InitialsAvatar`, `AppListItem`, `StepIndicator`, gráficas Canvas (`BarChart`, `DonutChart`, `LineChart`), `FaceGuideOverlay`, `GuidanceChip`, `SampleDots`, `CameraPermissionGate`, ilustraciones vectoriales en Canvas |

Reglas: textos en español; `contentDescription` en íconos con significado; objetivos táctiles ≥ 48 dp; contraste AA (texto secundario sobre superficies ≥ 4.5:1); sin contenido recortado (columnas con *scroll*, `maxLines` + elipsis); el kiosco siempre usa su paleta oscura de alto contraste.

Lógica pura separada de la UI para probarla en JVM: `domain/analytics` (KPI, series, alertas), `domain/validation` (formulario de registro), `data/security/PasswordStrength`, `util/Initials`, filtros de `ui/admin` (`filterStudents`, `filterEvents`, `groupByDay`, `LogRange`).

## UI (resumen)

- Material 3 con la paleta de `BrandConfig` (teal `#00796B` / `#14B8A6`, azul `#1D4ED8`, azul marino `#0B1F3A`).
- Logo provisional vectorial `res/drawable/ic_brand_logo.xml`; ícono adaptable con capa monocroma (`mipmap-anydpi/ic_launcher.xml`).
- `util/WindowSizeClass.kt` → `LocalWindowSizeClass` (desde `calculateWindowSizeClass`) con respaldo por configuración para pruebas.
- Todos los textos visibles están en español.
