# Guía de pruebas — Acceso UTCJ

## 1. Pruebas unitarias (JVM)

```bash
./gradlew testDebugUnitTest
```

| Archivo | Cubre |
|---|---|
| `biometric/FaceMatcherTest.kt` | Umbral predeterminado 0.60 (MobileFaceNet) / legado 0.72; vectores idénticos 192-d y 268-d; dimensiones mixtas se omiten |
| `security/PasswordHasherTest.kt` | Hash/verificación PBKDF2, contraseña incorrecta, sales distintas ⇒ hashes distintos, ida y vuelta hex |
| `security/GuardAuthManagerLockoutTest.kt` | `GuardAuthManager` real con almacén en memoria y reloj falso: bloqueo tras 5 fallos, rechazo durante 5 min incluso con contraseña correcta, desbloqueo, reinicio del contador, cambio de contraseña |
| `sync/SyncQueueDedupTest.kt` | `SyncQueue` real con DAO falso: clave duplicada no se encola dos veces, reintentos sin duplicar, reencolar tras eliminar |
| `analytics/DashboardCalculatorTest.kt` | KPI del tablero: entradas de hoy vs ayer, tasa de éxito, tiempo promedio, accesos por hora, 7 días, actividad reciente ordenada |
| `analytics/AlertsEngineTest.kt` | Alertas: 3+ denegados seguidos (por matrícula y rostros desconocidos), un éxito rompe la racha, acceso fuera de horario, orden |
| `validation/RegistrationValidatorTest.kt` | Normalización de matrícula y reglas de matrícula/nombre del asistente de registro |
| `validation/PasswordStrengthTest.kt` | Medidor de fortaleza (orientativo; la regla obligatoria de 6 caracteres sigue en `GuardAuthManager`) |
| `ui/InitialsTest.kt` | Iniciales para avatares sin foto |
| `ui/AdminFiltersTest.kt` | Búsqueda sin acentos, filtros por estatus, filtro de bitácora, agrupación «Hoy/Ayer», límites de periodo (`LogRange`), explicación del umbral |
| `flow/GuardFlowTest.kt` | Recorrido real con Hilt + `MainActivity` (Robolectric, Room en memoria **sin** consultas en el hilo principal): rol → configuración inicial / inicio de sesión → panel vacío y con datos → todas las secciones → kiosco; también en tableta (cajón) |
| `data/AccessEventDaoFilterTest.kt` | Regresión: filtros opcionales en `null` de la bitácora no lanzan `NullPointerException` |
| `security/SecurePrefsRecoveryTest.kt` | Qué errores del almacén cifrado provocan recrearlo (keyset dañado) y cuáles se propagan |
| `screenshots/ScreenshotTests.kt` | 17 pantallas renderizadas con Robolectric (prueba de humo de la UI); con `recordRoborazziDebug` además escribe los PNG (ver §4) |

Informe: `app/build/reports/tests/testDebugUnitTest/index.html`.

## 2. Pruebas manuales (dispositivo físico)

Prepara: dispositivo con cámara frontal, buena luz, 2–3 voluntarios. Anota los resultados en **Panel → Modo evaluación**.

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

### 2.2 Registro del alumno
| # | Pasos | Esperado |
|---|---|---|
| R0 | Datos: matrícula con guion / nombre con números | Mensaje de error bajo el campo; no avanza |
| R1 | Paso Consentimiento sin marcar la casilla | «Acepto y continuar» deshabilitado |
| R2 | Marcar → Acepto y continuar | Se pide el permiso de cámara (primera vez) y aparece el óvalo guía |
| R3 | Sin rostro / dos personas / muy lejos / muy cerca / de lado / oscuro / movido | Guía: «No se detectó un rostro», «Solo una persona a la vez», «Acércate un poco», «Aléjate un poco», «Mira de frente a la cámara», «Mejora la iluminación», «Mantén el dispositivo estable» |
| R4 | Mirar de frente con buena luz | Se capturan muestras (≈1 por segundo) hasta 5; «Guardar» se habilita con 3 |
| R5 | Guardar | Paso «Listo»: «¡Registro enviado!» · *Pendiente de aprobación* |
| R8 | Registrarse de nuevo con una matrícula BAJA | El estatus sigue en BAJA (no vuelve a PENDIENTE) |
| R6 | Verificar que no hay fotos: `adb shell run-as edu.utcj.acceso.debug ls -R files cache` | Ningún .jpg/.png del rostro |
| R7 | Inspeccionar BD (`App Inspection`) tabla `face_embeddings` | Solo BLOBs cifrados + IV |

### 2.3 Aprobación y kiosco
| # | Pasos | Esperado |
|---|---|---|
| K1 | Kiosco con alumno PENDIENTE | ACCESO DENEGADO; bitácora con motivo «Pendiente de aprobación» |
| K2 | Panel → Aprobaciones → Aprobar → Kiosco | ACCESO PERMITIDO en verde con nombre y matrícula |
| K3 | Esperar en resultado | Barra de cuenta regresiva; vuelve solo a la cámara a los **10 s** (ajustable en Configuración → Modo kiosco: 5/10/15/30 s). Tocar la pantalla regresa antes |
| K4 | Persona no registrada | ACCESO DENEGADO en rojo, «No reconocido» |
| K5 | Pantalla del kiosco | Inmersiva (sin barras), no se apaga, **horizontal** por defecto (Configuración → Orientación: Horizontal / Vertical / Automática) |
| K10 | Resultado denegado → «Guardia» | Mensaje «Se avisó al personal de seguridad»; aparece en Panel → Incidencias |
| K6 | Configuración → umbral 0.90 | Más rechazos de personas legítimas (FRR ↑) |
| K7 | Umbral 0.60 | Más aceptaciones; vigilar falsos positivos (FAR ↑) |
| K8 | Activar liveness → kiosco | Pide parpadear antes de verificar |
| K9 | Horario fin = hora actual | Alumno aprobado ⇒ DENEGADO «Fuera de horario» en bitácora |

### 2.4 Respaldos
| # | Pasos | Esperado |
|---|---|---|
| F1 | Kiosco → «Huella» (con huella enrolada) | Diálogo del sistema; si es válida, PERMITIDO, método FINGERPRINT |
| F2 | Dispositivo sin huella | «Huella no disponible en este dispositivo» |
| Q1 | Soy alumno → Mi acceso → «Mostrar mi QR» (matrícula aprobada) | QR con anillo de cuenta regresiva de 30 s que se renueva solo |
| Q2 | Kiosco → «Código QR» → mostrar QR | PERMITIDO, resultado QR |
| Q3 | Captura de pantalla del QR, usar a los 31+ s | «QR expirado» |
| Q4 | Matrícula PENDIENTE / BAJA en Mi acceso | Estatus explicado; «Mostrar mi QR» deshabilitado |

### 2.5 Estatus institucional
| # | Pasos | Esperado |
|---|---|---|
| S1 | Registrar y aprobar con matrícula `20210003` (BAJA en el CSV) → Configuración → «Cargar CSV de assets» | Estatus BAJA; kiosco ⇒ DENEGADO «Estatus BAJA» |
| S2 | Importar un CSV propio con el selector de documentos | Estatus actualizados |

### 2.6 Bitácora, panel y sincronización
| # | Pasos | Esperado |
|---|---|---|
| L1 | Bitácora → buscar nombre/matrícula, periodo (Hoy/7/30/Personalizado) y resultado | Lista filtrada y agrupada por día |
| L2 | Ícono de descarga → CSV y PDF | Hoja para compartir; CSV abre con acentos en Excel; PDF con encabezado de marca, resumen y tabla paginada |
| L3 | Buscar un botón de editar/borrar | No existe (solo lectura) |
| P1 | Entrada manual sin motivo | Botón deshabilitado; con motivo ⇒ evento MANUAL con guardia |
| P2 | Visitantes → «Registrar visita» → «Salida» | Pasa de «Dentro» a «Historial» |
| P3 | Incidencias → «Reportar» con categoría | Aparece en la lista |
| P4 | Inicio | KPI, barras por hora, dona permitidos/denegados, tendencia de 7 días, actividad reciente |
| P5 | 3 intentos fallidos seguidos de la misma matrícula (p. ej. alumno BAJA) | Alerta «3 fallos seguidos» en Inicio |
| P6 | Teléfono / tableta vertical / tableta horizontal | Barra inferior / riel / cajón permanente |
| P7 | Configuración → Apariencia → Oscuro | Toda la app cambia a tema oscuro |
| P8 | Configuración → tocar 7 veces «Versión» | Abre el modo evaluación |
| O1 | Modo avión → verificar varias veces | Indicador «Sin conexión»; acceso sigue funcionando |
| O2 | Desactivar modo avión | «Pendientes: N» → baja a 0 / «En línea»; sin eventos duplicados |

### 2.7 Privacidad
| # | Pasos | Esperado |
|---|---|---|
| D1 | Consentimiento → «Eliminar mis datos» → matrícula | Alumno y embeddings eliminados; kiosco ya no lo reconoce |

## 3. Modo evaluación (oculto) y métricas

**Panel → Configuración → tocar 7 veces «Versión»**. Por cada escenario anota: nombre, ¿pasó?, similitud (visible en la bitácora / exportación CSV) y notas.

Escenarios sugeridos (10 intentos cada uno, por voluntario):

| Escenario | Notas |
|---|---|
| Base (frente, buena luz) | Referencia |
| Lentes graduados | |
| Lentes de sol | Se espera fallo con el motor escolar |
| Cubrebocas | Se espera fallo con el motor escolar |
| Poca luz | |
| Contraluz | |
| Ángulo ±15° | |
| Gorra | |
| Impostor (persona no registrada) | Debe fallar |
| Foto impresa / pantalla con liveness activado | Debe fallar |

Métricas:

- **TAR** (tasa de aceptación verdadera) = aceptaciones legítimas / intentos legítimos
- **FRR** = 1 − TAR
- **FAR** = aceptaciones de impostores / intentos de impostores
- **Tiempo medio de verificación** (Tablero o columna `duracion_ms` del CSV)

Para calibrar el umbral: exporta el CSV, grafica la columna `similitud` de intentos legítimos e impostores y elige el valor que separe ambas distribuciones. Punto de partida: **0.60** (MobileFaceNet TFLite) o **0.72** (motor legado histograma).

## 4. Capturas de pantalla (Roborazzi)

Las capturas de `docs/screenshots/` se generan en la JVM con **Roborazzi 1.10 + Robolectric 4.11** (gráficos nativos), sin emulador ni dispositivo:

```bash
./gradlew recordRoborazziDebug                       # todas (escribe docs/screenshots/*.png)
./gradlew recordRoborazziDebug --tests '*ScreenshotTests*'
```

- Pruebas: `app/src/test/java/edu/utcj/acceso/screenshots/ScreenshotTests.kt` con datos ficticios deterministas (`FakeData`).
- Usan los composables **sin estado** (`KioskContent`, `DashboardContent`, `AdminShellContent`…), por eso no requieren Hilt ni cámara (la cámara se sustituye por `CameraPlaceholder`).
- `app/src/test/resources/robolectric.properties` fija `sdk=34` y una `Application` simple (sin Hilt ni Keystore).
- Dispositivos: teléfono `w393dp-h852dp-xxhdpi`, tableta/kiosco `w1280dp-h800dp-land-xhdpi`.
- Se renderiza con *movimiento reducido* y reloj de pruebas manual para que las animaciones infinitas no bloqueen la captura.
- `./gradlew testDebugUnitTest` también ejecuta estas pruebas (verifica que todas las pantallas se rendericen sin errores) pero **no** sobrescribe los PNG.
- Para agregar una captura: nueva función `@Test fun sNN_nombre() = shot("NN-nombre") { …Content(…) }`; usa `@Config(qualifiers = TABLET)` para tableta.
