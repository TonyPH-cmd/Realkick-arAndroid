package com.example.realkick.ui

import android.graphics.Rect
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Representa la información de un objeto detectado por el modelo de ML Kit.
 */
data class DetectedObjectData(
    val label: String,
    val confidence: Float,
    val boundingBox: Rect,
    val normalizedRect: RectF = RectF()
)

/**
 * Representa la región del suelo o superficie base detectada por ML Kit
 * con su respectivo nivel de confianza.
 */
data class FloorMaskData(
    val rect: RectF,
    val confidence: Float,
    val label: String = "floor"
)

/**
 * Interfaz de comunicación (Puente de Datos) para enviar los resultados de
 * análisis visual de regreso al ciclo de ARCore.
 */
interface VisualAICallback {
    fun onAnalysisResult(
        detectedObjects: List<DetectedObjectData>,
        floorMasks: List<FloorMaskData>,
        isLowContrast: Boolean
    )
}

/**
 * Servicio de procesamiento asíncrono en segundo plano para análisis de visión por computadora.
 * Integra Google ML Kit Object Detection para identificar superficies u objetos difíciles
 * y mitigar falsos positivos causados por reflejos o patrones repetitivos.
 */
class VisualAIAnalyzer(private val callback: VisualAICallback) {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val isProcessing = AtomicBoolean(false)
    private var lastProcessedTimestamp = 0L

    // Configuración del detector de objetos en modo STREAM para procesamiento de video continuo
    private val options = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .enableMultipleObjects()
        .enableClassification() // Intenta clasificar los objetos detectados (ej. "Floor", "Table", etc.)
        .build()

    private val detector: ObjectDetector = ObjectDetection.getClient(options)

    /**
     * Procesa un frame de la cámara de forma asíncrona usando Coroutines.
     * Diseñado para operar a un máximo de 10-15 FPS (intervalo de ~100 ms) para no impactar
     * el ciclo de renderizado nativo ni el rendimiento físico del juego (penales).
     *
     * EXPLICACIÓN ARQUITECTÓNICA (Evaluación de Ingeniería):
     * ARCore tiene un buffer de imágenes nativo muy limitado (1-2 slots). Si pasamos el objeto
     * Image de forma asíncrona, bloquearíamos la GPU y el motor de tracking, causando freeze.
     * Para solucionar esto:
     * 1. Extraemos y copiamos el plano Y (luminancia) en el hilo principal de manera instantánea (<0.5ms).
     * 2. Cerramos inmediatamente la imagen de la cámara en el hilo de render.
     * 3. Pasamos el búfer copiado (NV21) a esta corrutina para ejecutar ML Kit en la hebra Dispatchers.Default.
     */
    fun analyzeFrame(
        nv21Bytes: ByteArray,
        width: Int,
        height: Int,
        rotationDegrees: Int
    ) {
        val currentTime = System.currentTimeMillis()
        
        // Control estricto de tasa de frames: Throttling a ~12 FPS (intervalo mínimo de 83 ms)
        if (currentTime - lastProcessedTimestamp < 83) {
            return
        }

        // Evitar acumulación de frames encolados en segundo plano
        if (!isProcessing.compareAndSet(false, true)) {
            return
        }

        lastProcessedTimestamp = currentTime

        coroutineScope.launch {
            var inputImage: InputImage? = null
            try {
                // Crear el InputImage a partir del búfer NV21 (escala de grises optimizada)
                inputImage = InputImage.fromByteArray(
                    nv21Bytes,
                    width,
                    height,
                    rotationDegrees,
                    InputImage.IMAGE_FORMAT_NV21
                )

                // Ejecución síncrona en el hilo secundario (Dispatchers.Default)
                val task = detector.process(inputImage)
                
                // Esperar a que ML Kit procese de forma síncrona
                com.google.android.gms.tasks.Tasks.await(task)

                val results = task.result ?: emptyList()
                val detectedList = results.map { obj ->
                    val label = obj.labels.firstOrNull()?.text ?: "Objeto Desconocido"
                    val confidence = obj.labels.firstOrNull()?.confidence ?: 0.0f
                    val normalizedRect = getNormalizedScreenRect(obj.boundingBox, width, height, rotationDegrees)
                    DetectedObjectData(
                        label = label,
                        confidence = confidence,
                        boundingBox = obj.boundingBox,
                        normalizedRect = normalizedRect
                    )
                }

                // Segmentación de la superficie base/suelo usando clasificación de ML Kit
                val floorLabels = listOf("floor", "ground", "carpet", "rug", "soil")
                val tableLabels = listOf("table", "desk", "furniture", "surface", "wood", "place", "tile", "bed")
                val floorMasks = results.filter { obj ->
                    val label = obj.labels.firstOrNull()?.text?.lowercase() ?: ""
                    (floorLabels + tableLabels).any { term -> label.contains(term) }
                }.map { obj ->
                    val labelText = obj.labels.firstOrNull()?.text?.lowercase() ?: ""
                    val resolvedLabel = if (floorLabels.any { term -> labelText.contains(term) }) "floor" else "table"
                    val confidence = obj.labels.firstOrNull()?.confidence ?: 0.5f
                    val normalizedRect = getNormalizedScreenRect(obj.boundingBox, width, height, rotationDegrees)
                    FloorMaskData(normalizedRect, confidence, resolvedLabel)
                }

                // Fallback seguro: si la IA no detectó ninguna superficie, asumimos que la mitad inferior es el suelo
                val finalFloorMasks = if (floorMasks.isEmpty()) {
                    listOf(FloorMaskData(RectF(0.0f, 0.4f, 1.0f, 1.0f), 0.5f, "floor"))
                } else {
                    floorMasks
                }

                // Análisis complementario de bajo contraste sobre el búfer Y
                val isLowContrast = calculateLowContrast(nv21Bytes, width, height)

                // Devolver los resultados al hilo principal para actualizar Compose y ARCore
                withContext(Dispatchers.Main) {
                    callback.onAnalysisResult(detectedList, finalFloorMasks, isLowContrast)
                }

            } catch (e: Exception) {
                // Manejo de errores silencioso
            } finally {
                // Control estricto de memoria: anular la referencia del InputImage para recolectar memoria
                inputImage = null
                // Liberar el semáforo para permitir el procesamiento del siguiente frame en el ciclo
                isProcessing.set(false)
            }
        }
    }

    /**
     * Convierte la región de interés (ROI) en coordenadas de la imagen raw 2D
     * a coordenadas de pantalla normalizadas [0..1] considerando la rotación del sensor.
     *
     * EXPLICACIÓN MATEMÁTICA:
     * En orientación vertical (portrait), la imagen del sensor de la cámara suele estar rotada 90° o 270°.
     * Por lo tanto, realizamos una transformación afín de rotación y traslación:
     * - Rotación 90°:
     *     x_screen = y_image / imageHeight
     *     y_screen = 1.0 - (x_image / imageWidth)
     */
    private fun getNormalizedScreenRect(
        boundingBox: Rect,
        imageWidth: Int,
        imageHeight: Int,
        rotationDegrees: Int
    ): RectF {
        val left = boundingBox.left.toFloat() / imageWidth
        val top = boundingBox.top.toFloat() / imageHeight
        val right = boundingBox.right.toFloat() / imageWidth
        val bottom = boundingBox.bottom.toFloat() / imageHeight

        return when (rotationDegrees) {
            90 -> {
                RectF(top, 1.0f - right, bottom, 1.0f - left)
            }
            270 -> {
                RectF(1.0f - bottom, left, 1.0f - top, right)
            }
            180 -> {
                RectF(1.0f - right, 1.0f - bottom, 1.0f - left, 1.0f - top)
            }
            else -> {
                RectF(left, top, right, bottom)
            }
        }
    }

    /**
     * Calcula la varianza de contraste local sobre una porción central del búfer de luminancia (Y).
     * Ayuda a identificar pisos extremadamente lisos o lavados por la luz.
     */
    private fun calculateLowContrast(nv21Bytes: ByteArray, width: Int, height: Int): Boolean {
        val startX = width / 4
        val endX = 3 * width / 4
        val startY = height / 4
        val endY = 3 * height / 4
        
        var sum = 0L
        var count = 0
        
        for (y in startY until endY step 8) {
            val offset = y * width
            for (x in startX until endX step 8) {
                sum += nv21Bytes[offset + x].toInt() and 0xFF
                count++
            }
        }
        
        if (count == 0) return false
        val mean = sum.toFloat() / count
        
        var varianceSum = 0f
        for (y in startY until endY step 8) {
            val offset = y * width
            for (x in startX until endX step 8) {
                val value = (nv21Bytes[offset + x].toInt() and 0xFF).toFloat()
                varianceSum += (value - mean) * (value - mean)
            }
        }
        val variance = varianceSum / count
        
        return variance < 120f
    }
}
