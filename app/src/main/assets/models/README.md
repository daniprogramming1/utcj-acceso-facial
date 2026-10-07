# Modelos TFLite

## mobilefacenet.tflite (requerido para el motor neuronal)

Este directorio **no** incluye el binario del modelo (licencia / tamaño).

### Cómo obtenerlo

Desde la raíz del repositorio:

```bash
./scripts/download_mobilefacenet.sh
```

Eso descarga un MobileFaceNet comunitario (112×112 RGB → embedding ~192-d) a:

`app/src/main/assets/models/mobilefacenet.tflite`

Origen por defecto: [MCarlomagno/FaceRecognitionAuth](https://github.com/MCarlomagno/FaceRecognitionAuth) (BSD-3-Clause).  
Arquitectura / entrenamiento de referencia: [sirius-ai/MobileFaceNet_TF](https://github.com/sirius-ai/MobileFaceNet_TF) (Apache-2.0).

También puedes colocar manualmente cualquier `.tflite` compatible con:

| Parámetro | Valor esperado |
|---|---|
| Entrada | `[1, 112, 112, 3]` float32 |
| Normalización | `(pixel - 127.5) / 128.0` → [-1, 1] |
| Salida | `[1, N]` float32 (N típico = 192) |

### Sin el modelo

La app arranca igual: usa el **motor legado** (histograma 268-d) y escribe un aviso en Logcat en español.

### Migración

Los embeddings del histograma y de MobileFaceNet **no son intercambiables**.  
Tras añadir o cambiar el modelo, **todos los alumnos deben volver a registrarse**.
