# Guía de pruebas — Acceso UTCJ

## 1. Pruebas unitarias (JVM)

```bash
./gradlew testDebugUnitTest
# compuerta de calidad completa:
./gradlew assembleDebug testDebugUnitTest lintDebug
```

| Archivo | Cubre |
|---|---|
| `qr/QrTokenTest.kt` | QR de acceso con llave EC en software (`SoftwareQrSigner`): válido, vencido, alterado, firmado con otra llave, reutilizado, vigencia mayor al máximo, tolerancia de reloj (±30 s) hacia adelante y atrás, y cada motivo de `AccessDecider` (no registrado, sin llave, pendiente, rechazado, baja, suspendido, fuera de horario) |
| `qr/RegistrationQrTest.kt` | QR de registro: ida y vuelta con acentos y separadores, correo opcional, tamaño apto para QR, nombre alterado y llave cambiada fallan la firma, BAJA/SUSPENDIDO del CSV bloquean la aprobación, teléfono nuevo = reemplazo de llave, un QR de acceso no es de registro |
| `data/QrAccessServiceTest.kt` | `QrAccessService` con Room en memoria: registro → aprobación → acceso permitido, nonce de un solo uso, todos los rechazos (vencido, aún no válido, vigencia, firma, horario, baja, suspendido, no registrado, código ajeno), purga de nonces, evento QR en bitácora, `ScanDebouncer` |
| `data/MigrationTest.kt` | Migración Room 1 → 2 sobre el esquema exacto de la v1.1.x: conserva alumnos y bitácora, columnas nuevas en `NULL`, elimina `face_embeddings`, `used_nonces` funciona y Room valida el esquema |
| `security/PasswordHasherTest.kt` | Hash/verificación PBKDF2, contraseña incorrecta, sales distintas ⇒ hashes distintos, ida y vuelta hex |
| `security/GuardAuthManagerLockoutTest.kt` | Bloqueo tras 5 fallos, rechazo durante 5 min incluso con contraseña correcta, desbloqueo, reinicio del contador, cambio de contraseña |
| `security/SecurePrefsRecoveryTest.kt` | Qué errores del almacén cifrado provocan recrearlo y cuáles se propagan |
| `sync/SyncQueueDedupTest.kt` | `SyncQueue` real: clave duplicada no se encola dos veces, reintentos sin duplicar |
| `analytics/DashboardCalculatorTest.kt` | KPI del tablero: entradas de hoy vs ayer, tasa de éxito, tiempo promedio, accesos por hora, 7 días |
| `analytics/AlertsEngineTest.kt` | Alertas: 3+ denegados seguidos, un éxito rompe la racha, fuera de horario, orden |
| `validation/RegistrationValidatorTest.kt` | Matrícula, nombre y correo opcional del registro |
| `validation/PasswordStrengthTest.kt` | Medidor de fortaleza (orientativo) |
| `ui/InitialsTest.kt` | Iniciales para avatares |
| `ui/AdminFiltersTest.kt` | Búsqueda sin acentos, filtros, agrupación «Hoy/Ayer», `LogRange`, etiquetas de vigencia |
| `data/AccessEventDaoFilterTest.kt` | Regresión: filtros en `null` de la bitácora no lanzan `NullPointerException` |
| `flow/GuardFlowTest.kt` | Recorrido real con Hilt + `MainActivity` (Robolectric): configuración inicial / inicio de sesión → panel → todas las secciones → kiosco; tableta; y **flujo QR completo**: el guardia escanea un QR de registro (entregado con `ExternalQrInput`), lo aprueba (alumno APROBADO con llave en Room), escanea un QR de acceso ⇒ tarjeta «Acceso permitido» con nombre y matrícula ⇒ «Registrar entrada» (evento QR en bitácora) ⇒ QR vencido ⇒ «Acceso denegado · QR vencido» |
| `screenshots/ScreenshotTests.kt` | 22 pantallas renderizadas (prueba de humo de la UI); con `recordRoborazziDebug` además escribe los PNG (ver §4) |

Informe: `app/build/reports/tests/testDebugUnitTest/index.html`. Lint: `app/build/reports/lint-results-debug.html`.

## 2. Pruebas manuales (dos teléfonos)

Prepara: **Teléfono A** (alumno) y **Teléfono B** (guardia; opcionalmente una tableta para el kiosco), ambos con la APK instalada y la **hora automática** activada. Anota resultados en **Panel → Modo evaluación** (7 toques en «Versión»).

### 2.1 Autenticación del guardia
| # | Pasos | Resultado esperado |
|---|---|---|
| G1 | Primera ejecución → Soy guardia | Pantalla «Configurar contraseña» |
| G2 | Contraseña de 4 caracteres | «La contraseña debe tener al menos 6 caracteres» |
| G3 | Contraseñas distintas | «Las contraseñas no coinciden» |
| G4 | Cerrar sesión → entrar con contraseña correcta | Panel de guardia |
| G5 | 5 contraseñas incorrectas | Intentos restantes decrecen; al 5.º «Bloqueado 300 s» |
| G6 | Durante el bloqueo, contraseña correcta | Sigue bloqueado |
| G7 | Esperar 5 min | Entra con la correcta |
| G8 | Cambiar contraseña con la actual incorrecta / correcta | Error / éxito; la nueva funciona |
| G9 | En kiosco, botón Atrás o ícono de candado | Pide contraseña; sin ella no se sale |
| G10 | Con el bloqueo activo | Cuenta regresiva (anillo mm:ss) y sin campo de contraseña; al terminar vuelve el formulario |


### 2.2 Registro (Teléfono A) y aprobación (Teléfono B)
| # | Pasos | Esperado |
|---|---|---|
| R1 | A: Soy alumno → Datos con matrícula con guion / nombre con números / correo inválido | Error bajo el campo; no avanza |
| R2 | A: Aviso de privacidad sin marcar la casilla | «Acepto y generar mi QR» deshabilitado |
| R3 | A: Marcar → Acepto y generar mi QR → «Ver mi QR de registro» | Pestaña *QR de registro* con «Pendiente: muéstrale este QR al guardia para activar tu acceso» |
| R4 | B: Panel → Aprobar → *Escanear QR* → apuntar al QR de A | Tarjeta «QR de registro» con nombre, matrícula, carrera, correo, consentimiento y estatus institucional |
| R5 | B: **Aprobar acceso** | Mensaje «… ya puede entrar con su QR»; en Alumnos aparece «QR registrado» |
| R6 | B: Escanear el QR de registro de una matrícula BAJA del CSV (`20210003`) | «No se puede aprobar: Baja»; sin botón Aprobar |
| R7 | A: Volver a registrarse con la misma matrícula y aprobar de nuevo | La llave anterior se reemplaza (el QR viejo da «Firma inválida») |
| R8 | A: Verificar que no hay fotos: `adb shell run-as edu.utcj.acceso.debug ls -R files cache` | Ningún .jpg/.png |

### 2.3 Acceso con QR (A muestra, B escanea)
| # | Pasos | Esperado |
|---|---|---|
| Q1 | A: *Ya me aprobaron* → **QR de acceso** | QR con anillo de 60 s que se renueva solo; brillo al máximo |
| Q2 | B: pestaña **Escanear** → apuntar al QR | Tarjeta: iniciales, nombre, matrícula, carrera, estatus, hora, **Acceso permitido** en verde |
| Q3 | B: **Registrar entrada** | Aparece en Bitácora: método QR, PERMITIDO, guardia en turno |
| Q4 | **Vencimiento**: tomar captura del QR de A, esperar a que pase su vigencia + 30 s, escanear la captura | **Acceso denegado · QR vencido** (queda en bitácora) |
| Q5 | **Repetición**: escanear un QR, *Escanear otro* y volver a escanear el mismo QR (o su captura) antes de que venza | **Acceso denegado · QR ya usado** |
| Q6 | **Baja**: Configuración → importar un CSV donde esa matrícula sea BAJA → escanear un QR nuevo de A | **Acceso denegado · Alumno dado de baja** |
| Q7 | A: Vigencia 5 min; B: Configuración → Acceso con QR → máximo 1 min → escanear | **Vigencia no permitida** |
| Q8 | A: hora manual adelantada 5 min → escanear | **Hora del QR inválida** |
| Q9 | B: escanear con una matrícula no aprobada en B (otro alumno que solo se registró) | **Alumno no registrado** o **Pendiente de aprobación** |
| Q10 | B: horario fin = hora actual → escanear | **Fuera de horario** |
| Q11 | B: escanear un QR cualquiera (p. ej. una URL) | **QR no válido** |
| Q12 | B: Escanear → *No permitir* | Evento DENEGADO «Denegado por el guardia» |
| M1 | B: alumno sin teléfono → **Entrada manual** sin motivo / con motivo | Botón deshabilitado / evento MANUAL con guardia |

### 2.4 Kiosco (tableta o Teléfono B)
| # | Pasos | Esperado |
|---|---|---|
| K1 | Panel → Modo kiosco | Inmersivo, horizontal por defecto, recuadro con línea animada, «Muestra tu QR de acceso» |
| K2 | Mostrar el QR de acceso de A a la cámara frontal | Capa verde «Acceso permitido» con nombre y matrícula; se registra solo (guardia «Kiosco») |
| K3 | Mostrar el QR de registro en el kiosco | Rojo «QR de registro»: debe escanearse en Aprobaciones |
| K4 | Esperar en el resultado | Vuelve solo a la cámara a los **10 s** (Configuración → Modo kiosco: 5/10/15/30 s) |
| K5 | Resultado denegado → «Llamar al guardia» | «Se avisó al personal de seguridad»; aparece en Incidencias |
| K6 | Atrás o candado | Pide contraseña |

### 2.5 Bitácora, panel y sincronización
| # | Pasos | Esperado |
|---|---|---|
| L1 | Bitácora → buscar nombre/matrícula, periodo (Hoy/7/30/Personalizado) y resultado | Lista filtrada y agrupada por día |
| L2 | Ícono de descarga → CSV y PDF | Hoja para compartir; CSV abre con acentos en Excel; PDF con encabezado de marca, resumen y tabla paginada |
| L3 | Buscar un botón de editar/borrar | No existe (solo lectura) |
| P1 | Entrada manual sin motivo | Botón deshabilitado; con motivo ⇒ evento MANUAL con guardia |
| P2 | Visitantes → «Registrar visita» → «Salida» | Pasa de «Dentro» a «Historial» |
| P3 | Incidencias → «Reportar» con categoría | Aparece en la lista |
| P4 | Inicio | KPI, barras por hora, dona permitidos/denegados, tendencia de 7 días, actividad reciente |
| P5 | 3 escaneos denegados seguidos de la misma matrícula (p. ej. alumno BAJA) | Alerta «3 fallos seguidos» en Inicio |
| P6 | Teléfono / tableta vertical / tableta horizontal | Barra inferior (Inicio · Escanear · Aprobar · Bitácora · Más) / riel / cajón permanente |
| P7 | Configuración → Apariencia → Oscuro | Toda la app cambia a tema oscuro |
| P8 | Configuración → tocar 7 veces «Versión» | Abre el modo evaluación |
| O1 | Modo avión en ambos teléfonos → escanear varias veces | Indicador «Sin conexión»; acceso sigue funcionando |
| O2 | Desactivar modo avión | «Pendientes: N» → baja a 0 / «En línea»; sin eventos duplicados |

### 2.6 Privacidad y actualización
| # | Pasos | Esperado |
|---|---|---|
| D1 | A: Mi acceso → «Eliminar mis datos» | Se borran el registro y la llave del Keystore; A ya no puede generar QR hasta registrarse de nuevo |
| U1 | Instalar la v1.2.0 **encima** de la v1.1.x con alumnos y bitácora | Abre sin cerrarse; alumnos y bitácora siguen; alumnos antiguos con «Sin QR» |

## 3. Modo evaluación (oculto)

**Panel → Configuración → tocar 7 veces «Versión»**. Escenarios sugeridos: QR válido, QR vencido, QR repetido (captura), QR de otro alumno, alumno dado de baja, fuera de horario, brillo bajo, pantalla estrellada, reloj desfasado. Anota si pasó, la duración de verificación (ms, también en la columna `duracion_ms` del CSV de la bitácora) y notas.

## 4. Capturas de pantalla (Roborazzi)

Las capturas de `docs/screenshots/` se generan en la JVM con **Roborazzi 1.10 + Robolectric 4.11** (gráficos nativos), sin emulador ni dispositivo:

```bash
./gradlew recordRoborazziDebug                       # todas (escribe docs/screenshots/*.png)
./gradlew recordRoborazziDebug --tests '*ScreenshotTests*'
```

- Pruebas: `app/src/test/java/edu/utcj/acceso/screenshots/ScreenshotTests.kt` con datos ficticios deterministas (`FakeData`).
- Usan los composables **sin estado** (`KioskContent`, `DashboardContent`, `AdminShellContent`…), por eso no requieren Hilt ni cámara (la cámara se sustituye por `CameraPlaceholder` y los QR son cadenas de demostración fijas).
- `app/src/test/resources/robolectric.properties` fija `sdk=34` y una `Application` simple (sin Hilt ni Keystore).
- Dispositivos: teléfono `w393dp-h852dp-xxhdpi`, tableta/kiosco `w1280dp-h800dp-land-xhdpi`.
- Se renderiza con *movimiento reducido* y reloj de pruebas manual para que las animaciones infinitas no bloqueen la captura.
- `./gradlew testDebugUnitTest` también ejecuta estas pruebas (verifica que todas las pantallas se rendericen sin errores) pero **no** sobrescribe los PNG.
- Para agregar una captura: nueva función `@Test fun sNN_nombre() = shot("NN-nombre") { …Content(…) }`; usa `@Config(qualifiers = TABLET)` para tableta.
