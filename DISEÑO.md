# DISEÑO — Acceso UTCJ (evolución v2)

> Dirección de producto y diseño. Las reglas generales están en `CLAUDE.md`; las fases, en `PLAN.md`; las decisiones, en `DECISIONES.md`.

## 1. Concepto

Acceso UTCJ convierte el teléfono del alumno en su **pase de entrada**: un QR firmado que se renueva solo y no se puede copiar.
El guardia (teléfono) o el kiosco (tableta) lo valida en menos de un segundo, **aun sin internet**.
Es para alumnos de la UTCJ y para los guardias en turno.

### Para quién
| Persona | Contexto | Lo que necesita |
|---|---|---|
| **Alumno** (18–25 años) | Teléfono Android; de 6:30 a 8:00, en la fila de la caseta, con prisa y con una mano ocupada | Abrir la app y mostrar el pase en menos de 2 s, sin pensar |
| **Guardia** | Teléfono o tableta en la caseta, al sol y con ruido, atendiendo a la vez la fila y la puerta | Saber «pasa / no pasa» sin leer, con vibración y sonido |
| **Kiosco** | Tableta fija en horizontal, vista por toda la fila | Instrucción clara y resultado enorme, sin exponer datos privados |

### Identidad
- **Personalidad:** confiable, ágil, fronteriza.
- **Referencias:** pases de abordar de Wallet, señalética de cruces fronterizos (carril SENTRI / Ready Lane), el atardecer sobre la Sierra de Juárez, la arena de Samalayuca.
- **Lo que NO debe parecer:** un trámite de gobierno, una app bancaria genérica o una startup de degradados.
- **Toque personal:** la fila del puente internacional. Tu QR es tu «carril rápido».

### Voz
Directa, cálida y norteña sin caricatura. Frases cortas, tuteo, y nada de «Bienvenido a nuestra plataforma».

| Momento | Texto |
|---|---|
| Acceso permitido | «¡Pásale, Ana!» |
| Acceso denegado (kiosco) | «Ahorita no. Acércate con el guardia.» |
| QR del alumno | «Tu pase se renueva solo.» |
| Bitácora vacía | «Nadie ha cruzado hoy. La caseta está tranquila.» |
| Sin conexión | «Sin internet, sin problema: todo se guarda y se sube después.» |
| Error de cámara | «No veo nada. Revisa que la cámara tenga permiso.» |

## 2. Idea firma

| # | Idea | A favor | En contra |
|---|---|---|---|
| A | **Pase vivo.** El QR va en una tarjeta tipo pase de abordar con una *banda de horizonte* animada: un sol que cruza la sierra y avanza con la cuenta regresiva. Su color sigue la hora real de Juárez (amanecer, mediodía, noche). | El guardia distingue al instante un pase vivo de una captura de pantalla. Es memorable y 100 % local. | La animación continua exige cuidar la batería y respetar el movimiento reducido. |
| B | **Carril verde.** Al validar, una luz recorre la pantalla de abajo arriba como el semáforo de un cruce, con un patrón háptico propio (doble toque), igual en el guardia y en el kiosco. | Es feedback que se siente sin mirar. | Es una microinteracción; por sí sola no se recuerda. |
| C | **Pasaporte de sellos.** Cada entrada estampa un sello en el teléfono del alumno. | Muy compartible. | Inviable sin conexión: la entrada la registra el teléfono del guardia, y el del alumno nunca se entera. |

**Elegida: A (Pase vivo)**, con B como su microinteracción de éxito. Resuelve un riesgo real (compartir capturas del QR), se reconoce aunque se quite el logo y es puro Juárez. C se descarta por la arquitectura offline.

### Cómo funciona el Pase vivo
- **Tarjeta:** muescas laterales y una línea perforada que separa la cabecera (nombre, matrícula, carrera) del QR.
- **Banda de horizonte:** una franja de 56 dp bajo el QR con la silueta de la sierra. El sol avanza de izquierda a derecha durante la vigencia del QR y vuelve al inicio cuando el QR se renueva.
- **Color según la hora** (zona `America/Ciudad_Juarez`): 5:00–8:00 amanecer (rosa y naranja), 8:00–17:00 día (arena y azul cielo), 17:00–20:00 atardecer (terracota y morado), 20:00–5:00 noche (azul marino con estrellas).
- **Con movimiento reducido:** el sol no se anima. Solo cambia de posición cada segundo y lo acompaña el número de segundos.
- **Ante el guardia:** si el sol no se mueve y la hora no coincide, el pase es sospechoso.

## 3. Direcciones visuales

### Dirección 1 — «Atardecer de frontera» ✅ recomendada
| Token | Valor | Uso |
|---|---|---|
| Protagonista · Terracota Juárez | `#C2410C` | Acción principal, foco, pase (contraste 5.2:1 sobre blanco) |
| Acento · Teal UTCJ | `#00796B` | Guiño institucional, enlaces, estados informativos |
| Tinta · Noche del desierto | `#1B1F3B` | Texto principal, kiosco |
| Neutros arena | `#FBF7F2` `#F3EBE1` `#E2D5C5` `#A8957F` `#6B5A48` `#2E241B` | Fondos, superficies, bordes, texto secundario |
| Éxito | `#15803D` | Permitido |
| Peligro | `#B91C1C` | Denegado |
| Aviso | `#B45309` | Pendiente |
| Info | `#1D4ED8` | Manual / información |
| Kiosco | fondo `#141A2E`, texto `#F3EBE1` | Alto contraste para la fila |

- **Fuentes:** display **Bricolage Grotesque** (con carácter, licencia OFL) para títulos y números grandes; lectura **Atkinson Hyperlegible** (diseñada para la máxima legibilidad, ideal al sol).
- **Composición:** tarjetas tipo pase con muesca y línea perforada; números grandes y tabulares (hora, conteos); títulos alineados a la izquierda que rebasan la tarjeta (asimetría); sombras tintadas en terracota, nunca grises; radios distintos según la pieza (pase 28, botón 14, chip 10).
- **Microinteracción:** al validar, el pase se «perfora» (un troquel que avanza en 240 ms), se enciende el carril verde y vibra con un doble toque.

### Dirección 2 — «Señalética de cruce»
| Token | Valor |
|---|---|
| Protagonista · Amarillo señal | `#FFC72C` sobre casi negro `#111315` |
| Acento · Verde carril | `#00A86B` |
| Neutros oliva | `#F4F4EF` `#D9D9CF` `#7C7C6E` `#2B2B26` |
| Semánticos | Los mismos de la Dirección 1 |

- **Fuentes:** **Archivo** (Expanded Black, estilo señal vial) + **IBM Plex Sans**.
- **Composición:** retícula estricta, bloques sólidos, flechas grandes, sin sombras y con contraste máximo.
- **Microinteracción:** una barra vertical de semáforo que pasa de ámbar a verde.

### Recomendación
**Dirección 1.** Es cálida y local sin dejar de ser confiable. La 2 es muy legible, pero es fría y se parece a cualquier app de tránsito. De la 2 conservamos el contraste extremo para el kiosco.

### Tokens de movimiento
| Nombre | Duración | Curva | Uso |
|---|---|---|---|
| `Motion.tap` | 120 ms | standard | Presionar botones y chips |
| `Motion.state` | 200 ms | emphasized | Cambios de estado y aparición de tarjetas |
| `Motion.perforate` | 240 ms | emphasized-decelerate | Perforación del pase al validar |
| `Motion.lane` | 300 ms | emphasized | Barrido del carril verde |
| `Motion.page` | 320 ms | emphasized | Transiciones entre pantallas |

Todas pasan a *fade* o a un estado estático con `LocalReducedMotion`.

## 4. Stack y arquitectura

Se conserva lo que ya funciona. Lo único nuevo son 2 fuentes TTF.

| Pieza | Elección | Por qué |
|---|---|---|
| Lenguaje y UI | Kotlin + Jetpack Compose (Material 3) | Ya existe; cámara, kiosco y offline nativos |
| Inyección | Hilt (`di/*`) | Ya está cableada; ViewModels y Workers inyectables |
| Datos locales | Room v2 (`data/local`) | Fuente de verdad offline, con migraciones probadas |
| Lectura de QR | CameraX + ML Kit Barcode | Rápida y 100 % en el dispositivo |
| Generación de QR | ZXing | Ligera; genera el QR en el teléfono del alumno |
| Firma | Android Keystore + ECDSA P-256 (`domain/qr`, `data/qr`) | La llave privada nunca sale del teléfono |
| Sincronización | WorkManager + `SyncQueue` (`data/sync`) | Idempotente, con reintentos y sin duplicados |
| Nube (opcional) | Firebase Firestore | Se activa sola con `app/google-services.json` |
| Servidor del campus | Módulo `:server` (Ktor + PostgreSQL) | En progreso; fuera del alcance de este plan |
| Feedback háptico y sonoro | `VibrationEffect` / `ToneGenerator` del SDK | Sin librerías nuevas |
| Pruebas visuales | Roborazzi + Robolectric | Capturas como regresión visual en la JVM |

### Carpetas
```
app/src/main/java/edu/utcj/acceso/
  brand/        BrandConfig: nombre, paleta, zona horaria
  ui/theme/     ÚNICO lugar de tokens: Color, Type, Shape, Spacing, Motion
  ui/components Piezas reutilizables (PassCard, HorizonBand, LaneSweep… nuevas aquí)
  ui/<feature>/ student, guard, kiosk, admin, onboarding, role, scan, splash
  domain/       Lógica pura probada en JVM (qr, analytics, validation)
  data/         local (Room), qr, security, remote, sync, repository
  di/           Módulos Hilt
```
No se reorganiza todo por feature: movería más de 100 archivos sin ningún valor para el usuario. El código nuevo va en `ui/<feature>` o `ui/components`.

### Pantallas y flujo (sin cambios de navegación)
Splash → Onboarding → Rol →
- **Alumno:** Registro (datos → aviso → QR de registro) → **Pase vivo**
- **Guardia:** Login → Panel (Inicio · Escanear · Aprobar · Bitácora · Más) → Kiosco
