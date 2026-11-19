# PLAN — Acceso UTCJ v2

Una fase a la vez. Al cerrar cada fase la app compila, corre y pasa sus pruebas.
Diseño en `DISEÑO.md` · decisiones en `DECISIONES.md`.

**Comando base** (la terminal usa Java 8; Gradle necesita JDK 17):
```bash
JAVA_HOME="$HOME/.jdks/jbr-17.0.14" ./gradlew :app:compileDebugKotlin :app:testDebugUnitTest
JAVA_HOME="$HOME/.jdks/jbr-17.0.14" ./gradlew :app:recordRoborazziDebug   # regenera capturas
```

---

## Fase 0 — Concepto y diseño ✅
- [x] `DISEÑO.md`: concepto, idea firma, 2 direcciones visuales, stack
- [x] `PLAN.md`, `DECISIONES.md`, `CLAUDE.md` §2 (identidad)
- [x] **OK del usuario para empezar la Fase 1**

## Fase 1 — Nueva piel + Pase vivo *(visible y funcional)* ✅
- [x] Tokens de la Dirección 1 en `ui/theme/*` y `BrandConfig.palette` (claro, oscuro y kiosco), sombras tintadas, radios por pieza y movimiento con nombre (`Motion.TAP/STATE/PERFORATE/LANE/PAGE`)
- [x] Fuentes Bricolage Grotesque + Atkinson Hyperlegible en `res/font`, con licencias OFL en `docs/licenses`; `Type.kt` reescrito
- [x] Componentes `PassCard` (muescas + perforación) y `HorizonBand` (sol y sierra según la hora de Juárez, ligado a la cuenta regresiva), con versión estática para `LocalReducedMotion`
- [x] `StudentHomeScreen` → **Pase vivo** (`DayPhase` + `horizonProgress` en `domain/time`, con pruebas)
- [x] Quitar los chips de vigencia de la pantalla del alumno; vigencia fija de 60 s y purga de la clave antigua
- [x] Verificar: compila, 105/105 pruebas en verde y capturas nuevas `04`, `04b` y `04c` (noche, oscuro); autocrítica: se corrigió que el sol tapaba la etiqueta y que la selección usaba el acento en vez de la terracota
- Pendiente para la Fase 5: el ícono del lanzador y el logo siguen con los colores institucionales anteriores

## Fase 2 — Caseta rápida ✅
- [x] Háptica y tono en el escáner del guardia (`rememberScanFeedback`, compartido con el kiosco; CONFIRM/REJECT + ACK/NACK)
- [x] Permitido se registra solo tras 5 s con «No permitir» en la tarjeta; el siguiente QR confirma el anterior y la cámara no se pausa. No hay categoría de «dudosos» (ver DECISIONES)
- [x] Kiosco: `kioskDisplay` → «¡Pásale, Ana S.!»; motivos del código visibles, motivos personales como «Ahorita no. Acércate con el guardia.»
- [x] Microinteracciones «carril verde» (`Modifier.laneSweep`) y perforación (`AnimatedPerforation`)
- [x] Verificar: 110/110 pruebas (nuevas: `KioskDisplayTest`, `shortName`), capturas 06, 07, 14 y 15 regeneradas; autocrítica: la guía del visor tapaba el recuadro con resultado → movida a la tarjeta

## Fase 3 — Bitácora, panel y registro ✅
- [x] Bitácora: botón «Filtros · periodo · resultado» con panel inferior; motivo en su propia línea; rechazos seguidos agrupados (`groupConsecutiveDenials`, «2 intentos · 13:15–13:16»); estados vacíos con voz propia
- [x] Panel: «Visitantes dentro» reemplaza «Tiempo promedio» («alumnos dentro» no se puede calcular: no hay registro de salida); el promedio se ve en Modo evaluación
- [x] Registro: carrera de lista (`BrandConfig.CARRERAS`, con «Otra carrera…») + grupo validado; teclado numérico y validación en vivo de la matrícula
- [x] Mi acceso: «QR de registro» oculto con el pase activo o tras «Ya me aprobaron»; enlace «¿Te pidieron tu QR de registro?»
- [x] Verificar: 118/118 pruebas (nuevas: `LogGroupingTest`, `CarreraFormatTest`); capturas 03b, 04, 09 y 11 regeneradas
- Pendiente: confirmar con Servicios Escolares la lista oficial de carreras

## Fase 4 — Accesibilidad y voz
- [ ] Todos los textos a `strings.xml`, reescritos con la voz de `DISEÑO.md`
- [ ] Auditar `contentDescription` (hoy hay 49 en `null`), foco visible y áreas táctiles ≥ 48 dp
- [ ] Capturas Roborazzi con `fontScale` 1.5; contraste AA de todos los pares de tokens

## Fase 5 — Resto de pantallas y cierre
- [ ] Onboarding, rol, login, consentimiento, eliminar datos, visitantes e incidencias con la nueva dirección
- [ ] Estados vacío, error y carga con voz propia en cada vista
- [ ] Actualizar el Figma «Acceso UTCJ — Layouts», `README.md`, `ARCHITECTURE.md` y `docs/screenshots`
- [ ] Autocrítica de `CLAUDE.md` §6 pantalla por pantalla
