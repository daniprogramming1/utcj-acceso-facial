# Arquitectura — Acceso UTCJ

## Visión general

MVVM con capas *clean-ish*:

```
UI (Compose + ViewModels Hilt)
   │  StateFlow / suspend
   ▼
Repositorios (data/repository)  ──►  Room (data/local)
   │                                  EncryptedSharedPreferences (data/security)
   ├─► QR firmado (domain/qr + data/qr)    Android Keystore (llave EC del alumno)
   ├─► Fuentes remotas (data/remote): CSV de estatus, Firestore (opcional)
   └─► Cola de sincronización (data/sync) ──► WorkManager (SyncWorker)
```

- **Inyección**: Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, `@HiltWorker`).
- **Persistencia**: Room `acceso_utcj.db` versión **2** (alumnos + llave pública, nonces usados, eventos de acceso, visitantes, incidentes, cola de sincronización, resultados de evaluación). Migraciones explícitas en `AppDatabase.ALL_MIGRATIONS` (ver [Migración 1 → 2](#migración-de-base-de-datos-1--2)).
- **Navegación**: Navigation Compose, rutas en `ui/navigation/Routes.kt`, grafo en `NavGraph.kt` (ver [Navegación](#navegación)).
- **MainActivity** (`FragmentActivity`) instala el *splash* (`core-splashscreen`), activa *edge-to-edge*, calcula el `WindowSizeClass` y aplica el tema elegido (`SettingsRepository.themeModeFlow`).
- **Pantallas = envoltorio + contenido**: cada pantalla tiene un composable con Hilt/ViewModel (`KioskScreen`, `StudentsSection`…) y un composable **sin estado** (`KioskContent`, `StudentsContent`…) que recibe datos y lambdas. Las pruebas de capturas usan solo los segundos.

## QR firmado

### Formatos (`domain/qr/QrCodec.kt`)

Texto separado por `.`; campos de texto y bytes en **base64url sin relleno**, tiempos en **segundos** Unix.

| QR | Formato | Lo firma |
|---|---|---|
| Registro | `UTCJR1.matrícula.nombre.carrera.correo.versiónConsentimiento.llavePública.emitido.firma` | La llave privada del propio alumno (prueba de posesión) |
| Acceso | `UTCJA1.matrícula.emitido.expira.nonce.firma` | La llave privada del alumno; nonce aleatorio de 12 bytes |

- Firma: **ECDSA P-256 con SHA-256** (`SHA256withECDSA`) sobre todo lo anterior al último `.`. La llave pública va en X.509 (DER).
- `QrCodec.parse()` nunca lanza: devuelve `Registration`, `Access` o `Invalid`.
- Llave del alumno: `AndroidKeystoreQrKeyStore` (alias `utcj_qr_<matrícula>`, `secp256r1`, solo `PURPOSE_SIGN`, **no exportable**). En pruebas se usa `SoftwareQrSigner` / `InMemoryQrKeyStore` detrás de las interfaces `QrSigner` / `QrKeyStore` (`di/QrModule`).

### Registro y aprobación

```
Teléfono del alumno: Datos → Aviso de privacidad → QrKeyStore.getOrCreate(matrícula)
  → StudentRepository.registerWithConsent (PENDIENTE local) → QrCrypto.registrationQr → QR de registro
Teléfono del guardia: Escanear → QrAccessService.scan → RegistrationDecider.check
  · firma válida con la llave incluida, si no ⇒ «QR no válido»
  · estatus previo BAJA/SUSPENDIDO (CSV) ⇒ bloqueado (no se puede aprobar)
  → Aprobar / Rechazar → StudentRepository.approveFromRegistration (estatus + llave pública en Room)
```

### Verificación de acceso (panel y kiosco)

```
CameraX (ImageAnalysis, KEEP_ONLY_LATEST) → ML Kit Barcode (solo QR) → ScanDebouncer (mismo texto ignorado 4 s)
  → QrAccessService.scan(raw, ahora)
       1. borra nonces vencidos
       2. AccessDecider.decide (lógica pura, en este orden):
          no registrado → sin llave → firma → expira ≤ emitido (dañado) → emitido − 30 s > ahora («Hora del QR inválida»)
          → vigencia > máximo del guardia → ahora − 30 s > expira («QR vencido») → nonce usado («QR ya usado»)
          → estatus (BAJA, SUSPENDIDO, PENDIENTE, RECHAZADO) → horario
       3. si la decisión consume el nonce: INSERT OR IGNORE en used_nonces (atómico; si ya existía ⇒ «QR ya usado»)
  → Panel: tarjeta del alumno; denegado se registra al instante; permitido espera «Registrar entrada» / «No permitir»
    (si se sale sin decidir se registra DENEGADO «Sin confirmar por el guardia»)
  → Kiosco: se registra solo (guardia «Kiosco») y muestra la capa verde/roja con el motivo
  → AccessLogRepository.log(método QR, resultado, motivo, guardia, duración) → Room + SyncQueue + SyncWorker
```

- Los nonces se guardan hasta `expira + 30 s`, así que un QR no puede reutilizarse mientras sea válido.
- Motivos de rechazo (`DenialReason`, título + explicación en español): «QR no válido», «QR de registro» (mostrado en el kiosco), «Alumno no registrado», «Sin QR de registro», «Firma inválida», «Hora del QR inválida», «Vigencia no permitida», «QR vencido», «QR ya usado», «Alumno dado de baja», «Alumno suspendido», «Pendiente de aprobación», «Registro rechazado», «Fuera de horario», «Denegado por el guardia».
- Respaldo: **Entrada manual** con motivo (evento MANUAL con el guardia en turno).

## Dónde configurar

| Parámetro | Dónde | Predeterminado |
|---|---|---|
| **Vigencia del QR de acceso** (alumno) | `SettingsRepository.getStudentQrValiditySec()` (`DEFAULT_STUDENT_QR_VALIDITY_SEC`, opciones `STUDENT_QR_VALIDITY_OPTIONS`); el alumno la elige en **Mi acceso → Vigencia del QR** | **60 s** (30 s / 1 / 2 / 5 min) |
| **Vigencia máxima aceptada** (guardia) | `SettingsRepository.getGuardMaxQrValiditySec()`, **Panel → Configuración → Acceso con QR** | **5 min** |
| Tolerancia de reloj | `GuardPolicy.DEFAULT_CLOCK_SKEW_SEC` | ±30 s |
| **Horario permitido** | `SettingsRepository.getHoursStart()/getHoursEnd()` (`DEFAULT_HOURS_START`/`END`), editable en Configuración; zona `America/Ciudad_Juarez` (`TimeUtil`) | 6:00–22:00 |
| Regreso del kiosco tras un resultado | `SettingsRepository.getKioskIdleMs()` (`DEFAULT_KIOSK_IDLE_MS`, opciones `KIOSK_IDLE_OPTIONS_MS`), **Configuración → Modo kiosco** | **10 s** (antes 30 s) |
| Orientación / sonido del kiosco | `SettingsRepository.getKioskOrientation()` / `isKioskSoundEnabled()` | Horizontal / activado |
| Tema | `SettingsRepository.themeModeFlow` (`ThemeMode`), **Configuración → Apariencia** | Sistema |
| Marca | `brand/BrandConfig.kt` | UTCJ |
| **Contraseña del guardia** | Se define la primera vez (`FirstPasswordSetupScreen`) y se cambia en **Panel → Configuración → Seguridad** (o **Más → Cambiar contraseña** en teléfono). Lógica en `GuardAuthManager` | — |
| Política de bloqueo | `GuardAuthManager.MAX_ATTEMPTS` / `LOCKOUT_MS` | 5 intentos / 5 min |
| PBKDF2 | `PasswordHasher.ITERATIONS`, `KEY_LENGTH_BITS`, `SALT_BYTES` | 120 000 / 256 bits / 16 bytes |
| Versión de consentimiento | `ConsentRecord.CURRENT_VERSION` | 2.0.0 |
| Firestore | `FirestoreDataSource.FIRESTORE_COMPILED_IN` + Gradle (ver README) | Desactivado |

Las contraseñas olvidadas no se pueden recuperar (solo existe el hash). Restablecer = borrar datos de la app (Ajustes de Android → Apps → Acceso UTCJ → Almacenamiento → Borrar datos), lo que también elimina los registros locales.

## Seguridad

- **Contraseña**: PBKDF2WithHmacSHA256, sal aleatoria por contraseña, hash y sal en hex dentro de `EncryptedSharedPreferences` (AES256-SIV para claves, AES256-GCM para valores). Comparación en tiempo constante. Los `CharArray` se limpian tras usarse.
- **Bloqueo**: contador de fallos + `lock_until_ms` persistidos; el reloj es inyectable (`GuardAuthManager.clock`) para pruebas.
- **QR**: la llave privada del alumno es no exportable (Android Keystore); el guardia solo guarda la pública. Cada QR de acceso caduca, tiene un nonce de un solo uso (inserción atómica) y el guardia limita la vigencia máxima. No se guardan fotos: la cámara solo decodifica QR en memoria.
- **Manifest**: `usesCleartextTraffic=false`, `allowBackup=false`, reglas de extracción que excluyen BD y preferencias.
- **Bitácora**: el DAO no expone `UPDATE`/`DELETE` de eventos salvo marcar `synced`; la UI es de solo lectura.
- **Registro**: `StudentRepository.registerWithConsent` conserva un estatus BAJA/SUSPENDIDO previo (volver a registrarse no lo convierte en PENDIENTE). El QR de registro de un alumno BAJA/SUSPENDIDO no se puede aprobar. El permiso de cámara se pide en tiempo de ejecución (`CameraPermissionGate`) y la cámara es opcional en el manifiesto.
- **Preferencias cifradas dañadas**: si `EncryptedSharedPreferences` no puede abrirse (keyset dañado, clave del Keystore perdida), `SecurePrefs` borra el archivo y la clave maestra y los recrea **vacíos** (nunca sin cifrar); se registra en log y el guardia debe definir otra vez su contraseña.
- **Kiosco**: «Llamar al guardia» solo crea una incidencia local (`IncidentRepository`), no concede acceso. La salida del kiosco sigue exigiendo reautenticación.
- **Preferencias obsoletas**: `SettingsRepository.purgeObsoleteKeys()` (al arrancar) borra las claves del reconocimiento facial y el antiguo secreto HMAC del QR.

## Migración de base de datos 1 → 2

`AppDatabase.MIGRATION_1_2` (sin borrado destructivo; solo hay *fallback* destructivo al **bajar** de versión):

1. `ALTER TABLE students ADD COLUMN correo TEXT` y `ADD COLUMN publicKey TEXT` (los alumnos existentes quedan con llave `NULL` ⇒ «Sin QR» hasta que vuelvan a registrarse).
2. `CREATE TABLE used_nonces (nonce PK, matricula, expiresAtMs)` + índice en `expiresAtMs`.
3. `DROP TABLE face_embeddings` (y su índice): se eliminan las plantillas faciales.

La bitácora conserva los eventos históricos con método FACE/FINGERPRINT y su columna `similarity` (ya no se escribe). Probada en `data/MigrationTest` con el esquema exacto de la v1.

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
                                              │               └► Registro: Datos → Aviso de privacidad → Listo (QR de registro)
                                              ├─ ¿Ya te registraste? ─► Consultar matrícula ─► Mi acceso
                                              └─ Personal de seguridad ─► Login (o configuración inicial)
                                                                          └► AdminShell
                                                                               ├─ Inicio · Escanear QR · Aprobaciones · Bitácora · Alumnos
                                                                               ├─ Visitantes · Incidencias · Configuración
                                                                               ├─ Entrada manual ─ Cambiar contraseña ─ Modo evaluación (7 toques en «Versión»)
                                                                               └─ Modo kiosco ─► (Atrás / candado) Reautenticación ─► Panel
```

- Rutas en `Routes.kt`; transiciones *fade + slide* (solo *fade* con movimiento reducido).
- **AdminShell** (`ui/admin/AdminShell.kt`) cambia de sección por estado (`AdminSection`), no por rutas, con `AnimatedContent`. Según `WindowSizeClass`:
  - **Compact** (teléfono): `NavigationBar` con Inicio, **Escanear**, Aprobar (insignia con pendientes), Bitácora y **Más** (Alumnos, Visitantes, Incidencias, Configuración, Entrada manual, Kiosco, Cerrar sesión).
  - **Medium**: `NavigationRail` con todas las secciones y botón flotante de kiosco.
  - **Expanded**: `PermanentDrawerSheet` con marca, botón «Iniciar modo kiosco», secciones, guardia en turno y estado de sincronización.
- Cada sección usa `SectionScaffold` (barra superior con logo, FAB, *snackbar*) y aplica los *insets* de `LocalSectionInsets`. Las acciones globales (kiosco, entrada manual, cerrar sesión…) llegan por `LocalAdminActions`.
- El resultado del kiosco es una **capa** dentro de `KioskScreen` (ya no existe `KioskResultScreen`); mientras se muestra, el ViewModel ignora códigos. `ExternalQrInput` permite entregar códigos que no vienen de la cámara (lectores externos, pruebas) a la pantalla de escaneo visible.

## Sistema de diseño

| Pieza | Archivo | Notas |
|---|---|---|
| Marca | `brand/BrandConfig.kt` | Nombre, institución, soporte, zona horaria, `BrandPalette`, `logoRes` |
| Color | `ui/theme/Color.kt` | Esquemas claro/oscuro completos (incl. `surfaceContainer*`), escala neutra, `ExtendedColors` (éxito/aviso/peligro/info + contenedores, colores de gráficas, degradado del héroe) vía `AppTheme.extended` |
| Tipografía | `ui/theme/Type.kt` | Plus Jakarta Sans (TTF en `res/font`, OFL), escala M3 completa y `KioskType` (64/44/26 sp) |
| Formas | `ui/theme/Shape.kt` | `AppShapes`: botón 16, tarjeta 20, campo 14, píldora |
| Espaciado | `ui/theme/Spacing.kt` | `xxs…huge`, márgenes por tamaño de ventana, `contentMaxWidth` 640, `formMaxWidth` 520, altura de botón 56 |
| Movimiento | `ui/theme/Motion.kt` | Duraciones/curvas; `LocalReducedMotion` (escala de animación del sistema = 0) |
| Componentes | `ui/components/*` | `AccesoTopBar`, `PrimaryButton`/`SecondaryButton`/`TonalButton`/`DangerButton`, `KpiCard`, `SectionHeader`, `AlertBanner`, `StatusPill` (Aprobado/Pendiente/Baja/Rechazado…), `EmptyState`, `Modifier.shimmer`/`SkeletonList`, `ConfirmDialog`, `AppSearchField`, `PasswordField` + `PasswordStrengthMeter`, `InitialsAvatar`, `AppListItem`, `StepIndicator`, gráficas Canvas (`BarChart`, `DonutChart`, `LineChart`), `QrScannerCamera`, `ScanFrameOverlay`, `GuidanceChip`, `CountdownRing`, `CameraPermissionGate`, ilustraciones vectoriales en Canvas |

Reglas: textos en español; `contentDescription` en íconos con significado; objetivos táctiles ≥ 48 dp; contraste AA (texto secundario sobre superficies ≥ 4.5:1); sin contenido recortado (columnas con *scroll*, `maxLines` + elipsis); el kiosco siempre usa su paleta oscura de alto contraste.

Lógica pura separada de la UI para probarla en JVM: `domain/qr` (formato, firma, decisión), `domain/analytics` (KPI, series, alertas), `domain/validation` (formulario de registro), `data/security/PasswordStrength`, `util/Initials`, filtros de `ui/admin` (`filterStudents`, `filterEvents`, `groupByDay`, `LogRange`).

## UI (resumen)

- Material 3 con la paleta de `BrandConfig` (teal `#00796B` / `#14B8A6`, azul `#1D4ED8`, azul marino `#0B1F3A`).
- Logo provisional vectorial `res/drawable/ic_brand_logo.xml`; ícono adaptable con capa monocroma (`mipmap-anydpi/ic_launcher.xml`).
- `util/WindowSizeClass.kt` → `LocalWindowSizeClass` (desde `calculateWindowSizeClass`) con respaldo por configuración para pruebas.
- Todos los textos visibles están en español.
