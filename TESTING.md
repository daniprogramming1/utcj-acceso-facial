# Guía de pruebas — Acceso UTCJ

## 1. Pruebas unitarias (JVM)

```bash
./gradlew testDebugUnitTest
```

| Archivo | Cubre |
|---|---|
| `biometric/FaceMatcherTest.kt` | Umbral predeterminado 0.72, vectores idénticos coinciden, ortogonales no, se elige la mejor muestra |
| `security/PasswordHasherTest.kt` | Hash/verificación PBKDF2, contraseña incorrecta, sales distintas ⇒ hashes distintos, ida y vuelta hex |
| `security/GuardAuthManagerLockoutTest.kt` | `GuardAuthManager` real con almacén en memoria y reloj falso: bloqueo tras 5 fallos, rechazo durante 5 min incluso con contraseña correcta, desbloqueo, reinicio del contador, cambio de contraseña |
| `sync/SyncQueueDedupTest.kt` | `SyncQueue` real con DAO falso: clave duplicada no se encola dos veces, reintentos sin duplicar, reencolar tras eliminar |

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
| G9 | En kiosco, botón Atrás o «Salir» | Pide contraseña; sin ella no se sale |

### 2.2 Registro del alumno
| # | Pasos | Esperado |
|---|---|---|
| R1 | Soy alumno → sin marcar casilla | «Aceptar» deshabilitado |
| R2 | Marcar → Aceptar | Pantalla de registro |
| R3 | Sin rostro / dos personas / muy lejos / muy cerca / de lado / oscuro / movido | Guía: «No se detectó un rostro», «Solo una persona a la vez», «Acércate un poco», «Aléjate un poco», «Mira de frente a la cámara», «Mejora la iluminación», «Mantén el dispositivo estable» |
| R4 | Mirar de frente con buena luz | Se capturan muestras (≈1 por segundo) hasta 5; «Guardar» se habilita con 3 |
| R5 | Guardar | «Registro enviado – PENDIENTE» |
| R6 | Verificar que no hay fotos: `adb shell run-as edu.utcj.acceso.debug ls -R files cache` | Ningún .jpg/.png del rostro |
| R7 | Inspeccionar BD (`App Inspection`) tabla `face_embeddings` | Solo BLOBs cifrados + IV |

### 2.3 Aprobación y kiosco
| # | Pasos | Esperado |
|---|---|---|
| K1 | Kiosco con alumno PENDIENTE | ACCESO DENEGADO; bitácora con motivo «Pendiente de aprobación» |
| K2 | Panel → Aprobaciones → Aprobar → Kiosco | ACCESO PERMITIDO en verde con nombre y matrícula |
| K3 | Esperar en resultado | Vuelve solo a la cámara a los 30 s |
| K4 | Persona no registrada | ACCESO DENEGADO en rojo, «No reconocido» |
| K5 | Pantalla del kiosco | Inmersiva (sin barras), no se apaga, vertical fija |
| K6 | Configuración → umbral 0.90 | Más rechazos de personas legítimas (FRR ↑) |
| K7 | Umbral 0.60 | Más aceptaciones; vigilar falsos positivos (FAR ↑) |
| K8 | Activar liveness → kiosco | Pide parpadear antes de verificar |
| K9 | Horario fin = hora actual | Alumno aprobado ⇒ DENEGADO «Fuera de horario» en bitácora |

### 2.4 Respaldos
| # | Pasos | Esperado |
|---|---|---|
| F1 | Kiosco → «Verificar con huella» (con huella enrolada) | Diálogo del sistema; si es válida, PERMITIDO, método FINGERPRINT |
| F2 | Dispositivo sin huella | «Huella no disponible en este dispositivo» |
| Q1 | Inicio → «mostrar mi QR dinámico» → matrícula aprobada | QR con contador de 30 s |
| Q2 | Kiosco → «Escanear QR dinámico» → mostrar QR | PERMITIDO, resultado QR |
| Q3 | Captura de pantalla del QR, usar a los 31+ s | «QR expirado» |
| Q4 | Matrícula PENDIENTE / BAJA en el generador | Mensaje de error, no genera QR |

### 2.5 Estatus institucional
| # | Pasos | Esperado |
|---|---|---|
| S1 | Registrar y aprobar con matrícula `20210003` (BAJA en el CSV) → Configuración → «Cargar CSV de assets» | Estatus BAJA; kiosco ⇒ DENEGADO «Estatus BAJA» |
| S2 | Importar un CSV propio con el selector de documentos | Estatus actualizados |

### 2.6 Bitácora, panel y sincronización
| # | Pasos | Esperado |
|---|---|---|
| L1 | Bitácora → filtrar matrícula / resultado | Lista filtrada |
| L2 | Exportar CSV y PDF | Hoja para compartir; archivos legibles con todas las columnas |
| L3 | Buscar un botón de editar/borrar | No existe (solo lectura) |
| P1 | Entrada manual sin motivo | Botón deshabilitado; con motivo ⇒ evento MANUAL con guardia |
| P2 | Visitante: registrar entrada → salida | Desaparece de «Dentro del campus» |
| P3 | Incidente con categoría y URI | Aparece en la lista |
| P4 | Tablero | Barras por hora, % éxito/fallo, tiempo medio |
| P5 | 3 intentos fallidos seguidos de la misma matrícula (p. ej. alumno BAJA) | Alerta «3+ fallos consecutivos» |
| O1 | Modo avión → verificar varias veces | Indicador «Sin conexión»; acceso sigue funcionando |
| O2 | Desactivar modo avión | «Pendientes: N» → baja a 0 / «En línea»; sin eventos duplicados |

### 2.7 Privacidad
| # | Pasos | Esperado |
|---|---|---|
| D1 | Consentimiento → «Eliminar mis datos» → matrícula | Alumno y embeddings eliminados; kiosco ya no lo reconoce |

## 3. Modo evaluación (oculto) y métricas

**Panel → Modo evaluación**. Por cada escenario anota: nombre, ¿pasó?, similitud (visible en la bitácora / exportación CSV) y notas.

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

Para calibrar el umbral: exporta el CSV, grafica la columna `similitud` de intentos legítimos e impostores y elige el valor que separe ambas distribuciones (con el motor escolar, 0.72 es un punto de partida).
