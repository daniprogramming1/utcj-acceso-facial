# DECISIONES

Una línea por decisión: qué se decidió y por qué.

| Fecha | Decisión | Por qué |
|---|---|---|
| 2026-11-18 | Evolucionar Acceso UTCJ en vez de reescribirla | El núcleo de QR firmado, offline y seguridad ya está probado |
| 2026-11-18 | Seguir en Android nativo (Compose) | Cámara, kiosco y funcionamiento sin red son el corazón del producto |
| 2026-11-18 | Idea firma: «Pase vivo» | Es memorable y además frena el uso de capturas del QR |
| 2026-11-18 | Dirección visual «Atardecer de frontera» | Cálida y local sin perder confianza; la «Señalética» se sentía fría y genérica |
| 2026-11-18 | Bricolage Grotesque + Atkinson Hyperlegible reemplazan a Plus Jakarta Sans | Display con carácter y lectura pensada para leerse al sol |
| 2026-11-18 | No reorganizar carpetas por feature | Movería más de 100 archivos sin valor para el usuario; lo nuevo va en `ui/<feature>` |
| 2026-11-18 | La vigencia del QR la decide seguridad, no el alumno | Si el alumno puede elegir, siempre elegirá la máxima |
| 2026-11-18 | El kiosco no muestra el motivo del rechazo | Privacidad frente a la fila; el motivo se ve en el teléfono del guardia |
| 2026-11-18 | Robolectric 4.11.1 → 4.12.2 (solo pruebas) | 4.11 no trae el runtime de gráficos para Windows: las capturas y `GuardFlowTest` no podían correr en esta máquina |
| 2026-11-18 | Bricolage como fuente variable con `@OptIn(ExperimentalTextApi)` | Un solo TTF (408 KB) cubre todos los pesos y tamaños ópticos; la API es estable desde Compose 1.7 |
| 2026-11-18 | La selección del modo del pase usa terracota (primaryContainer) | El color protagonista debe marcar el foco; el teal queda como acento institucional |
| 2026-11-18 | `GuardFlowTest` calcula el horario con la zona de Juárez | Usaba la zona por defecto de la JVM y podía caer «Fuera de horario» según dónde corriera |
| 2026-11-19 | Permitido se registra al cerrar una ventana de 5 s, no antes | La bitácora es solo de alta: «deshacer» no puede borrar un evento, así que se escribe cuando ya no se puede objetar |
| 2026-11-19 | El siguiente QR confirma la entrada en espera y la cámara sigue activa | En hora pico el guardia no debería tocar nada entre alumno y alumno |
| 2026-11-19 | Todos los permitidos usan la ventana; no hay categoría de «dudosos» | `AccessDecider` solo devuelve permitido/denegado; inventar una zona gris sería adivinar. Se puede agregar si seguridad la pide |
| 2026-11-19 | El kiosco muestra motivos del código (vencido, ya usado…) pero oculta los personales (baja, pendiente…) | Los primeros los resuelve el alumno solo; los segundos son privados frente a la fila |
| 2026-11-19 | Nombre corto = primer nombre + inicial de la siguiente palabra | En nombres de 3 palabras no se sabe dónde empieza el apellido; cualquier inicial protege igual |
| 2026-11-19 | Vibración con `performHapticFeedback` (CONFIRM/REJECT), sin permiso VIBRATE | Patrones distintos sin pedir permisos nuevos |
| 2026-11-19 | Un solo ajuste «Sonido de confirmación» para panel y kiosco | Misma decisión de la caseta; se conserva la clave `kiosk_sound` para no perder la preferencia |
| 2026-11-19 | Bitácora: filtros en un panel inferior; el motivo en su propia línea; rechazos seguidos (mismo alumno y motivo) en una sola fila | Las dos filas de chips se salían de la pantalla y el motivo, lo más importante de un rechazo, se cortaba |
| 2026-11-19 | KPI «Visitantes dentro» en lugar de «Tiempo promedio»; el promedio pasa a Modo evaluación | La app no registra salidas de alumnos, así que «alumnos dentro» no se puede calcular con honestidad; los visitantes sí tienen salida |
| 2026-11-19 | Carrera de una lista (`BrandConfig.CARRERAS`) + grupo validado; «Otra carrera…» como respaldo | Datos consistentes con el CSV; la lista sale de los datos del proyecto y la debe confirmar Servicios Escolares |
| 2026-11-19 | La matrícula sigue aceptando letras; el teclado numérico depende de `BrandConfig.MATRICULA_NUMERIC` | La app es white-label y otras escuelas usan letras; la UTCJ usa 8 dígitos |
| 2026-11-19 | Validación en vivo, salvo «demasiado corta» (solo al continuar) | Avisar a tiempo sin regañar a quien sigue escribiendo |
| 2026-11-19 | «QR de registro» se oculta si el pase está activo o si el alumno tocó «Ya me aprobaron»; queda un enlace | El teléfono del alumno nunca se entera de la aprobación del guardia: la marca local es la única señal |
| 2026-11-19 | Los textos se quedan en el código (no `strings.xml`) | Decisión del usuario: 521 textos en una app solo en español; alto riesgo y poco valor hoy |
| 2026-11-19 | El contraste se verifica con una prueba (`ContrastTest`), no a ojo | Encontró un fallo real (franja del tema oscuro 2.28:1) que las capturas no hacían evidente |
| 2026-11-19 | KPI con ícono arriba y etiqueta debajo | Con texto grande, ícono + etiqueta en la misma fila partía las palabras |
| 2026-11-19 | El logotipo (teal/azul) no se cambia; solo el fondo del ícono del lanzador | Es la marca institucional de la UTCJ; recolorearla no le toca a la app |
| 2026-11-19 | Lema «Tu pase de entrada, sin filas ni papeles.» | El anterior («inteligente, rápido y privado») era relleno genérico que CLAUDE.md §3 prohíbe |
| 2026-11-19 | Figma: se actualizaron variables y estilos, no la distribución de cada pantalla | Cambia toda la piel en dos llamadas; rehacer cada layout es un trabajo aparte |
