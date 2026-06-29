package com.example.realkick.ui

import android.graphics.RectF
import android.opengl.Matrix
import io.github.sceneview.math.Position
import com.google.ar.core.Pose
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.tanh

// ═══════════════════════════════════════════════════════════════
// CLASES DE TRANSPORTE Y PROCESAMIENTO MULTIHILO SEGURO
// Compartidas entre ARGameScreen y HistoryARScreen
// ═══════════════════════════════════════════════════════════════

internal data class CopiedPlaneData(
    val id: String,
    val translation: FloatArray,
    val rotation: FloatArray,
    val extentX: Float,
    val extentZ: Float,
    val polygon: FloatArray
)

internal data class PlaneHistoryEntry(
    val heights: List<Float>,
    val normals: List<FloatArray>
)

internal data class SampledDepthPoint(
    val u: Int,
    val v: Int,
    val depthMm: Int
)

internal data class DepthParams(
    val fx: Float,
    val fy: Float,
    val cx: Float,
    val cy: Float
)

internal data class CopiedFrameData(
    val activePlanes: List<CopiedPlaneData>,
    val rawPointCloudPoints: FloatArray?,
    val sampledDepthPoints: List<SampledDepthPoint>?,
    val depthParams: DepthParams?,
    val cameraPoseTranslation: FloatArray,
    val cameraPoseRotation: FloatArray,
    val isDepthSupported: Boolean,
    val isLowContrast: Boolean,
    val pixelIntensity: Float,
    val dynamicLimit: Int,
    val viewMatrix: FloatArray,
    val projectionMatrix: FloatArray,
    val floorMasks: List<FloorMaskData>,
    val detectedObjects: List<DetectedObjectData>,
    val depthBuffer: ShortArray?,
    val depthWidth: Int,
    val depthHeight: Int
)

/**
 * Resultado genérico del cálculo de estabilidad en segundo plano.
 * `finalDots` usa Position con estabilidad (isStable).
 */
internal data class StabilityResultPositions(
    val planeStabilities: Map<String, Float>,
    val planeInclinations: Map<String, Float>,
    val maxStability: Float,
    val stabilityLevel: String,
    val finalDotPositions: List<Position>,
    val finalDotStable: List<Boolean>
)

/**
 * FILTRADO ESPACIAL Y DE RUIDO (SUAVIZADO):
 * Algoritmo basado en voxel grid de O(N) para filtrar ruido en la nube de puntos.
 * Agrupa los puntos en celdas espaciales bidimensionales y descarta aquellos
 * que no tengan suficientes vecinos en su vecindario o cuya altura difiera significativamente
 * de la media local. Filtra destellos de luz e imperfecciones en pisos de azulejo.
 */
internal fun filterPointCloudSpatial(
    rawPoints: FloatArray?,
    voxelSize: Float = 0.15f,
    minNeighbors: Int = 3,
    maxHeightDev: Float = 0.04f
): List<FloatArray> {
    if (rawPoints == null) return emptyList()
    val count = rawPoints.size / 4
    if (count == 0) return emptyList()

    val grid = mutableMapOf<Pair<Int, Int>, MutableList<FloatArray>>()
    for (i in 0 until count) {
        val px = rawPoints[4 * i]
        val py = rawPoints[4 * i + 1]
        val pz = rawPoints[4 * i + 2]
        val pc = rawPoints[4 * i + 3]
        if (pc > 0.1f) {
            val cellX = Math.round(px / voxelSize).toInt()
            val cellZ = Math.round(pz / voxelSize).toInt()
            grid.getOrPut(Pair(cellX, cellZ)) { mutableListOf() }.add(floatArrayOf(px, py, pz, pc))
        }
    }

    val filtered = mutableListOf<FloatArray>()
    for (i in 0 until count) {
        val px = rawPoints[4 * i]
        val py = rawPoints[4 * i + 1]
        val pz = rawPoints[4 * i + 2]
        val pc = rawPoints[4 * i + 3]
        if (pc <= 0.1f) continue

        val cellX = Math.round(px / voxelSize).toInt()
        val cellZ = Math.round(pz / voxelSize).toInt()

        val neighbors = mutableListOf<FloatArray>()
        for (dx in -1..1) {
            for (dz in -1..1) {
                val list = grid[Pair(cellX + dx, cellZ + dz)]
                if (list != null) {
                    for (n in list) {
                        val dx2 = n[0] - px
                        val dz2 = n[2] - pz
                        if (dx2 * dx2 + dz2 * dz2 <= voxelSize * voxelSize) {
                            neighbors.add(n)
                        }
                    }
                }
            }
        }

        if (neighbors.size < minNeighbors) continue

        val meanY = neighbors.map { it[1] }.average().toFloat()
        if (abs(py - meanY) <= maxHeightDev) {
            filtered.add(floatArrayOf(px, py, pz, pc))
        }
    }
    return filtered
}

internal fun isPointInPolygon(x: Float, z: Float, polygon: FloatArray): Boolean {
    var intersectCount = 0
    val n = polygon.size / 2
    if (n < 3) return false
    for (i in 0 until n) {
        val x1 = polygon[2 * i]
        val z1 = polygon[2 * i + 1]
        val x2 = polygon[2 * ((i + 1) % n)]
        val z2 = polygon[2 * ((i + 1) % n) + 1]
        
        if (((z1 > z) != (z2 > z)) &&
            (x < (x2 - x1) * (z - z1) / (z2 - z1 + 1e-6f) + x1)) {
            intersectCount++
        }
    }
    return (intersectCount % 2) != 0
}

internal fun rotateVectorByQuaternion(vector: FloatArray, q: FloatArray): FloatArray {
    val vx = vector[0]
    val vy = vector[1]
    val vz = vector[2]
    val qx = q[0]
    val qy = q[1]
    val qz = q[2]
    val qw = q[3]
    
    val ix = qw * vx + qy * vz - qz * vy
    val iy = qw * vy + qz * vx - qx * vz
    val iz = qw * vz + qx * vy - qy * vx
    val iw = -qx * vx - qy * vy - qz * vz
    
    val rx = ix * qw + iw * -qx + iy * -qz - iz * -qy
    val ry = iy * qw + iw * -qy + iz * -qx - ix * -qz
    val rz = iz * qw + iw * -qz + ix * -qy - iy * -qx
    
    return floatArrayOf(rx, ry, rz)
}

internal fun transformPoint(point: FloatArray, translation: FloatArray, rotation: FloatArray): FloatArray {
    val rotated = rotateVectorByQuaternion(point, rotation)
    return floatArrayOf(
        rotated[0] + translation[0],
        rotated[1] + translation[1],
        rotated[2] + translation[2]
    )
}

/**
 * Proyecta un punto en el espacio 3D (coordenadas del mundo) al plano de la pantalla 2D [0..1]
 * utilizando la matriz de vista y proyección de la cámara.
 *
 * EXPLICACIÓN MATEMÁTICA:
 * Multiplicamos el vector homogéneo del punto 3D por la matriz combinada de Vista-Proyección (VP):
 *   [x_clip, y_clip, z_clip, w_clip]^T = VP * [x, y, z, 1.0]^T
 * Luego aplicamos la división de perspectiva:
 *   x_ndc = x_clip / w_clip
 *   y_ndc = y_clip / w_clip
 * Y mapeamos a coordenadas de pantalla normalizadas [0..1] (con origen en la esquina superior izquierda):
 *   x_screen = (x_ndc + 1.0) / 2.0
 *   y_screen = (1.0 - y_ndc) / 2.0
 */
internal fun projectWorldPointToScreen(
    x: Float,
    y: Float,
    z: Float,
    viewMatrix: FloatArray,
    projectionMatrix: FloatArray
): Pair<Float, Float>? {
    val viewProjectionMatrix = FloatArray(16)
    Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

    val worldPoint = floatArrayOf(x, y, z, 1.0f)
    val clipPoint = FloatArray(4)
    Matrix.multiplyMV(clipPoint, 0, viewProjectionMatrix, 0, worldPoint, 0)

    if (clipPoint[3] == 0.0f) return null
    val ndcX = clipPoint[0] / clipPoint[3]
    val ndcY = clipPoint[1] / clipPoint[3]

    val screenX = (ndcX + 1.0f) / 2.0f
    val screenY = (1.0f - ndcY) / 2.0f
    return Pair(screenX, screenY)
}

/**
 * 1. CAPTURA Y REDUCCIÓN DE RUIDO DEL DEPTH MAP:
 * Aplica un filtro espacial Gaussiano sobre el ShortArray de profundidad en milímetros
 * para rellenar huecos causados por reflejos (valores cero) y suavizar ruido.
 * Luego, ajusta una ecuación analítica de plano para la máscara de suelo usando mínimos cuadrados
 * y fuerza un gradiente continuo desde la base de la pantalla hacia el horizonte.
 */
internal fun smoothAndInterpolateDepthMap(
    depth: ShortArray,
    w: Int,
    h: Int,
    floorMasks: List<FloorMaskData>
): FloatArray {
    val smoothed = FloatArray(w * h)
    val kernel = floatArrayOf(
        1f, 2f, 1f,
        2f, 4f, 2f,
        1f, 2f, 1f
    )
    
    // Suavizado Gaussiano 3x3 que ignora ceros (huecos de reflexión)
    for (y in 0 until h) {
        for (x in 0 until w) {
            var sum = 0f
            var weightSum = 0f
            for (ky in -1..1) {
                val ny = y + ky
                if (ny in 0 until h) {
                    for (kx in -1..1) {
                        val nx = x + kx
                        if (nx in 0 until w) {
                            val d = depth[ny * w + nx].toInt() and 0xFFFF
                            if (d > 0) {
                                val wIdx = (ky + 1) * 3 + (kx + 1)
                                val weight = kernel[wIdx]
                                sum += d * weight
                                weightSum += weight
                            }
                        }
                    }
                }
            }
            smoothed[y * w + x] = if (weightSum > 0f) (sum / weightSum) / 1000f else 0f
        }
    }
    
    // Regresión lineal para estimar la ecuación analítica del plano de suelo en 2D:
    // 1/d = A * v + B, donde v es la coordenada vertical normalizada de pantalla [0..1]
    var sumX = 0f
    var sumY = 0f
    var sumXY = 0f
    var sumXX = 0f
    var count = 0
    
    for (y in 0 until h) {
        val v = y.toFloat() / (h - 1).coerceAtLeast(1)
        if (v > 0.45f) { // Focalizarse en el área inferior (suelo principal)
            for (x in 0 until w) {
                val d = smoothed[y * w + x]
                if (d > 0.3f && d < 6.0f) {
                    val u = x.toFloat() / (w - 1).coerceAtLeast(1)
                    var insideFloor = false
                    for (mask in floorMasks) {
                        if (mask.rect.contains(u, v)) {
                            insideFloor = true
                            break
                        }
                    }
                    if (insideFloor) {
                        sumX += v
                        sumY += 1.0f / d
                        sumXY += v * (1.0f / d)
                        sumXX += v * v
                        count++
                    }
                }
            }
        }
    }
    
    var A = 1.2f
    var B = -0.1f // Valores por defecto si la regresión no tiene suficientes datos
    if (count >= 15) {
        val denom = count * sumXX - sumX * sumX
        if (abs(denom) > 1e-5f) {
            A = (count * sumXY - sumX * sumY) / denom
            B = (sumY - A * sumX) / count
        }
    }
    
    // Corregir y rellenar usando la ecuación analítica continua
    for (y in 0 until h) {
        val v = y.toFloat() / (h - 1).coerceAtLeast(1)
        for (x in 0 until w) {
            val u = x.toFloat() / (w - 1).coerceAtLeast(1)
            var insideFloor = false
            for (mask in floorMasks) {
                if (mask.rect.contains(u, v)) {
                    insideFloor = true
                    break
                }
            }
            
            if (insideFloor) {
                val modelInvD = A * v + B
                val modelD = if (modelInvD > 0.05f) 1.0f / modelInvD else 15.0f
                val obsD = smoothed[y * w + x]
                
                // Si la profundidad observada es cero o difiere significativamente del plano, es incoherente
                if (obsD <= 0f || abs(obsD - modelD) > (0.15f * modelD + 0.25f)) {
                    smoothed[y * w + x] = modelD // Forzar profundidad analítica del modelo
                } else {
                    // Mezcla ponderada para mantener detalles y eliminar vibración
                    smoothed[y * w + x] = 0.6f * obsD + 0.4f * modelD
                }
            }
        }
    }
    
    return smoothed
}

/**
 * 2. REPROYECCIÓN DE PUNTOS TRIDIMENSIONALES (PointCloud Recalibration):
 * Proyecta la nube de puntos 3D al espacio de cámara y pantalla, comprueba si pertenecen
 * a la máscara de suelo, y si es así, ajusta sus coordenadas 3D para que coincidan con la
 * profundidad matemática suavizada del mapa corregido.
 */
internal fun recalibratePointCloud(
    rawPoints: FloatArray?,
    viewMatrix: FloatArray,
    projectionMatrix: FloatArray,
    correctedDepth: FloatArray,
    depthWidth: Int,
    depthHeight: Int,
    floorMasks: List<FloorMaskData>,
    detectedObjects: List<DetectedObjectData>
): FloatArray? {
    if (rawPoints == null) return null
    val count = rawPoints.size / 4
    if (count == 0) return rawPoints
    
    val recalibrated = rawPoints.clone()
    val invViewMatrix = FloatArray(16)
    val success = Matrix.invertM(invViewMatrix, 0, viewMatrix, 0)
    if (!success) return rawPoints
    
    for (i in 0 until count) {
        val px = rawPoints[4 * i]
        val py = rawPoints[4 * i + 1]
        val pz = rawPoints[4 * i + 2]
        
        val screenPos = projectWorldPointToScreen(px, py, pz, viewMatrix, projectionMatrix)
        if (screenPos != null) {
            val u = screenPos.first
            val v = screenPos.second
            
            // FILTRADO DE CONTORNOS POR SEGMENTACIÓN:
            // Descartar puntos que caen sobre objetos externos en la mesa (como laptop, mouse, peluche)
            var onExternalObject = false
            val surfaceTerms = listOf("floor", "ground", "carpet", "rug", "soil", "table", "desk", "surface", "tile")
            for (obj in detectedObjects) {
                val label = obj.label.lowercase()
                val isSurface = surfaceTerms.any { label.contains(it) }
                if (!isSurface && obj.confidence > 0.4f && obj.normalizedRect.contains(u, v)) {
                    onExternalObject = true
                    break
                }
            }
            
            if (onExternalObject) {
                recalibrated[4 * i + 3] = 0f // Descartar punto
                continue
            }
            
            var insideFloor = false
            for (mask in floorMasks) {
                if (mask.rect.contains(u, v)) {
                    insideFloor = true
                    break
                }
            }
            
            if (insideFloor) {
                val col = Math.round(u * (depthWidth - 1)).toInt().coerceIn(0, depthWidth - 1)
                val row = Math.round(v * (depthHeight - 1)).toInt().coerceIn(0, depthHeight - 1)
                val dCorr = correctedDepth[row * depthWidth + col]
                
                if (dCorr > 0.05f) {
                    val pCam = FloatArray(4)
                    Matrix.multiplyMV(pCam, 0, viewMatrix, 0, floatArrayOf(px, py, pz, 1.0f), 0)
                    
                    val currentDepth = -pCam[2]
                    if (currentDepth > 0.05f) {
                        val scale = dCorr / currentDepth
                        pCam[0] = pCam[0] * scale
                        pCam[1] = pCam[1] * scale
                        pCam[2] = -dCorr
                        
                        val pWorldNew = FloatArray(4)
                        Matrix.multiplyMV(pWorldNew, 0, invViewMatrix, 0, pCam, 0)
                        
                        recalibrated[4 * i] = pWorldNew[0]
                        recalibrated[4 * i + 1] = pWorldNew[1]
                        recalibrated[4 * i + 2] = pWorldNew[2]
                    }
                }
            }
        }
    }
    return recalibrated
}

internal fun isPointOnStablePlane(
    pos: Position,
    activePlanes: List<CopiedPlaneData>,
    planeStabilities: Map<String, Float>
): Boolean {
    for (plane in activePlanes) {
        val stability = planeStabilities[plane.id] ?: 0.0f
        val area = plane.extentX * 2.0f * plane.extentZ * 2.0f
        // Consideramos estable si el plano tiene estabilidad >= 1.0f y cumple el área mínima
        if (stability >= 1.0f && area >= 0.5f) {
            val dy = abs(pos.y - plane.translation[1])
            if (dy < 0.08f) {
                val dx = pos.x - plane.translation[0]
                val dz = pos.z - plane.translation[2]
                if (dx * dx + dz * dz < (plane.extentX * plane.extentX + plane.extentZ * plane.extentZ)) {
                    return true
                }
            }
        }
    }
    return false
}

/**
 * OPTIMIZACIÓN DE MUESTREO TEMPORAL Y PROCESAMIENTO EN HILO SECUNDARIO:
 * Ejecuta los filtros matemáticos pesados de la nube de puntos y el cálculo de estabilidad
 * en un hilo secundario (Dispatchers.Default) para no bloquear la hebra de renderizado UI/GL.
 *
 * Explicación matemática del filtro de desviación estándar (para defensa académica):
 * Para cada plano, mantenemos un historial de alturas (eje Y) de los últimos 10 frames.
 * Calculamos la desviación estándar (SD) de estas alturas para evaluar la vibración temporal
 * provocada por reflejos en el piso de azulejo:
 *   1. Media (μ) = (1 / N) * Σ(y_i)
 *   2. Varianza (σ²) = (1 / N) * Σ(y_i - μ)²
 *   3. Desviación estándar (σ) = sqrt(σ²)
 * Si σ > 0.02f metros (2 centímetros), el plano se considera inestable y se marca con baja estabilidad.
 *
 * Además, aplicamos un filtrado heurístico geométrico estricto:
 *   1. Inclinación: Descartamos planos cuya inclinación supere los 5 grados respecto al vector
 *      de gravedad (normalY >= cos(5°) ≈ 0.99619f).
 *   2. Altura Coherente: Descartamos planos flotantes cuya altura Y sea superior a -0.3 metros,
 *      ya que una superficie real sobre la que se camina debe estar por debajo del nivel de la cámara.
 *   3. Historial Mínimo: Se requiere un historial completo de al menos 10 frames para consolidar estabilidad.
 */
internal fun mergeAdjacentPlanes(
    planes: List<CopiedPlaneData>,
    rawPoints: FloatArray?
): List<CopiedPlaneData> {
    if (planes.size < 2) return planes

    // 1. Calcular los puntos de confianza (cantidad de puntos cercanos en la PointCloud) para cada plano
    val confidencePoints = IntArray(planes.size) { 0 }
    if (rawPoints != null) {
        val count = rawPoints.size / 4
        for (i in 0 until count) {
            val px = rawPoints[4 * i]
            val py = rawPoints[4 * i + 1]
            val pz = rawPoints[4 * i + 2]
            val pc = rawPoints[4 * i + 3]
            if (pc > 0.1f) {
                for (pIdx in planes.indices) {
                    val plane = planes[pIdx]
                    val dy = abs(py - plane.translation[1])
                    if (dy < 0.05f) {
                        val dx = abs(px - plane.translation[0])
                        val dz = abs(pz - plane.translation[2])
                        if (dx <= plane.extentX && dz <= plane.extentZ) {
                            confidencePoints[pIdx]++
                        }
                    }
                }
            }
        }
    }

    val mergedPlanes = planes.map { it.copy(translation = it.translation.clone()) }.toMutableList()
    val visited = BooleanArray(planes.size) { false }

    for (i in planes.indices) {
        if (visited[i]) continue
        
        // Búsqueda en anchura para agrupar planos adyacentes horizontalmente a alturas similares
        val groupIndices = mutableListOf<Int>()
        val queue = mutableListOf<Int>()
        queue.add(i)
        visited[i] = true
        
        while (queue.isNotEmpty()) {
            val curr = queue.removeAt(0)
            groupIndices.add(curr)
            
            for (j in planes.indices) {
                if (!visited[j]) {
                    val planeA = planes[curr]
                    val planeB = planes[j]
                    
                    val heightDiff = abs(planeA.translation[1] - planeB.translation[1])
                    if (heightDiff < 0.05f) {
                        val distCenterX = abs(planeA.translation[0] - planeB.translation[0])
                        val distCenterZ = abs(planeA.translation[2] - planeB.translation[2])
                        val sumExtentX = planeA.extentX + planeB.extentX
                        val sumExtentZ = planeA.extentZ + planeB.extentZ
                        
                        val dx = distCenterX - sumExtentX
                        val dz = distCenterZ - sumExtentZ
                        
                        if (dx <= 0.15f && dz <= 0.15f) {
                            visited[j] = true
                            queue.add(j)
                        }
                    }
                }
            }
        }
        
        if (groupIndices.size > 1) {
            var masterIdx = groupIndices[0]
            var maxConf = confidencePoints[masterIdx]
            for (gIdx in groupIndices) {
                val conf = confidencePoints[gIdx]
                if (conf > maxConf) {
                    maxConf = conf
                    masterIdx = gIdx
                }
            }
            
            val masterHeight = planes[masterIdx].translation[1]
            for (gIdx in groupIndices) {
                mergedPlanes[gIdx].translation[1] = masterHeight
            }
        }
    }
    
    return mergedPlanes
}

internal fun calculateStabilityInBackground(
    copiedData: CopiedFrameData,
    planeHistory: Map<String, PlaneHistoryEntry>
): StabilityResultPositions {
    val tempPoints = mutableListOf<Position>()
    val planeStabilities = mutableMapOf<String, Float>()
    val planeInclinations = mutableMapOf<String, Float>()
    var maxStability = 0.0f

    // COALESCENCIA DE PLANOS (Plane Merging Control):
    // Fusionar planos adyacentes horizontalmente a alturas similares antes de realizar los cálculos de estabilidad.
    val activePlanes = mergeAdjacentPlanes(copiedData.activePlanes, copiedData.rawPointCloudPoints)

    // A. Procesar y corregir el mapa de profundidad en background
    val correctedDepth = if (copiedData.isDepthSupported && copiedData.depthBuffer != null) {
        smoothAndInterpolateDepthMap(
            copiedData.depthBuffer,
            copiedData.depthWidth,
            copiedData.depthHeight,
            copiedData.floorMasks
        )
    } else {
        null
    }

    // B. Recalibrar la nube de puntos 3D usando el mapa de profundidad corregido y la segmentación de ML Kit
    val rawRecalibratedPoints = if (correctedDepth != null && copiedData.rawPointCloudPoints != null) {
        recalibratePointCloud(
            copiedData.rawPointCloudPoints,
            copiedData.viewMatrix,
            copiedData.projectionMatrix,
            correctedDepth,
            copiedData.depthWidth,
            copiedData.depthHeight,
            copiedData.floorMasks,
            copiedData.detectedObjects
        )
    } else {
        if (copiedData.rawPointCloudPoints != null) {
            val recalibrated = copiedData.rawPointCloudPoints.clone()
            val count = recalibrated.size / 4
            val surfaceTerms = listOf("floor", "ground", "carpet", "rug", "soil", "table", "desk", "surface", "tile")
            for (i in 0 until count) {
                val px = recalibrated[4 * i]
                val py = recalibrated[4 * i + 1]
                val pz = recalibrated[4 * i + 2]
                val screenPos = projectWorldPointToScreen(px, py, pz, copiedData.viewMatrix, copiedData.projectionMatrix)
                if (screenPos != null) {
                    val u = screenPos.first
                    val v = screenPos.second
                    var onExternalObject = false
                    for (obj in copiedData.detectedObjects) {
                        val label = obj.label.lowercase()
                        val isSurface = surfaceTerms.any { label.contains(it) }
                        if (!isSurface && obj.confidence > 0.4f && obj.normalizedRect.contains(u, v)) {
                            onExternalObject = true
                            break
                        }
                    }
                    if (onExternalObject) {
                        recalibrated[4 * i + 3] = 0f
                    }
                }
            }
            recalibrated
        } else {
            null
        }
    }

    // FILTRADO POR ALTURA MÁXIMA (Ignorar puntos altos):
    // Descarta en el acto cualquier punto o mini-plano que esté por encima de la zona baja.
    // Solo acepta puntos en el rango de suelo estimado (ej. entre -1.2 y -1.6 metros respecto a la cámara).
    // Optimización por Heurística de Altura Relativa para Entornos de Bajo Contraste y Alta Reflectancia.
    val camY = copiedData.cameraPoseTranslation[1]
    val recalibratedPointCloudPoints = if (rawRecalibratedPoints != null) {
        val count = rawRecalibratedPoints.size / 4
        val filteredList = FloatArray(rawRecalibratedPoints.size)
        var destIndex = 0
        for (i in 0 until count) {
            val px = rawRecalibratedPoints[4 * i]
            val py = rawRecalibratedPoints[4 * i + 1]
            val pz = rawRecalibratedPoints[4 * i + 2]
            val pc = rawRecalibratedPoints[4 * i + 3]
            if (pc <= 0.1f) continue // Descartar inmediatamente puntos marcados como obstáculos o inválidos
            val relY = py - camY
            if (relY in -1.6f..-1.2f || relY in -0.9f..-0.4f) {
                filteredList[destIndex] = px
                filteredList[destIndex + 1] = py
                filteredList[destIndex + 2] = pz
                filteredList[destIndex + 3] = pc
                destIndex += 4
            }
        }
        if (destIndex > 0) {
            filteredList.copyOf(destIndex)
        } else {
            null
        }
    } else {
        null
    }

    // C. Filtrado espacial de la nube de puntos (ahora recalibrada) con sensibilidad adaptada al contraste
    val filteredPointCloud = if (copiedData.isLowContrast) {
        // En bajo contraste (piso liso/lavado), incrementamos la sensibilidad permitiendo celdas menos pobladas
        filterPointCloudSpatial(recalibratedPointCloudPoints, minNeighbors = 2, maxHeightDev = 0.06f)
    } else {
        filterPointCloudSpatial(recalibratedPointCloudPoints, minNeighbors = 3, maxHeightDev = 0.04f)
    }

    // 2. Procesar cada plano activo
    for (plane in activePlanes) {
        val planeId = plane.id
        
        // A. Cálculo de inclinación con respecto a la gravedad
        val worldNormal = rotateVectorByQuaternion(floatArrayOf(0f, 1f, 0f), plane.rotation)
        val normalY = worldNormal[1]
        val inclinationRad = acos(normalY.coerceIn(-1.0f, 1.0f))
        val inclinationDeg = Math.toDegrees(inclinationRad.toDouble()).toFloat()
        planeInclinations[planeId] = inclinationDeg

        // B. Recuperar el historial temporal de alturas
        val history = planeHistory[planeId]
        val heights = history?.heights ?: emptyList()
        // El tamaño de historial requerido ahora varía dinámicamente según la calidad de la iluminación
        val hasEnoughHistory = heights.size >= copiedData.dynamicLimit

        // C. Cálculo de la desviación estándar (SD)
        val meanY = if (heights.isNotEmpty()) heights.average().toFloat() else plane.translation[1]
        val sd = if (heights.isNotEmpty()) {
            val sumSq = heights.map { (it - meanY) * (it - meanY) }.sum()
            Math.sqrt((sumSq / heights.size).toDouble()).toFloat()
        } else 999.0f

        // D. Validaciones y filtros heurísticos
        val area = plane.extentX * 2.0f * plane.extentZ * 2.0f
        val isAreaValid = area >= 0.5f
        val isInclinationValid = inclinationDeg <= 5.0f // Máxima inclinación tolerable de 5° (normalY >= 0.99619f)
        val isHeightCoherent = plane.translation[1] <= -0.3f // Evita planos flotantes cerca de la cámara
        val isSdValid = sd <= 0.02f // La vibración del plano debe ser menor a 2 cm

        // E. Validación de máscara de suelo de ML Kit (Filtrado Geométrico Cruzado)
        val screenPos = projectWorldPointToScreen(
            plane.translation[0],
            plane.translation[1],
            plane.translation[2],
            copiedData.viewMatrix,
            copiedData.projectionMatrix
        )

        var isInsideFloorMask = false
        var isHighConfidenceFloor = false

        if (screenPos != null) {
            for (mask in copiedData.floorMasks) {
                if (mask.rect.contains(screenPos.first, screenPos.second)) {
                    isInsideFloorMask = true
                    if (mask.confidence >= 0.85f) {
                        isHighConfidenceFloor = true
                    }
                    break
                }
            }
        }

        // El plano es estable si pasa todos los filtros geométricos clásicos Y cae dentro de la máscara de ML Kit.
        // Sin embargo, si la confianza de ML Kit es superior al 85%, se confirma la estabilidad de inmediato (bypass de historial geométrico).
        val isStable = if (isHighConfidenceFloor) {
            isAreaValid && isInclinationValid && isHeightCoherent
        } else {
            isAreaValid && isInclinationValid && isHeightCoherent && hasEnoughHistory && isSdValid && isInsideFloorMask
        }

        // Asignamos estabilidad binaria: 1.0f (estable) o 0.0f (inestable)
        val stability = if (isStable) 1.0f else 0.0f
        planeStabilities[planeId] = stability

        if (stability > maxStability) {
            maxStability = stability
        }
    }

    // 3. Procesar estimación de profundidad si está activa (ToF)
    val depthData = if (correctedDepth != null && copiedData.depthParams != null) {
        val list = mutableListOf<SampledDepthPoint>()
        val cols = 18
        val rows = 18
        val stepX = copiedData.depthWidth / (cols + 1)
        val stepY = copiedData.depthHeight / (rows + 1)
        for (c in 0 until cols) {
            val u = (c + 1) * stepX
            for (r in 0 until rows) {
                val v = (r + 1) * stepY
                val idx = v * copiedData.depthWidth + u
                if (idx >= 0 && idx < correctedDepth.size) {
                    val dCorr = correctedDepth[idx]
                    if (dCorr > 0f) {
                        list.add(SampledDepthPoint(u, v, (dCorr * 1000f).toInt()))
                    }
                }
            }
        }
        list
    } else {
        null
    }
    val params = copiedData.depthParams
    if (copiedData.isDepthSupported && depthData != null && params != null) {
        val cols = 18
        val rows = 18
        val wPoints = Array(cols) { arrayOfNulls<FloatArray>(rows) }

        for (pt in depthData) {
            val depthM = pt.depthMm / 1000f
            val x = (pt.u - params.cx) * depthM / params.fx
            val y = -(pt.v - params.cy) * depthM / params.fy
            val z = -depthM
            val worldPt = transformPoint(floatArrayOf(x, y, z), copiedData.cameraPoseTranslation, copiedData.cameraPoseRotation)
            val c = pt.u % cols
            val r = pt.v % rows
            wPoints[c][r] = worldPt
        }

        val horizontalPoints = mutableListOf<FloatArray>()
        for (c in 0 until cols - 1) {
            for (r in 0 until rows - 1) {
                val w0 = wPoints[c][r]
                val w1 = wPoints[c + 1][r]
                val w2 = wPoints[c][r + 1]

                if (w0 != null && w1 != null && w2 != null) {
                    val v1x = w1[0] - w0[0]
                    val v1y = w1[1] - w0[1]
                    val v1z = w1[2] - w0[2]

                    val v2x = w2[0] - w0[0]
                    val v2y = w2[1] - w0[1]
                    val v2z = w2[2] - w0[2]

                    val nx = v1y * v2z - v1z * v2y
                    val ny = v1z * v2x - v1x * v2z
                    val nz = v1x * v2y - v1y * v2x

                    val len = Math.sqrt((nx * nx + ny * ny + nz * nz).toDouble()).toFloat()
                    if (len > 0.0001f) {
                        val nuy = ny / len
                        if (abs(nuy) > 0.85f) {
                            horizontalPoints.add(w0)
                        }
                    }
                }
            }
        }

        val heightBucketSize = 0.06f
        val bucketCounts = mutableMapOf<Int, Int>()
        for (pt in horizontalPoints) {
            val bucket = Math.round(pt[1] / heightBucketSize).toInt()
            bucketCounts[bucket] = (bucketCounts[bucket] ?: 0) + 1
        }

        val largePlaneThreshold = 20
        val validBuckets = bucketCounts.filter { it.value >= largePlaneThreshold }.keys

        val spacing = 0.15f
        val gridMap = mutableMapOf<Pair<Int, Int>, Float>()
        val gridCount = mutableMapOf<Pair<Int, Int>, Int>()

        for (pt in horizontalPoints) {
            val bucket = Math.round(pt[1] / heightBucketSize).toInt()
            if (bucket in validBuckets) {
                val gx = Math.round(pt[0] / spacing).toInt()
                val gz = Math.round(pt[2] / spacing).toInt()
                val key = Pair(gx, gz)
                gridMap[key] = (gridMap[key] ?: 0f) + pt[1]
                gridCount[key] = (gridCount[key] ?: 0) + 1
            }
        }

        for ((key, sumY) in gridMap) {
            val count = gridCount[key] ?: 1
            val avgY = sumY / count
            tempPoints.add(Position(key.first * spacing, avgY, key.second * spacing))
        }
    } else {
        // Fallback a nube de puntos filtrada
        val count = filteredPointCloud.size
        if (count > 0) {
            val step = max(1, count / 120)
            for (i in 0 until count step step) {
                val pt = filteredPointCloud[i]
                tempPoints.add(Position(pt[0], pt[1], pt[2]))
            }
        }
    }

    // 4. Malla de puntos de planos detectados para visualización dot-based
    for (plane in activePlanes) {
        val ex = plane.extentX
        val ez = plane.extentZ
        val spacing = 0.15f

        var xi = -ex
        while (xi <= ex && tempPoints.size < 400) {
            var zi = -ez
            while (zi <= ez && tempPoints.size < 400) {
                if (isPointInPolygon(xi, zi, plane.polygon)) {
                    val worldPt = transformPoint(floatArrayOf(xi, 0.002f, zi), plane.translation, plane.rotation)
                    tempPoints.add(Position(worldPt[0], worldPt[1], worldPt[2]))
                }
                zi += spacing
            }
            xi += spacing
        }
    }

    // 5. Determinar estabilidad general (ALTA si hay al menos un plano que pasa todos los filtros)
    val hasStableLargePlane = activePlanes.any { plane ->
        (planeStabilities[plane.id] ?: 0.0f) >= 1.0f
    }
    val stabilityLevel = if (hasStableLargePlane) "ALTA" else "BAJA"

    val stableFlags = tempPoints.map { pos ->
        isPointOnStablePlane(pos, activePlanes, planeStabilities)
    }

    return StabilityResultPositions(
        planeStabilities = planeStabilities,
        planeInclinations = planeInclinations,
        maxStability = maxStability,
        stabilityLevel = stabilityLevel,
        finalDotPositions = tempPoints,
        finalDotStable = stableFlags
    )
}

/**
 * Optimización por Heurística de Altura Relativa para Entornos de Bajo Contraste y Alta Reflectancia.
 * Calcula el punto de intersección simulado desde el centro de la pantalla hacia el plano virtual
 * horizontal infinito ubicado a una altura de plano dinámica (floorY) en coordenadas mundiales.
 */
internal fun getSimulatedCenterRaycast(cameraPose: Pose, floorY: Float): Position {
    val C = cameraPose.translation
    val q = cameraPose.rotationQuaternion
    
    // Obtener vector frontal de la cámara R_camera * [0, 0, -1]
    val D = rotateVectorByQuaternion(floatArrayOf(0f, 0f, -1f), q)
    val Dy = D[1]
    
    return if (Dy < -0.05f) {
        val t = (floorY - C[1]) / Dy
        Position(
            C[0] + t * D[0],
            floorY,
            C[2] + t * D[2]
        )
    } else {
        val dirX = D[0]
        val dirZ = D[2]
        val lenXZ = kotlin.math.sqrt(dirX * dirX + dirZ * dirZ)
        if (lenXZ > 1e-5f) {
            val normX = dirX / lenXZ
            val normZ = dirZ / lenXZ
            Position(
                C[0] + normX * 2.0f,
                floorY,
                C[2] + normZ * 2.0f
            )
        } else {
            Position(C[0], floorY, C[2] - 2.0f)
        }
    }
}

/**
 * DISPERSIÓN DE RAYCASTS (Evitar zonas saturadas):
 * Evalúa si en un radio de 10 centímetros hay una aglomeración excesiva de puntos de la nube
 * que no pertenecen a la superficie activa (por ejemplo, acumulados sobre una laptop o mesa).
 * Si hay más de X (15) puntos en esa zona, se bloquea la colocación para evitar porterías flotantes.
 */
internal fun isPositionSaturatedNonFloor(
    targetPos: Position,
    pointCloud: com.google.ar.core.PointCloud?,
    camY: Float,
    validRange: ClosedFloatingPointRange<Float>,
    radius: Float = 0.10f,
    maxPoints: Int = 15
): Boolean {
    if (pointCloud == null) return false
    val points = pointCloud.points
    val count = points.remaining() / 4
    var accumulated = 0
    for (i in 0 until count) {
        val px = points.get(4 * i)
        val py = points.get(4 * i + 1)
        val pz = points.get(4 * i + 2)
        
        val dx = px - targetPos.x
        val dy = py - targetPos.y
        val dz = pz - targetPos.z
        val distSq = dx * dx + dy * dy + dz * dz
        
        if (distSq <= radius * radius) {
            val relY = py - camY
            // Si el punto NO está al nivel de la superficie activa, lo contamos
            if (relY !in validRange) {
                accumulated++
                if (accumulated > maxPoints) {
                    return true
                }
            }
        }
    }
    return false
}

/**
 * FORZAR PLANO BASE DESDE LOS PUNTOS VALIDADOS:
 * Toma el promedio de altura (Y) de los pocos puntos válidos que caen en la superficie activa
 * proyectados dentro del área que ML Kit confirmó. Si no hay suficientes, realiza fallback al
 * valor predeterminado (Y de la cámara - offset).
 */
internal fun getValidatedFloorAverageY(
    pointCloud: com.google.ar.core.PointCloud?,
    cameraPose: Pose,
    viewMatrix: FloatArray,
    projectionMatrix: FloatArray,
    floorMasks: List<FloorMaskData>,
    detectedObjects: List<DetectedObjectData>,
    defaultY: Float,
    relYRange: ClosedFloatingPointRange<Float>
): Float {
    if (pointCloud == null || floorMasks.isEmpty()) return defaultY
    val points = pointCloud.points
    val count = points.remaining() / 4
    val validHeights = mutableListOf<Float>()
    val camY = cameraPose.translation[1]
    
    val surfaceTerms = listOf("floor", "ground", "carpet", "rug", "soil", "table", "desk", "surface", "tile")
    
    for (i in 0 until count) {
        val px = points.get(4 * i)
        val py = points.get(4 * i + 1)
        val pz = points.get(4 * i + 2)
        val relY = py - camY
        
        if (relY in relYRange) {
            val screenPos = projectWorldPointToScreen(px, py, pz, viewMatrix, projectionMatrix)
            if (screenPos != null) {
                val u = screenPos.first
                val v = screenPos.second
                
                // Excluir puntos que caen sobre obstáculos
                var onExternalObject = false
                for (obj in detectedObjects) {
                    val label = obj.label.lowercase()
                    val isSurface = surfaceTerms.any { label.contains(it) }
                    if (!isSurface && obj.confidence > 0.4f && obj.normalizedRect.contains(u, v)) {
                        onExternalObject = true
                        break
                    }
                }
                
                if (!onExternalObject) {
                    val insideFloor = floorMasks.any { it.rect.contains(u, v) }
                    if (insideFloor) {
                        validHeights.add(py)
                    }
                }
            }
        }
    }
    
    if (validHeights.isEmpty()) return defaultY
    
    // AJUSTE DE ALTURA MEDIANTE MEDIANA ESTADÍSTICA (Snap to Surface):
    validHeights.sort()
    val mid = validHeights.size / 2
    val initialMedian = if (validHeights.size % 2 != 0) {
        validHeights[mid]
    } else {
        (validHeights[mid - 1] + validHeights[mid]) / 2.0f
    }
    
    // FILTRADO DE VALORES ATÍPICOS (RANSAC / Outlier Removal):
    // Si un punto se desvía más de 3 centímetros hacia arriba (py - initialMedian > 0.03f),
    // se clasifica como outlier (ej. puntos en laptop, control o mouse) y se elimina del cálculo.
    val cleanHeights = validHeights.filter { py -> py - initialMedian <= 0.03f }
    
    if (cleanHeights.isEmpty()) return initialMedian
    
    val cleanMid = cleanHeights.size / 2
    return if (cleanHeights.size % 2 != 0) {
        cleanHeights[cleanMid]
    } else {
        (cleanHeights[cleanMid - 1] + cleanHeights[cleanMid]) / 2.0f
    }
}

/**
 * 3. RESTRICCIÓN DE RAYCAST LOCALIZADO:
 * Unproyecta la coordenada de pantalla (u, v) hacia el plano horizontal virtual 3D a la altura floorY.
 */
internal fun getSimulatedTouchRaycast(
    cameraPose: Pose,
    u: Float,
    v: Float,
    viewMatrix: FloatArray,
    projectionMatrix: FloatArray,
    floorY: Float
): Position {
    val vpMatrix = FloatArray(16)
    Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
    
    val invVP = FloatArray(16)
    val success = Matrix.invertM(invVP, 0, vpMatrix, 0)
    if (!success) {
        return getSimulatedCenterRaycast(cameraPose, floorY)
    }
    
    // Coordenadas de dispositivo normalizadas (NDC)
    val ndcX = 2.0f * u - 1.0f
    val ndcY = 1.0f - 2.0f * v
    
    // Punto en plano cercano
    val nearClip = floatArrayOf(ndcX, ndcY, -1.0f, 1.0f)
    val nearWorld = FloatArray(4)
    Matrix.multiplyMV(nearWorld, 0, invVP, 0, nearClip, 0)
    if (nearWorld[3] == 0.0f) return getSimulatedCenterRaycast(cameraPose, floorY)
    val nearX = nearWorld[0] / nearWorld[3]
    val nearY = nearWorld[1] / nearWorld[3]
    val nearZ = nearWorld[2] / nearWorld[3]
    
    // Punto en plano lejano
    val farClip = floatArrayOf(ndcX, ndcY, 1.0f, 1.0f)
    val farWorld = FloatArray(4)
    Matrix.multiplyMV(farWorld, 0, invVP, 0, farClip, 0)
    if (farWorld[3] == 0.0f) return getSimulatedCenterRaycast(cameraPose, floorY)
    val farX = farWorld[0] / farWorld[3]
    val farY = farWorld[1] / farWorld[3]
    val farZ = farWorld[2] / farWorld[3]
    
    // Dirección del rayo
    val dx = farX - nearX
    val dy = farY - nearY
    val dz = farZ - nearZ
    val len = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    if (len < 1e-5f) return getSimulatedCenterRaycast(cameraPose, floorY)
    
    val dirX = dx / len
    val dirY = dy / len
    val dirZ = dz / len
    
    val cameraY = cameraPose.translation[1]
    return if (dirY < -0.05f) {
        val t = (floorY - cameraY) / dirY
        Position(
            cameraPose.translation[0] + t * dirX,
            floorY,
            cameraPose.translation[2] + t * dirZ
        )
    } else {
        getSimulatedCenterRaycast(cameraPose, floorY)
    }
}
