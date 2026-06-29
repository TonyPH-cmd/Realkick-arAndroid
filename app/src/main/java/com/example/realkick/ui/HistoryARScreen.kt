package com.example.realkick.ui

import android.Manifest
import android.content.pm.PackageManager
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.ar.core.InstantPlacementPoint
import com.google.ar.core.PointCloud
import com.google.ar.core.DepthPoint
import java.nio.ByteOrder
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Size
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.SphereNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.node.ModelNode
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.rememberScene
import io.github.sceneview.ar.scene.PlaneRendererV2
import io.github.sceneview.ar.scene.PlaneRenderer
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.material.setParameter
import androidx.compose.runtime.DisposableEffect

import io.github.sceneview.math.Color as MaterialColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.ar.core.Pose
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.min
import kotlin.math.max
import kotlin.math.abs
import kotlin.math.tanh

private data class HistoryVisualPoint(
    val position: Position,
    val isStable: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryARScreen(
    modelId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val item = remember { HISTORY_ITEMS.firstOrNull { it.id == modelId } ?: HISTORY_ITEMS[0] }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // AJUSTES DE ESCALA PARA MODELOS HISTÓRICOS (.glb)
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    val WORLDCUP_SCALE_MULT = 0.35f  // Escala al 35% para tamaÃ±o realista (36.5 cm)
    val WORLDCUP_ROTATION_OFFSET = Rotation(y = 0f)
    val WORLDCUP_POSITION_OFFSET = Position(0f, 0f, 0f)

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 1. MANEJO DE PERMISOS DE CÃMARA
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 2. VALIDACIÃ“N DE COMPATIBILIDAD CON ARCORE
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    val arAvailability = remember {
        try {
            com.google.ar.core.ArCoreApk.getInstance().checkAvailability(context)
        } catch (e: Exception) {
            com.google.ar.core.ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE
        }
    }
    val supportsAr = remember(arAvailability) {
        arAvailability.isSupported
    }

    // Declarándolo aquí arriba para que sea accesible tanto al ARScene como al card inferior de instrucciones
    var statusText by remember { mutableStateOf("Escanea el suelo y pulsa para colocar el modelo 3D.") }
    var anchorState by remember { mutableStateOf<Anchor?>(null) }
    var stabilityLevel by remember { mutableStateOf("BAJA") }
    val coroutineScope = rememberCoroutineScope()
    var isProcessingStability by remember { mutableStateOf(false) }
    var planeStabilities by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var planeInclinations by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var maxStability by remember { mutableStateOf(0.0f) }
    var isLowContrast by remember { mutableStateOf(false) }
    var edgeScreenPoints by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    var detectedObjects by remember { mutableStateOf<List<DetectedObjectData>>(emptyList()) }
    var floorMasks by remember { mutableStateOf<List<FloorMaskData>>(emptyList()) }
    var targetedSurfaceType by remember { mutableStateOf("floor") }
    val visualAIAnalyzer = remember {
        VisualAIAnalyzer(object : VisualAICallback {
            @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            override fun onAnalysisResult(
                objects: List<DetectedObjectData>,
                masks: List<FloorMaskData>,
                lowContrast: Boolean
            ) {
                detectedObjects = objects
                floorMasks = masks
                isLowContrast = lowContrast
                
                // Fusión de etiquetas: clasificar si estamos apuntando a una mesa o piso en tiempo real
                val matchedMask = masks.firstOrNull { it.rect.contains(0.5f, 0.5f) }
                targetedSurfaceType = if (matchedMask != null) matchedMask.label else "floor"
            }
        })
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 3. CONFIGURACIÃ“N E HILO DE ROTACIÃ“N DE LOS MODELOS 3D
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    var theta by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            theta += 0.03f // Velocidad de rotaciÃ³n lenta
            if (theta > 2 * Math.PI) {
                theta -= (2 * Math.PI).toFloat()
            }
            delay(16)
        }
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 4. DISEÃ‘O DE INTERFAZ GENERAL
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    Box(modifier = Modifier.fillMaxSize()) {
        if (!hasCameraPermission) {
            // Solicitar permisos de cámara
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF000000), Color(0xFF0F0F12))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.85f).padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F12)),
                    border = BorderStroke(1.dp, Color(0xFF1E1E22))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Permiso de Cámara Requerido", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                        Text("Esta sección necesita acceso a la cámara para mostrar los modelos históricos en tu espacio real.", color = Color.LightGray, fontSize = 14.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(25.dp)),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                        ) {
                            Text("Conceder Permiso 📷", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(25.dp)),
                            border = BorderStroke(1.dp, Color.White),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Volver atrÃ¡s", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        } else if (!supportsAr) {
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // FALLBACK 2D: Para celulares que no soportan Realidad Aumentada
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            CameraPreview(modifier = Modifier.fillMaxSize())

            // Holograma 2D en el centro
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 180.dp),
                contentAlignment = Alignment.Center
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "hologram")
                val rotationAngle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(8000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotation"
                )
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                // Halo neon detrÃ¡s del emoji
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .rotate(rotationAngle)
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(
                                    Color(0x80D4AF37),
                                    Color(0x1A000000),
                                    Color(0x80D4AF37)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {}

                // Emoji flotante rotativo
                Text(
                    text = item.emoji,
                    fontSize = 80.sp,
                    modifier = Modifier
                        .rotate(rotationAngle * 0.2f) // RotaciÃ³n lenta contraria
                        .size(120.dp)
                        .align(Alignment.Center),
                    textAlign = TextAlign.Center
                )
            }

            // Banner superior de aviso de fallback
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xE60D0D0F)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp),
                border = BorderStroke(1.dp, Color(0xFF332211))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("âš ï¸", fontSize = 20.sp)
                    Text(
                        text = "ARCore no soportado. Mostrando el modelo en formato holograma 2D.",
                        color = Color(0xFFFFCC66),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // MODO AR: Realidad Aumentada Real con ARCore
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            val engine = rememberEngine()
            val materialLoader = rememberMaterialLoader(engine)
            val modelLoader = rememberModelLoader(engine)
            val worldCupModelInstance = rememberModelInstance(modelLoader, "copa_del_mundo.glb")
            val mascotModelInstance = rememberModelInstance(modelLoader, "mascota_2026.glb")
            val classicBallModelInstance = rememberModelInstance(modelLoader, "primer_balon.glb")
            val goldenBootModelInstance = rememberModelInstance(modelLoader, "botin_de_oro.glb")
            val whistleModelInstance = rememberModelInstance(modelLoader, "silbato_1930.glb")

            // Materiales de los objetos
            val goldMaterial = remember(materialLoader) {
                materialLoader.createColorInstance(color = MaterialColor(0.83f, 0.68f, 0.21f, 1f), metallic = 0.8f, roughness = 0.2f)
            }
            val silverMaterial = remember(materialLoader) {
                materialLoader.createColorInstance(color = MaterialColor(0.75f, 0.75f, 0.75f, 1f), metallic = 0.8f, roughness = 0.2f)
            }
            val brownMaterial = remember(materialLoader) {
                materialLoader.createColorInstance(color = MaterialColor(0.54f, 0.27f, 0.07f, 1f), metallic = 0.1f, roughness = 0.9f)
            }
            val whiteMaterial = remember(materialLoader) {
                materialLoader.createColorInstance(color = MaterialColor(1f, 1f, 1f, 1f), metallic = 0.2f, roughness = 0.8f)
            }
            val blackMaterial = remember(materialLoader) {
                materialLoader.createColorInstance(color = MaterialColor(0.1f, 0.1f, 0.1f, 1f), metallic = 0.2f, roughness = 0.8f)
            }
            val redMaterial = remember(materialLoader) {
                materialLoader.createColorInstance(color = MaterialColor(1f, 0f, 0f, 1f), metallic = 0.2f, roughness = 0.8f)
            }
            val blueMaterial = remember(materialLoader) {
                materialLoader.createColorInstance(color = MaterialColor(0f, 0f, 1f, 1f), metallic = 0.2f, roughness = 0.8f)
            }
            val beigeMaterial = remember(materialLoader) {
                materialLoader.createColorInstance(color = MaterialColor(0.82f, 0.70f, 0.55f, 1f), metallic = 0.1f, roughness = 0.9f)
            }
            val stableDotMaterial = remember(materialLoader) {
                materialLoader.createColorInstance(color = MaterialColor(1f, 1f, 1f, 1f), metallic = 0.0f, roughness = 0.5f)
            }
            val unstableDotMaterial = remember(materialLoader) {
                materialLoader.createColorInstance(color = MaterialColor(1f, 0f, 0f, 0.5f), metallic = 0.0f, roughness = 0.5f)
            }
            val readyDotMaterial = remember(materialLoader) {
                materialLoader.createColorInstance(color = MaterialColor(0.13f, 0.77f, 0.37f, 1f), metallic = 0.0f, roughness = 0.5f)
            }
            val cyanDotMaterial = remember(materialLoader) {
                materialLoader.createColorInstance(color = MaterialColor(0.02f, 0.71f, 0.83f, 1f), metallic = 0.0f, roughness = 0.5f)
            }
            var isCalibrated by remember { mutableStateOf(false) }
            var calibrationStartTime by remember { mutableStateOf(0L) }
            var arSession by remember { mutableStateOf<com.google.ar.core.Session?>(null) }

            LaunchedEffect(Unit) {
                calibrationStartTime = System.currentTimeMillis()
            }

            var frameState by remember { mutableStateOf<Frame?>(null) }
            var isDepthSupported by remember { mutableStateOf(true) }

            // Escala adaptativa de objetos
            var gameScale by remember { mutableStateOf(1.0f) }

            // Puntos de visualización de planos
            var dotPositions by remember { mutableStateOf<List<HistoryVisualPoint>>(emptyList()) }
            val lastUpdateTime = remember { LongArray(1) { 0L } }
            val planeHistory = remember { mutableMapOf<String, PlaneHistoryEntry>() }
            val initialPlaneHeights = remember { mutableMapOf<String, Float>() }
            val lastPitch = remember { FloatArray(1) { 0f } }
            val dotFrameCounter = remember { intArrayOf(0) }
            var smoothTableY by remember { mutableStateOf<Float?>(null) }
            var smoothMinX by remember { mutableStateOf(0f) }
            var smoothMaxX by remember { mutableStateOf(0f) }
            var smoothMinZ by remember { mutableStateOf(0f) }
            var smoothMaxZ by remember { mutableStateOf(0f) }

            val scene = rememberScene(engine)

            ARScene(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                scene = scene,
                planeRenderer = false,
                sessionConfiguration = { session, config ->
                    // Configuración avanzada de enfoque y escaneo
                    config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                    config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                    config.focusMode = Config.FocusMode.AUTO // Forzar autoenfoque para mitigar el desenfoque en azulejos

                    // Habilitar la estimación de iluminación HDR ambiental avanzada
                    config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR

                    // Configuración avanzada de cámara trasera para control de exposición
                    try {
                        val filter = com.google.ar.core.CameraConfigFilter(session)
                        filter.facingDirection = com.google.ar.core.CameraConfig.FacingDirection.BACK
                        val cameraConfigs = session.getSupportedCameraConfigs(filter)
                        if (cameraConfigs.isNotEmpty()) {
                            session.cameraConfig = cameraConfigs[0]
                        }
                    } catch (e: Exception) {
                        // Ignorar si el dispositivo no soporta filtrado de configuración de cámara
                    }
                    
                    val depthSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
                    isDepthSupported = depthSupported
                    config.depthMode = if (depthSupported) {
                        Config.DepthMode.AUTOMATIC
                    } else {
                        Config.DepthMode.DISABLED
                    }
                },
                onTouchEvent = { motionEvent, _ ->
                    if (anchorState == null && motionEvent.action == MotionEvent.ACTION_UP) {
                        val frame = frameState
                        val session = arSession
                        if (frame != null && session != null) {
                            val displayMetrics = context.resources.displayMetrics
                            val touchX = (motionEvent.x / displayMetrics.widthPixels).coerceIn(0f, 1f)
                            val touchY = (motionEvent.y / displayMetrics.heightPixels).coerceIn(0f, 1f)
                            
                            val matchedMask = floorMasks.firstOrNull { it.rect.contains(touchX, touchY) }
                            if (matchedMask == null) {
                                statusText = "⚠ Colocación bloqueada por IA: El punto de toque no está en una zona plana validada."
                            } else {
                                val targetedType = matchedMask.label
                                
                                // Evitación de bordes físicos y obstáculos (desplazamiento suave hacia el centro)
                                var adjustedX = touchX
                                var adjustedY = touchY
                                
                                val centerX = (matchedMask.rect.left + matchedMask.rect.right) / 2f
                                val centerY = (matchedMask.rect.top + matchedMask.rect.bottom) / 2f
                                val maskWidth = matchedMask.rect.right - matchedMask.rect.left
                                val maskHeight = matchedMask.rect.bottom - matchedMask.rect.top
                                val marginX = maskWidth * 0.15f
                                val marginY = maskHeight * 0.15f
                                
                                val distLeft = touchX - matchedMask.rect.left
                                val distRight = matchedMask.rect.right - touchX
                                val distTop = touchY - matchedMask.rect.top
                                val distBottom = matchedMask.rect.bottom - touchY
                                
                                val nearEdge = distLeft < marginX || distRight < marginX || distTop < marginY || distBottom < marginY
                                
                                var nearObstacle = false
                                val surfaceTerms = listOf("floor", "ground", "carpet", "rug", "soil", "table", "desk", "surface", "tile")
                                for (obj in detectedObjects) {
                                    val label = obj.label.lowercase()
                                    val isSurface = surfaceTerms.any { label.contains(it) }
                                    if (!isSurface && obj.confidence > 0.4f) {
                                        val oRect = obj.normalizedRect
                                        val dx = maxOf(0f, oRect.left - touchX, touchX - oRect.right)
                                        val dy = maxOf(0f, oRect.top - touchY, touchY - oRect.bottom)
                                        val distToObstacle = kotlin.math.sqrt(dx * dx + dy * dy)
                                        if (distToObstacle < 0.12f) {
                                            nearObstacle = true
                                            break
                                        }
                                    }
                                }
                                
                                if (nearEdge || nearObstacle) {
                                    val shiftFactor = 0.4f
                                    adjustedX = touchX + (centerX - touchX) * shiftFactor
                                    adjustedY = touchY + (centerY - touchY) * shiftFactor
                                }
                                
                                val viewMatrix = FloatArray(16)
                                frame.camera.getViewMatrix(viewMatrix, 0)
                                val projectionMatrix = FloatArray(16)
                                frame.camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f)
                                
                                val pointCloud = try { frame.acquirePointCloud() } catch (e: Exception) { null }
                                val cameraPose = frame.camera.pose
                                val camY = cameraPose.translation[1]
                                
                                // RAYCAST CLASIFICADO POR DISTANCIA (Raycast Layering):
                                // Evaluar todos los planos impactados por el raycast de toque y dar prioridad absoluta
                                // al que esté más cerca en altura (Y) de la cámara física (ej. la mesa antes que el piso).
                                var bestPlaneHeight: Float? = null
                                var minCamDist = Float.MAX_VALUE
                                
                                val allPlanes = session.getAllTrackables(Plane::class.java)
                                for (plane in allPlanes) {
                                    if (plane.trackingState == TrackingState.TRACKING &&
                                        plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING
                                    ) {
                                        val planeY = plane.centerPose.ty()
                                        val intersectPos = getSimulatedTouchRaycast(cameraPose, adjustedX, adjustedY, viewMatrix, projectionMatrix, planeY)
                                        
                                        val localX = intersectPos.x - plane.centerPose.tx()
                                        val localZ = intersectPos.z - plane.centerPose.tz()
                                        
                                        if (kotlin.math.abs(localX) <= plane.extentX && kotlin.math.abs(localZ) <= plane.extentZ) {
                                            val polyBuf = plane.polygon
                                            val polyArr = FloatArray(polyBuf.remaining())
                                            val oldPos = polyBuf.position()
                                            polyBuf.get(polyArr)
                                            polyBuf.position(oldPos)
                                            
                                            if (isPointInPolygon(localX, localZ, polyArr)) {
                                                val distY = kotlin.math.abs(planeY - camY)
                                                if (distY < minCamDist) {
                                                    minCamDist = distY
                                                    bestPlaneHeight = planeY
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                val relYRange = if (targetedType == "table") -0.9f..-0.4f else -1.6f..-1.2f
                                val defaultY = if (targetedType == "table") camY - 0.65f else camY - 1.4f
                                
                                val floorY = if (bestPlaneHeight != null) {
                                    bestPlaneHeight
                                } else {
                                    getValidatedFloorAverageY(pointCloud, cameraPose, viewMatrix, projectionMatrix, floorMasks, detectedObjects, defaultY, relYRange)
                                }
                                val targetPos = getSimulatedTouchRaycast(cameraPose, adjustedX, adjustedY, viewMatrix, projectionMatrix, floorY)
                                val isSaturated = isPositionSaturatedNonFloor(targetPos, pointCloud, camY, relYRange)
                                
                                pointCloud?.release()
                                
                                if (isSaturated) {
                                    statusText = "⚠ Colocación bloqueada por Heurística de Densidad: Zona de alta acumulación de objetos (mesa/computadora)."
                                } else {
                                    val selectedType = if (floorY - camY <= -1.0f) "PLANO_PISO" else "PLANO_MESA"
                                    if (selectedType == "PLANO_MESA") {
                                        gameScale = 0.3f
                                        statusText = "🏆 ¡Mesa Detectada - Modo Mini! Modelo adaptado a escala de escritorio."
                                    } else {
                                        gameScale = 1.0f
                                        statusText = "¡Modelo colocado mediante Heurística de Altura Fija! Muévete a su alrededor para inspeccionarlo."
                                    }
                                    
                                    anchorState = session.createAnchor(Pose.makeTranslation(targetPos.x, targetPos.y, targetPos.z))
                                    dotPositions = emptyList()
                                }
                            }
                        }
                    }
                    true
                },
                onSessionUpdated = { session, updatedFrame ->
                    arSession = session
                    frameState = updatedFrame

                    if (!isCalibrated && calibrationStartTime > 0L) {
                        val timeElapsed = System.currentTimeMillis() - calibrationStartTime
                        val pointCloud = try { updatedFrame.acquirePointCloud() } catch (e: Exception) { null }
                        val pointCount = if (pointCloud != null) {
                            val count = pointCloud.points.remaining() / 4
                            pointCloud.release()
                            count
                        } else 0
                        if (timeElapsed >= 2000 || pointCount >= 3) {
                            isCalibrated = true
                        }
                    }

                    // RESETEO TEMPORAL DE NUBE DE PUNTOS EN CAMBIOS DE ÁNGULO (Pitch-Driven Reset)
                    val cameraPose = updatedFrame.camera.pose
                    val q = cameraPose.rotationQuaternion
                    val pitchRad = kotlin.math.asin((2.0f * (q[3] * q[0] - q[1] * q[2])).coerceIn(-1.0f, 1.0f))
                    val pitchDeg = java.lang.Math.toDegrees(pitchRad.toDouble()).toFloat()

                    val lastPitchVal = lastPitch[0]
                    lastPitch[0] = pitchDeg

                    if (lastPitchVal != 0f) {
                        val pitchDelta = kotlin.math.abs(pitchDeg - lastPitchVal)
                        if (pitchDelta > 15.0f && kotlin.math.abs(pitchDeg) < 35.0f) {
                            planeHistory.clear()
                            initialPlaneHeights.clear()
                            dotPositions = emptyList()
                        }
                    }

                    // OPTIMIZACIÓN DE MUESTREO TEMPORAL MEDIANTE HILO SECUNDARIO (CADA 300 MS)
                    val currentTime = System.currentTimeMillis()
                    if (anchorState == null) {
                        if (currentTime - lastUpdateTime[0] >= 300 && !isProcessingStability) {
                            lastUpdateTime[0] = currentTime
                            isProcessingStability = true

                            // Paso A: Extraer y submuestrear el canal de luminancia Y del frame de cámara de forma segura
                            val cameraImage = try { updatedFrame.acquireCameraImage() } catch (e: Exception) { null }
                            var yGrid: ByteArray? = null
                            if (cameraImage != null) {
                                try {
                                    val yPlane = cameraImage.planes[0]
                                    val yBuffer = yPlane.buffer
                                    val rowStride = yPlane.rowStride
                                    val pixelStride = yPlane.pixelStride
                                    val width = cameraImage.width
                                    val height = cameraImage.height
                                    
                                    // Downsampling ultra-rápido a 80x60 en el hilo principal (toma menos de 0.5ms)
                                    val gridW = 80
                                    val gridH = 60
                                    val yCopied = ByteArray(gridW * gridH)
                                    val stepX = width / gridW
                                    val stepY = height / gridH
                                    
                                    for (y in 0 until gridH) {
                                        val sourceY = y * stepY
                                        val rowOffset = sourceY * rowStride
                                        for (x in 0 until gridW) {
                                            val sourceX = x * stepX
                                            val index = rowOffset + sourceX * pixelStride
                                            if (index >= 0 && index < yBuffer.limit()) {
                                                yCopied[y * gridW + x] = yBuffer.get(index)
                                            } else {
                                                yCopied[y * gridW + x] = 127.toByte()
                                            }
                                        }
                                    }
                                    yGrid = yCopied

                                    // --- INTEGRACIÓN CON ML KIT ---
                                    // Generamos el buffer NV21 a partir del Y-plane de resolución completa.
                                    // Esto nos permite alimentar la biblioteca de detección de objetos externa de forma asíncrona.
                                    val nv21Bytes = ByteArray(width * height * 3 / 2)
                                    
                                    // Copiar el Y-plane. Si pixelStride es 1 y rowStride == width, podemos usar una copia directa rápida
                                    if (pixelStride == 1 && rowStride == width) {
                                        yBuffer.rewind()
                                        yBuffer.get(nv21Bytes, 0, width * height)
                                    } else {
                                        // Copia por fila segura contemplando el rowStride (esquiva padding de memoria de la cámara)
                                        var destOffset = 0
                                        for (row in 0 until height) {
                                            yBuffer.position(row * rowStride)
                                            val remaining = yBuffer.remaining()
                                            val bytesToCopy = min(width, remaining)
                                            yBuffer.get(nv21Bytes, destOffset, bytesToCopy)
                                            destOffset += width
                                        }
                                    }
                                    
                                    // Rellenar la sección de crominancia (U/V) con 128 (gris neutro) para obtener un frame NV21 válido en escala de grises
                                    java.util.Arrays.fill(nv21Bytes, width * height, nv21Bytes.size, 128.toByte())
                                    
                                    // Analizar el frame con ML Kit de forma asíncrona
                                    // La cámara ARCore de Android suele tener una rotación de 90 grados en orientación vertical (portrait)
                                    visualAIAnalyzer.analyzeFrame(nv21Bytes, width, height, 90)

                                } catch (e: Exception) {
                                    // Omitir en caso de buffers vacíos o bloqueados
                                } finally {
                                    // Cierre garantizado en hilo principal para evitar fugas de memoria en el pool de imágenes de ARCore
                                    cameraImage.close()
                                }
                            }

                            // ESTIMACIÓN DE ILUMINACIÓN Y FILTRADO POR COEFICIENTE:
                            // Obtenemos el coeficiente de intensidad de píxel ambiental y calculamos la penalización por contraste
                            val lightEstimate = updatedFrame.lightEstimate
                            val currentPixelIntensity = if (lightEstimate.state == com.google.ar.core.LightEstimate.State.VALID) {
                                lightEstimate.pixelIntensity
                            } else {
                                0.5f
                            }
                            val lightDeviation = abs(currentPixelIntensity - 0.5f)
                            val contrastPenalty = if (isLowContrast) 0.5f else 0.0f
                            
                            // Corrección matemática: a peor luz o menor contraste, aumentamos el buffer de estabilidad
                            // dynamicLimit = 10 + (lightDeviation + contrastPenalty) * 16, acotado entre [10, 18]
                            val currentDynamicLimit = (10 + (lightDeviation + contrastPenalty) * 16).toInt().coerceIn(10, 18)

                            // Copiar datos activos de ARCore en el hilo principal de forma segura
                            val activePlanes = session.getAllTrackables(Plane::class.java)
                            val activePlanesList = mutableListOf<CopiedPlaneData>()
                            
                            for (plane in activePlanes) {
                                if (plane.trackingState == TrackingState.TRACKING &&
                                    plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                                    plane.subsumedBy == null
                                ) {
                                    val planeId = plane.hashCode().toString()
                                    val currentHeight = plane.centerPose.ty()
                                    
                                    // Calcular la normal del plano en el espacio del mundo para el almacenamiento
                                    val worldNormal = rotateVectorByQuaternion(floatArrayOf(0f, 1f, 0f), plane.centerPose.rotationQuaternion)

                                    // Actualizar el historial temporal con tamaño dinámico basado en las condiciones de luz
                                    val entry = planeHistory[planeId] ?: PlaneHistoryEntry(emptyList(), emptyList())
                                    val newHeights = (entry.heights + currentHeight).takeLast(currentDynamicLimit)
                                    val newNormals = (entry.normals + worldNormal).takeLast(currentDynamicLimit)
                                    planeHistory[planeId] = PlaneHistoryEntry(newHeights, newNormals)

                                    val polyBuf = plane.polygon
                                    val polyArr = FloatArray(polyBuf.remaining())
                                    polyBuf.get(polyArr)

                                    val initialHeight = initialPlaneHeights.getOrPut(planeId) { currentHeight }
                                    val deltaY = kotlin.math.abs(currentHeight - initialHeight)

                                    if (deltaY > 0.15f) {
                                        val splitId = "${planeId}_split"
                                        activePlanesList.add(
                                            CopiedPlaneData(
                                                id = splitId,
                                                translation = floatArrayOf(plane.centerPose.tx(), currentHeight, plane.centerPose.tz()),
                                                rotation = plane.centerPose.rotationQuaternion,
                                                extentX = plane.extentX,
                                                extentZ = plane.extentZ,
                                                polygon = polyArr
                                            )
                                        )
                                        activePlanesList.add(
                                            CopiedPlaneData(
                                                id = planeId,
                                                translation = floatArrayOf(plane.centerPose.tx(), initialHeight, plane.centerPose.tz()),
                                                rotation = plane.centerPose.rotationQuaternion,
                                                extentX = plane.extentX,
                                                extentZ = plane.extentZ,
                                                polygon = polyArr
                                            )
                                        )
                                    } else {
                                        activePlanesList.add(
                                            CopiedPlaneData(
                                                id = planeId,
                                                translation = plane.centerPose.translation,
                                                rotation = plane.centerPose.rotationQuaternion,
                                                extentX = plane.extentX,
                                                extentZ = plane.extentZ,
                                                polygon = polyArr
                                            )
                                        )
                                    }
                                }
                            }

                            val pointCloud = try { updatedFrame.acquirePointCloud() } catch (e: Exception) { null }
                            val rawPoints = if (pointCloud != null) {
                                val buffer = pointCloud.points
                                val arr = FloatArray(buffer.remaining())
                                buffer.get(arr)
                                pointCloud.release()
                                arr
                            } else null

                            // Obtener datos de profundidad si están soportados
                            var depthParams: DepthParams? = null
                            var depthBuffer: ShortArray? = null
                            var depthWidth = 0
                            var depthHeight = 0

                            if (isDepthSupported) {
                                val depthImage = try { updatedFrame.acquireDepthImage16Bits() } catch (e: Exception) { null }
                                if (depthImage != null) {
                                    try {
                                        depthWidth = depthImage.width
                                        depthHeight = depthImage.height
                                        val plane = depthImage.planes[0]
                                        val byteBuffer = plane.buffer.order(java.nio.ByteOrder.nativeOrder())
                                        val shortBuffer = byteBuffer.asShortBuffer()
                                        depthBuffer = ShortArray(shortBuffer.remaining())
                                        shortBuffer.get(depthBuffer)
                                        
                                        val camera = updatedFrame.camera
                                        val intrinsics = camera.textureIntrinsics
                                        val focalLength = intrinsics.focalLength
                                        val principalPoint = intrinsics.principalPoint
                                        val textureSize = intrinsics.imageDimensions
                                        
                                        val scaleX = depthWidth.toFloat() / textureSize[0]
                                        val scaleY = depthHeight.toFloat() / textureSize[1]
                                        val fx = focalLength[0] * scaleX
                                        val fy = focalLength[1] * scaleY
                                        val cx = principalPoint[0] * scaleX
                                        val cy = principalPoint[1] * scaleY
                                        
                                        depthParams = DepthParams(fx, fy, cx, cy)
                                    } finally {
                                        depthImage.close() // Cierre garantizado en hilo principal
                                    }
                                }
                            }

                            val cameraPose = updatedFrame.camera.pose
                            val camTrans = cameraPose.translation
                            val camRot = cameraPose.rotationQuaternion

                            val viewMatrix = FloatArray(16)
                            updatedFrame.camera.getViewMatrix(viewMatrix, 0)
                            val projectionMatrix = FloatArray(16)
                            updatedFrame.camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f)

                            val copiedData = CopiedFrameData(
                                activePlanes = activePlanesList,
                                rawPointCloudPoints = rawPoints,
                                sampledDepthPoints = null,
                                depthParams = depthParams,
                                cameraPoseTranslation = camTrans,
                                cameraPoseRotation = camRot,
                                isDepthSupported = isDepthSupported,
                                isLowContrast = isLowContrast,
                                pixelIntensity = currentPixelIntensity,
                                dynamicLimit = currentDynamicLimit,
                                viewMatrix = viewMatrix,
                                projectionMatrix = projectionMatrix,
                                floorMasks = floorMasks,
                                detectedObjects = detectedObjects,
                                depthBuffer = depthBuffer,
                                depthWidth = depthWidth,
                                depthHeight = depthHeight
                            )

                             val planeHistoryCopy = planeHistory.toMap()

                             // Paso B: Lanzar el procesamiento matemático (Sobel y estabilidad) en hilo secundario
                             coroutineScope.launch(Dispatchers.Default) {
                                 var detectedIsLowContrast = isLowContrast
                                 val detectedEdges = mutableListOf<Pair<Float, Float>>()

                                 if (yGrid != null) {
                                     val gridW = 80
                                     val gridH = 60
                                     val gradients = FloatArray(gridW * gridH)
                                     var sumGradients = 0f

                                     // Ejecutar filtro de Sobel para estimación de bordes y gradientes
                                     for (y in 1 until gridH - 1) {
                                         for (x in 1 until gridW - 1) {
                                             val val00 = (yGrid[(y - 1) * gridW + (x - 1)].toInt() and 0xFF).toFloat()
                                             val val01 = (yGrid[(y - 1) * gridW + x].toInt() and 0xFF).toFloat()
                                             val val02 = (yGrid[(y - 1) * gridW + (x + 1)].toInt() and 0xFF).toFloat()
                                             
                                             val val10 = (yGrid[y * gridW + (x - 1)].toInt() and 0xFF).toFloat()
                                             val val12 = (yGrid[y * gridW + (x + 1)].toInt() and 0xFF).toFloat()
                                             
                                             val val20 = (yGrid[(y + 1) * gridW + (x - 1)].toInt() and 0xFF).toFloat()
                                             val val21 = (yGrid[(y + 1) * gridW + x].toInt() and 0xFF).toFloat()
                                             val val22 = (yGrid[(y + 1) * gridW + (x + 1)].toInt() and 0xFF).toFloat()
                                             
                                             val gx = (val02 + 2f * val12 + val22) - (val00 + 2f * val10 + val20)
                                             val gy = (val20 + 2f * val21 + val22) - (val00 + 2f * val01 + val02)
                                             
                                             val g = Math.sqrt((gx * gx + gy * gy).toDouble()).toFloat()
                                             gradients[y * gridW + x] = g
                                             sumGradients += g
                                         }
                                     }
                                     
                                     val avgGradient = sumGradients / ((gridW - 2) * (gridH - 2))
                                     
                                     var varianceSum = 0f
                                     for (y in 1 until gridH - 1) {
                                         for (x in 1 until gridW - 1) {
                                             val g = gradients[y * gridW + x]
                                             varianceSum += (g - avgGradient) * (g - avgGradient)
                                         }
                                     }
                                     val variance = varianceSum / ((gridW - 2) * (gridH - 2))
                                     
                                     // Piso liso o lavado por la luz si la varianza del gradiente o el promedio es muy bajo
                                     detectedIsLowContrast = avgGradient < 12.0f || variance < 80.0f
                                     
                                     // Detección de bordes estables
                                     val edgeThreshold = 35.0f
                                     for (y in 1 until gridH - 1) {
                                         for (x in 1 until gridW - 1) {
                                             val g = gradients[y * gridW + x]
                                             if (g >= edgeThreshold) {
                                                 // Coordenadas normalizadas [0..1]
                                                 detectedEdges.add(Pair(x.toFloat() / gridW, y.toFloat() / gridH))
                                             }
                                         }
                                     }
                                 }

                                 val finalCopiedData = copiedData.copy(isLowContrast = detectedIsLowContrast)
                                 val result = calculateStabilityInBackground(finalCopiedData, planeHistoryCopy)

                                 // Volver a la hebra principal para actualizar los estados de Compose
                                 withContext(Dispatchers.Main) {
                                     isLowContrast = detectedIsLowContrast
                                     edgeScreenPoints = detectedEdges
                                     planeStabilities = result.planeStabilities
                                     planeInclinations = result.planeInclinations
                                     maxStability = result.maxStability
                                     stabilityLevel = if (isCalibrated) "ALTA" else result.stabilityLevel
                                      dotPositions = result.finalDotPositions.mapIndexed { i, pos ->
                                         HistoryVisualPoint(pos, result.finalDotStable[i])
                                     }
                                     
                                      // Actualización dinámica del mensaje de estado
                                      if (stabilityLevel == "ALTA") {
                                          statusText = if (targetedSurfaceType == "table") {
                                               "🏆 ¡Mesa Detectada - Modo Mini! Toca para colocar el modelo 3D."
                                          } else {
                                              "🏆 ¡Espacio Listo! Toca para colocar el modelo 3D."
                                          }
                                      } else {
                                          if (!statusText.startsWith("⚠️")) {
                                              statusText = "Escanea el suelo y pulsa para colocar el modelo 3D."
                                          }
                                      }
                                     isProcessingStability = false
                                 }
                             }
                        }
                    } else {
                        if (dotPositions.isNotEmpty()) {
                            dotPositions = emptyList()
                        }
                    }
                }

            ) {
                // Malla de puntitos blancos (estables) y rojos (inestables)
                if (anchorState == null) {
                    dotPositions.forEachIndexed { index, gamePoint ->
                        key(index) {
                            SphereNode(
                                position = gamePoint.position,
                                radius = 0.02f,
                                materialInstance = if (isCalibrated) {
                                    if (targetedSurfaceType == "table") cyanDotMaterial else readyDotMaterial
                                } else {
                                    if (gamePoint.isStable) stableDotMaterial else unstableDotMaterial
                                }
                            )
                        }
                    }
                }

                // Renderizar los modelos históricos
                anchorState?.let { anchor ->
                    val s = gameScale
                    AnchorNode(anchor = anchor) {
                        when (item.id) {
                            "copa_del_mundo" -> {
                                if (worldCupModelInstance != null) {
                                    ModelNode(
                                        modelInstance = worldCupModelInstance,
                                        position = WORLDCUP_POSITION_OFFSET,
                                        scale = Scale(s * WORLDCUP_SCALE_MULT),
                                        rotation = Rotation(y = Math.toDegrees(theta.toDouble()).toFloat() + WORLDCUP_ROTATION_OFFSET.y)
                                    )
                                } else {
                                    // Copa del Mundo
                                    // Base
                                    CubeNode(
                                        position = Position(0f, 0.05f * s, 0f),
                                        size = Size(0.16f * s, 0.1f * s, 0.16f * s),
                                        materialInstance = blackMaterial
                                    )
                                    // Tiras verdes de la base
                                    CubeNode(
                                        position = Position(0f, 0.11f * s, 0f),
                                        size = Size(0.17f * s, 0.02f * s, 0.17f * s),
                                        materialInstance = blueMaterial // Verde/Azul de copa
                                    )
                                    // Tallo central
                                    CubeNode(
                                        position = Position(0f, 0.22f * s, 0f),
                                        size = Size(0.08f * s, 0.2f * s, 0.08f * s),
                                        materialInstance = goldMaterial
                                    )
                                    // Mundo/Globo superior
                                    SphereNode(
                                        position = Position(0f, 0.38f * s, 0f),
                                        radius = 0.11f * s,
                                        materialInstance = goldMaterial
                                    )
                                    // Siluetas laterales que rotan orbitalmente
                                    val rX = 0.05f * s * cos(theta)
                                    val rZ = 0.05f * s * sin(theta)
                                    CubeNode(
                                        position = Position(-rX, 0.28f * s, -rZ),
                                        size = Size(0.04f * s, 0.16f * s, 0.04f * s),
                                        materialInstance = goldMaterial
                                    )
                                    CubeNode(
                                        position = Position(rX, 0.28f * s, rZ),
                                        size = Size(0.04f * s, 0.16f * s, 0.04f * s),
                                        materialInstance = goldMaterial
                                    )
                                }
                            }
                            "mascota_2026" -> {
                                if (mascotModelInstance != null) {
                                    ModelNode(
                                        modelInstance = mascotModelInstance,
                                        position = Position(0f, 0f, 0f),
                                        scale = Scale(s),
                                        rotation = Rotation(y = Math.toDegrees(theta.toDouble()).toFloat())
                                    )
                                } else {
                                    // Mascota 2026 (Cuerpo rojo, cabeza blanca, sombrero azul)
                                    val orbitCos = cos(theta)
                                    val orbitSin = sin(theta)

                                    // Cuerpo
                                    SphereNode(
                                        position = Position(0f, 0.16f * s, 0f),
                                        radius = 0.13f * s,
                                        materialInstance = redMaterial
                                    )
                                    // Cabeza
                                    SphereNode(
                                        position = Position(0f, 0.3f * s, 0f),
                                        radius = 0.09f * s,
                                        materialInstance = whiteMaterial
                                    )
                                    // Ojos que orbitan al frente
                                    val eyeOffsetX = 0.03f * s * orbitCos - 0.08f * s * orbitSin
                                    val eyeOffsetZ = 0.03f * s * orbitSin + 0.08f * s * orbitCos
                                    SphereNode(
                                        position = Position(-eyeOffsetX, 0.32f * s, -eyeOffsetZ),
                                        radius = 0.015f * s,
                                        materialInstance = blackMaterial
                                    )
                                    SphereNode(
                                        position = Position(eyeOffsetX, 0.32f * s, eyeOffsetZ),
                                        radius = 0.015f * s,
                                        materialInstance = blackMaterial
                                    )
                                    // Sombrero
                                    CubeNode(
                                        position = Position(0f, 0.4f * s, 0f),
                                        size = Size(0.09f * s, 0.03f * s, 0.09f * s),
                                        materialInstance = blueMaterial
                                    )
                                    // Brazos orbitales
                                    val armX = 0.15f * s * orbitCos
                                    val armZ = 0.15f * s * orbitSin
                                    CubeNode(
                                        position = Position(-armX, 0.2f * s, -armZ),
                                        size = Size(0.04f * s, 0.1f * s, 0.04f * s),
                                        materialInstance = whiteMaterial
                                    )
                                    CubeNode(
                                        position = Position(armX, 0.2f * s, armZ),
                                        size = Size(0.04f * s, 0.1f * s, 0.04f * s),
                                        materialInstance = whiteMaterial
                                    )
                                }
                            }
                            "primer_balon" -> {
                                if (classicBallModelInstance != null) {
                                    ModelNode(
                                        modelInstance = classicBallModelInstance,
                                        position = Position(0f, 0.16f * s, 0f),
                                        scale = Scale(s),
                                        rotation = Rotation(y = Math.toDegrees(theta.toDouble()).toFloat())
                                    )
                                } else {
                                    // BalÃ³n clÃ¡sico de cuero Uruguay 1930
                                    SphereNode(
                                        position = Position(0f, 0.16f * s, 0f),
                                        radius = 0.15f * s,
                                        materialInstance = brownMaterial
                                    )
                                    // Detalles de costura orbitales
                                    val laceX = -0.15f * s * sin(theta)
                                    val laceZ = 0.15f * s * cos(theta)
                                    CubeNode(
                                        position = Position(laceX, 0.28f * s, laceZ),
                                        size = Size(0.05f * s, 0.018f * s, 0.02f * s),
                                        materialInstance = beigeMaterial
                                    )
                                }
                            }
                            "botin_de_oro" -> {
                                if (goldenBootModelInstance != null) {
                                    ModelNode(
                                        modelInstance = goldenBootModelInstance,
                                        position = Position(0f, 0f, 0f),
                                        scale = Scale(s),
                                        rotation = Rotation(y = Math.toDegrees(theta.toDouble()).toFloat())
                                    )
                                } else {
                                    // BotÃ­n de Oro
                                    val orbitCos = cos(theta)
                                    val orbitSin = sin(theta)

                                    // Pedestal
                                    CubeNode(
                                        position = Position(0f, 0.04f * s, 0f),
                                        size = Size(0.24f * s, 0.08f * s, 0.12f * s),
                                        materialInstance = blackMaterial
                                    )
                                    // Suela del botÃ­n (orbita)
                                    val soleX = 0f * orbitCos
                                    val soleZ = 0f * orbitSin
                                    CubeNode(
                                        position = Position(soleX, 0.09f * s, soleZ),
                                        size = Size(0.2f * s, 0.02f * s, 0.08f * s),
                                        materialInstance = goldMaterial
                                    )
                                    // Punta y talÃ³n orbitando
                                    val toeX = 0.06f * s * orbitCos
                                    val toeZ = 0.06f * s * orbitSin
                                    CubeNode(
                                        position = Position(toeX, 0.13f * s, toeZ),
                                        size = Size(0.12f * s, 0.06f * s, 0.08f * s),
                                        materialInstance = goldMaterial
                                    )
                                    val heelX = -0.04f * s * orbitCos
                                    val heelZ = -0.04f * s * orbitSin
                                    CubeNode(
                                        position = Position(heelX, 0.18f * s, heelZ),
                                        size = Size(0.08f * s, 0.1f * s, 0.07f * s),
                                        materialInstance = goldMaterial
                                    )
                                }
                            }
                            "silbato_1930" -> {
                                if (whistleModelInstance != null) {
                                    ModelNode(
                                        modelInstance = whistleModelInstance,
                                        position = Position(0f, 0.1f * s, 0f),
                                        scale = Scale(s),
                                        rotation = Rotation(y = Math.toDegrees(theta.toDouble()).toFloat())
                                    )
                                } else {
                                    // Silbato HistÃ³rico de Plata
                                    val orbitCos = cos(theta)
                                    val orbitSin = sin(theta)

                                    // Cuerpo cilÃ­ndrico central
                                    SphereNode(
                                        position = Position(0f, 0.1f * s, 0f),
                                        radius = 0.08f * s,
                                        materialInstance = silverMaterial
                                    )
                                    // Boquilla orbitando
                                    val mouthX = 0.1f * s * orbitCos
                                    val mouthZ = 0.1f * s * orbitSin
                                    CubeNode(
                                        position = Position(mouthX, 0.11f * s, mouthZ),
                                        size = Size(0.09f * s, 0.04f * s, 0.05f * s),
                                        materialInstance = silverMaterial
                                    )
                                    // Anillo de agarre opuesto
                                    val ringX = -0.1f * s * orbitCos
                                    val ringZ = -0.1f * s * orbitSin
                                    CubeNode(
                                        position = Position(ringX, 0.1f * s, ringZ),
                                        size = Size(0.04f * s, 0.04f * s, 0.04f * s),
                                        materialInstance = silverMaterial
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Banner superior de aviso si NO hay sensor ToF
            if (!isDepthSupported) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xE60D0D0F)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp),
                    border = BorderStroke(1.dp, Color(0xFF332211))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("⚠️", fontSize = 20.sp)
                        Text(
                            text = "Tu celular no cuenta con sensor de profundidad (ToF). El rastreo de superficies será menos preciso.",
                            color = Color(0xFFFFCC66),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
        // 5. INTERFAZ DE CABECERA Y DETALLES HISTÓRICOS (MINIMALISTA)
        // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0x80000000))
                ) {
                    Text("←", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xE60A0A0A)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E1E22))
                ) {
                    Text(
                        text = "HISTORIA DEL FÚTBOL AR",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            if (supportsAr && anchorState == null) {
                Spacer(modifier = Modifier.height(12.dp))
                val stabilityColor = when (stabilityLevel) {
                    "ALTA" -> Color(0xFF22C55E)
                    "MEDIA" -> Color(0xFFEAB308)
                    else -> Color(0xFFEF4444)
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = stabilityColor.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, stabilityColor)
                ) {
                    Text(
                        text = "ESTABILIDAD ML: $stabilityLevel",
                        color = stabilityColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Tarjeta flotante inferior con la descripción histórica
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xD90A0A0A)),
            border = BorderStroke(
                width = 2.dp,
                brush = Brush.horizontalGradient(colors = listOf(Color.White, Color(0xFF333333)))
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = item.year,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider(color = Color(0xFF2C2C30), modifier = Modifier.height(1.dp))

                Text(
                    text = item.description,
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                if (supportsAr && anchorState == null) {
                    Text(
                        text = statusText,
                        color = Color(0xFFFFE57F),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

private fun getDepthMm(u: Int, v: Int, byteBuffer: java.nio.ByteBuffer, rowStride: Int, pixelStride: Int): Int {
    val index = v * rowStride + u * pixelStride
    if (index < 0 || index >= byteBuffer.limit() - 1) return 0
    return byteBuffer.getShort(index).toInt() and 0xFFFF
}

