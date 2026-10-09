# Acceso UTCJ — Control de acceso con QR firmado para campus

App Android nativa para controlar el acceso de alumnos a la **Universidad Tecnológica de Ciudad Juárez (UTCJ)** con un **QR temporal firmado** que genera el teléfono del alumno y escanea el guardia (o la cámara del kiosco). Incluye bitácora auditable, aprobación por el guardia, funcionamiento sin conexión y un panel administrativo adaptable a teléfono, tableta y kiosco.

> Desde la **v1.2.0** ya no hay reconocimiento facial ni huella: no se capturan rostros ni biometría de ningún tipo. La marca es configurable (*white-label*): ver [Cambiar la marca](#cambiar-la-marca-white-label).

## Capturas de pantalla

Generadas con Roborazzi (Robolectric, sin emulador) y datos ficticios — ver [TESTING.md](TESTING.md#4-capturas-de-pantalla-roborazzi).

| Bienvenida | Selección de rol | Registro: datos | Registro: aviso de privacidad |
|---|---|---|---|
| ![](docs/screenshots/01-onboarding.png) | ![](docs/screenshots/02-seleccion-de-rol.png) | ![](docs/screenshots/03b-registro-datos.png) | ![](docs/screenshots/03-registro-consentimiento.png) |

| Alumno: QR de acceso (cuenta regresiva) | Alumno: QR de registro (pendiente) | Guardia: escáner | Guardia: aprobar registro |
|---|---|---|---|
| ![](docs/screenshots/04-alumno-qr-acceso.png) | ![](docs/screenshots/04b-alumno-qr-registro.png) | ![](docs/screenshots/13-guardia-escaner.png) | ![](docs/screenshots/16-guardia-aprobar-registro.png) |

| Guardia: acceso permitido | Guardia: acceso denegado |
|---|---|
| ![](docs/screenshots/14-guardia-acceso-permitido.png) | ![](docs/screenshots/15-guardia-acceso-denegado.png) |

| Kiosco en espera (horizontal) | Kiosco: permitido | Kiosco: denegado |
|---|---|---|
| ![](docs/screenshots/05-kiosco-espera.png) | ![](docs/screenshots/06-kiosco-acceso-permitido.png) | ![](docs/screenshots/07-kiosco-acceso-denegado.png) |

| Inicio de sesión del guardia | Bloqueo por intentos | Panel (teléfono) | Panel (modo oscuro) |
|---|---|---|---|
| ![](docs/screenshots/08-guardia-inicio-de-sesion.png) | ![](docs/screenshots/08b-guardia-bloqueo.png) | ![](docs/screenshots/09-panel-inicio-telefono.png) | ![](docs/screenshots/09c-panel-inicio-oscuro.png) |

| Panel en tableta (cajón permanente) |
|---|
| ![](docs/screenshots/09b-panel-inicio-tableta.png) |

| Alumnos | Bitácora | Configuración | Rol (oscuro) |
|---|---|---|---|
| ![](docs/screenshots/10-panel-alumnos.png) | ![](docs/screenshots/11-panel-bitacora.png) | ![](docs/screenshots/12-panel-configuracion.png) | ![](docs/screenshots/02b-seleccion-de-rol-oscuro.png) |

## Cómo funciona

**Alumno (su propio teléfono)**
1. **Soy alumno** → Datos (nombre, matrícula, carrera/grupo, correo opcional) → Aviso de privacidad → *Acepto y generar mi QR*.
2. El teléfono crea un par de llaves **EC P-256 en el Android Keystore** (la privada nunca sale del teléfono) y muestra su **QR de registro**: «Pendiente: muéstrale este QR al guardia para activar tu acceso».
3. Una vez aprobado, en **Mi acceso → QR de acceso** aparece un QR firmado que **se renueva solo** con un anillo de cuenta regresiva (vigencia **1 min** por defecto; 30 s / 1 / 2 / 5 min). El brillo sube al máximo mientras se muestra.
4. Lo muestra en caseta (al guardia o a la cámara del kiosco). Cada QR sirve **una sola vez**.

**Guardia (otro teléfono o tableta)**
1. Panel → **Aprobar** → *Escanear QR* (o pestaña **Escanear**) → escanea el QR de registro del alumno.
2. Revisa la tarjeta (nombre, matrícula, carrera, correo, estatus institucional) y su credencial → **Aprobar acceso** / *Rechazar*. Si el CSV dice BAJA o SUSPENDIDO, no se puede aprobar.
3. En la entrada: pestaña **Escanear** → escanea el QR de acceso → tarjeta del alumno con iniciales, nombre, matrícula, carrera, estatus, hora y **Acceso permitido** (verde) o **Acceso denegado** (rojo) con el motivo exacto.
4. **Registrar entrada** (o *No permitir*). En **Modo kiosco** la cámara frontal escanea y registra sola. Todo escaneo queda en la bitácora (método QR, resultado, motivo, guardia).

## Características

| Área | Qué hace |
|---|---|
| Diseño | Sistema de diseño propio (Material 3): tema claro/oscuro/sistema, Plus Jakarta Sans incluida (sin internet), componentes reutilizables, movimiento reducido respetado, textos 100 % en español |
| Primer uso | *Splash* de Android 12+, 3 páginas de bienvenida y selección de rol con tarjetas ilustradas |
| Alumno | Asistente **Datos → Aviso de privacidad → Listo**; **Mi acceso** con pestañas *QR de acceso* (cuenta regresiva, vigencia elegible, brillo máximo) y *QR de registro*; **Eliminar mis datos** (borra también la llave del Keystore) |
| QR firmado | `UTCJA1.matrícula.emitido.expira.nonce.firma` con **ECDSA P-256 / SHA-256**; el guardia verifica firma con la llave pública guardada al aprobar, vigencia (±30 s de tolerancia de reloj), vigencia máxima, **nonce de un solo uso**, estatus y horario — todo **sin conexión** |
| Kiosco | Horizontal por defecto, inmersivo y siempre encendido; cámara frontal con recuadro y línea animada; resultado a pantalla completa verde/rojo con nombre, matrícula, iniciales, hora y motivo; **Llamar al guardia** (crea una incidencia); regreso automático (10 s por defecto) |
| Guardia | Contraseña PBKDF2-HMAC-SHA256 (sal aleatoria, 120 000 iteraciones); bloqueo de 5 min tras 5 intentos con cuenta regresiva; salir del kiosco exige contraseña; **Entrada manual** con motivo como respaldo |
| Panel administrativo | Navegación adaptable (barra inferior *Inicio · Escanear · Aprobar · Bitácora · Más*, riel o cajón en tableta). Tablero con KPI y gráficas, **Escanear QR**, **Aprobaciones**, **Alumnos** («QR registrado» / «Sin QR»), **Bitácora** (CSV y PDF), **Visitantes**, **Incidencias**, **Configuración** (vigencia máxima del QR, horario, kiosco, CSV, tema) |
| Privacidad | Sin fotos ni biometría; solo datos personales y una llave **pública**; consentimiento versionado (2.0.0); aprobación obligatoria; bitácora de solo lectura |
| Sin conexión | Todo funciona sin red; indicador *En línea / Sin conexión / Sincronizando / Pendientes: N*; cola WorkManager sin duplicados |
| Estatus institucional | CSV (muestra en `assets/` + importación); BAJA/SUSPENDIDO = sin acceso ni aprobación, y volver a registrarse **no** restablece ese estatus |

## Requisitos

- Android Studio Hedgehog (2023.1.1) o más reciente
- JDK 17
- Android SDK 34
- Para el flujo completo: **dos teléfonos** (alumno y guardia) o un teléfono + tableta de kiosco, con cámara
- minSdk 26 (Android 8.0)

## Compilar y ejecutar

```bash
git clone https://github.com/daniprogramming1/utcj-acceso-facial.git
cd utcj-acceso-facial
cp local.properties.example local.properties   # ajusta sdk.dir (Android Studio lo crea solo)
./gradlew assembleDebug                        # APK en app/build/outputs/apk/debug/
./gradlew installDebug                         # instala en el dispositivo conectado
./gradlew testDebugUnitTest                    # pruebas unitarias + render de capturas
./gradlew recordRoborazziDebug                 # regenera docs/screenshots/*.png
```

O ábrelo en Android Studio → *Open* → selecciona la carpeta → espera el **Gradle Sync** → *Run ▶*.

### Primer uso
1. **Teléfono del guardia**: bienvenida (o *Omitir*) → **Personal de seguridad** → nombre de la caseta y contraseña (mínimo 6 caracteres). El CSV de muestra (`app/src/main/assets/students_status.csv`) se importa automáticamente.
2. **Teléfono del alumno**: **Soy alumno** → Datos → Aviso de privacidad → aparece el QR de registro.
3. Guardia: Panel → **Aprobar** → *Escanear QR* → escanea el QR de registro → **Aprobar acceso**.
4. Alumno: *Ya me aprobaron* → **QR de acceso**. Guardia: pestaña **Escanear** (o **Modo kiosco**) → escanea → tarjeta del alumno → **Registrar entrada**.

### Actualizar desde la v1.1.x
No hace falta desinstalar: la base de datos migra sola (v1 → v2) conservando alumnos, bitácora, visitantes e incidencias, y se eliminan las plantillas faciales. Los alumnos ya registrados aparecen como **«Sin QR»**: deben abrir *Soy alumno* en su teléfono, volver a registrarse y mostrar su QR de registro al guardia para ligar su llave.

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
  di/              Hilt: AppModule, DatabaseModule, SecurityModule, QrModule
  domain/qr        QrCodec (formato), QrCrypto (firma/verificación ECDSA), AccessDecision (reglas del guardia)
  data/qr          AndroidKeystoreQrKeyStore (llave del alumno), QrAccessService (verificación + nonces + bitácora)
  data/local       Room: entidades, DAOs (incl. UsedNonceDao), AppDatabase + migraciones, Converters
  data/remote      FirestoreDataSource, StudentStatusCsvDataSource
  data/repository  Student, AccessLog, Auth, Sync, Settings, Visitor, Incident
  data/security    PasswordHasher, GuardAuthManager, SecurePrefs, KeyValueStore
  data/sync        SyncWorker, SyncQueue
  domain/model     Student, AccessEvent, Visitor, Incident, ConsentRecord, GuardSession
  domain/analytics DashboardCalculator, AlertsEngine
  domain/validation RegistrationValidator
  data/export      PdfReportBuilder (reporte PDF con marca)
  brand/           BrandConfig
  ui/theme         Color, Type, Shape, Spacing, Motion, Theme
  ui/components    Botones, tarjetas, chips, gráficas, QrScanner (CameraX + ML Kit), Illustrations…
  ui/scan          Escáner del panel: ScanViewModel, ScanSection, tarjetas de alumno/registro
  ui/              onboarding, role, student, guard, kiosk, admin (AdminShell + secciones), navigation
  util/            WindowSizeClass, TimeUtil, ScreenBrightness, Initials, AppResult
```

Más detalles en [ARCHITECTURE.md](ARCHITECTURE.md). Guía de pruebas (incluido el plan con dos teléfonos) en [TESTING.md](TESTING.md).

## Cambiar la marca (white-label)

Todo lo visible de la marca vive en un solo lugar:

1. **`app/src/main/java/edu/utcj/acceso/brand/BrandConfig.kt`**: `APP_NAME`, `INSTITUTION_NAME`, `INSTITUTION_SHORT`, `TAGLINE`, `SUPPORT_EMAIL`, `SUPPORT_WEBSITE`, `SECURITY_DESK_LABEL`, `TIME_ZONE_ID` y la paleta `BrandPalette` (primario, secundario, acento, azul marino). El tema claro/oscuro, el kiosco, el PDF y las ilustraciones se derivan de ahí.
2. **Logo**: reemplaza `res/drawable/ic_brand_logo.xml` (vector; o apunta `BrandConfig.logoRes` a otro recurso).
3. **Ícono del launcher**: `res/drawable/ic_launcher_background.xml`, `ic_launcher_foreground.xml` y `ic_launcher_monochrome.xml` (ícono temático de Android 13).
4. **Splash y colores XML**: `res/values/colors.xml` (`brand_navy`, `window_background_*`) y `res/values/themes.xml`.
5. **Nombre en el launcher**: `app_name` en `res/values/strings.xml`.
6. **Paquete** (opcional, para publicar otra app): `namespace`/`applicationId` en `app/build.gradle.kts`.
7. Ejecuta `./gradlew recordRoborazziDebug` para regenerar las capturas con la nueva marca.

> `SUPPORT_EMAIL` (`soporte.acceso@utcj.edu.mx`) y el logo angular son **provisionales**: confírmalos/reemplázalos por los oficiales antes de distribuir.

## Limitaciones conocidas

- **Llave por teléfono**: la llave privada vive en el Keystore del teléfono del alumno y no se puede exportar. Si cambia de teléfono, borra datos o desinstala, debe registrarse de nuevo y mostrar el nuevo QR de registro al guardia (la aprobación reemplaza la llave anterior).
- **Aprobación local**: cada teléfono del guardia/kiosco guarda sus propias aprobaciones, llaves públicas y nonces usados (sin servidor). Con varias casetas, el alumno debe aprobarse en cada dispositivo (o sincronizar `students` por Firestore, pendiente).
- **El teléfono del alumno no sabe cuándo lo aprueban**: por eso hay un botón *Ya me aprobaron*; el QR de acceso siempre se puede generar y es el guardia quien decide.
- **Relojes**: la vigencia depende de la hora de ambos teléfonos; se toleran ±30 s. Un teléfono con la hora muy desfasada verá «QR vencido» o «Hora del QR inválida».
- **Capturas de pantalla**: un QR copiado sirve hasta que vence o se usa una vez en ese dispositivo; por eso la vigencia es corta y el guardia puede limitar la máxima (5 min por defecto).
- La app no reemplaza un control de acceso físico certificado.

## Privacidad

- No se capturan rostros, fotos ni huellas; la cámara solo lee códigos QR y los cuadros existen solo en memoria.
- El guardia guarda: nombre, matrícula, carrera, correo (opcional), versión del consentimiento y la **llave pública** del alumno.
- Respaldo en la nube (`allowBackup`) desactivado; preferencias y base de datos excluidas de backup/transferencia.
- «Eliminar mis datos» borra el registro del alumno y su llave del Keystore (la bitácora se conserva por seguridad institucional).
- Volver a registrarse no reinicia un estatus BAJA/SUSPENDIDO a PENDIENTE.

## Licencias de terceros

- Tipografía **Plus Jakarta Sans** — SIL Open Font License 1.1 (`docs/licenses/PlusJakartaSans-OFL.txt`).
