package com.example.realkick.ui

import android.Manifest
import android.content.pm.PackageManager
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.ar.core.ArCoreApk
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tanh

internal data class GameVisualPoint(
val position: Position,
val isStable: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARGameScreen(
teamName: String,
onBack: () -> Unit
) {
val context = LocalContext.current

// Ã¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢Â
// AJUSTES DE MODELOS GLB (Modifica estos valores para alinear/escalar tus modelos)
// Ã¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢Â
val GOAL_ROTATION_Y = 90f       // RotaciÃƒÂ³n de la porterÃƒÂ­a en grados (90 o -90 si estÃƒÂ¡ lateral)
val GOAL_SCALE_MULT = 1.0f      // Multiplicador de escala de la porterÃƒÂ­a
val GOAL_POSITION_OFFSET = Position(0f, 0f, 0f) // Desplazamiento (X, Y, Z) de la porterÃƒÂ­a

val KEEPER_ROTATION_Y = 0f      // RotaciÃƒÂ³n del arquero (en grados)
val KEEPER_SCALE_MULT = 0.5f    // Escala del arquero (0.5 = 50% de su tamaÃƒÂ±o para que quepa en la porterÃƒÂ­a)
val KEEPER_POSITION_OFFSET = Position(0f, 0.0f, 0f)

val BALL_SCALE_MULT = 1.0f      // Multiplicador de escala del balÃƒÂ³n
val BALL_POSITION_OFFSET = Position(0f, 0f, 0f)

// Ã¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢Â
// 1. MANEJO DE PERMISOS DE CÃƒÂMARA
// Ã¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢Â
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

// Ã¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢Â
// 2. VALIDACIÃƒâ€œN DE COMPATIBILIDAD CON ARCORE
// Ã¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢Â
val arAvailability = remember {
try {
ArCoreApk.getInstance().checkAvailability(context)
} catch (e: Exception) {
ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE
}
}
val supportsAr = remember(arAvailability) {
arAvailability.isSupported
}

// Ã¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢Â
// 3. EQUIPO Y MATERIALES 3D
// Ã¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢Â
val team = remember { TEAMS_LIST.firstOrNull { it.name == teamName } ?: TEAMS_LIST[0] }

// Ã¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢Â
// 4. ESTADOS DE ARCORE Y DEL JUEGO
// Ã¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢Â
val frameState = remember { mutableStateOf<Frame?>(null) }
var anchorState by remember { mutableStateOf<Anchor?>(null) }

// Factor de escala adaptativo (1.0 = tamaÃƒÂ±o completo, 0.15 = tamaÃƒÂ±o mÃƒÂ­nimo escritorio)
var gameScale by remember { mutableStateOf(1.0f) }

var shotsLeft by remember { mutableStateOf(5) }
var goals by remember { mutableStateOf(0) }
var saves by remember { mutableStateOf(0) }
var misses by remember { mutableStateOf(0) }

// Si no soporta AR, colocar la porterÃƒÂ­a directamente
var statusText by remember {
mutableStateOf(
if (supportsAr) "Busca una superficie plana y tÃƒÂ³cala para colocar la porterÃƒÂ­a."
else "Ã‚Â¡PorterÃƒÂ­a 2D lista sobre la cÃƒÂ¡mara! Desliza para tirar."
)
}
var isGameOver by remember { mutableStateOf(false) }

// PosiciÃƒÂ³n dinÃƒÂ¡mica del balÃƒÂ³n
var ballPositionState by remember { mutableStateOf(Position(0f, 0.11f, 2.2f)) }
var isBallInFlight by remember { mutableStateOf(false) }

var keeperX by remember { mutableStateOf(0f) }
var isDepthSupported by remember { mutableStateOf(true) }

// Si no soporta AR, saltar colocaciÃƒÂ³n
val isGoalPlaced = remember(anchorState, supportsAr) {
anchorState != null || !supportsAr
}

// Ã¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢Â
// 5. ESTADO DE VISUALIZACIÃƒâ€œN DE PLANOS (PUNTOS BLANCOS VISIBLES)
// Ã¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢Â
val coroutineScope = rememberCoroutineScope()
var isProcessingStability by remember { mutableStateOf(false) }
var planeStabilities by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
var planeInclinations by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
var maxStability by remember { mutableStateOf(0.0f) }
var dotPositions by remember { mutableStateOf<List<GameVisualPoint>>(emptyList()) }
val lastUpdateTime = remember { LongArray(1) { 0L } }
val planeHistory = remember { mutableMapOf<String, PlaneHistoryEntry>() }
val initialPlaneHeights = remember { mutableMapOf<String, Float>() }
val lastPitch = remember { FloatArray(1) { 0f } }
var isLowContrast by remember { mutableStateOf(false) }
var edgeScreenPoints by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
var stabilityLevel by remember { mutableStateOf("BAJA") }
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
val dotFrameCounter = remember { intArrayOf(0) }
var smoothTableY by remember { mutableStateOf<Float?>(null) }
var smoothMinX by remember { mutableStateOf(0f) }
var smoothMaxX by remember { mutableStateOf(0f) }
var smoothMinZ by remember { mutableStateOf(0f) }
var smoothMaxZ by remember { mutableStateOf(0f) }

// Ã¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢Â
// 6. ANIMACIÃƒâ€œN DEL ARQUERO (MOVIMIENTO LATERAL ESCALADO)
// Ã¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢Â
LaunchedEffect(isGoalPlaced, isGameOver) {
if (isGoalPlaced && !isGameOver) {
var time = 0f
val dt = 0.016f
while (!isGameOver) {
time += dt
// El arquero se mueve lateralmente, amplitud escalada al espacio
keeperX = sin(time.toDouble() * 2.5).toFloat() * 0.8f * gameScale
delay(16)
}
}
}

// Ã¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢Â
// 7. MOTOR DE FÃƒÂSICAS DEL BALÃƒâ€œN (ESCALADO ADAPTATIVO)
// Ã¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢Â
LaunchedEffect(isGoalPlaced) {
if (isGoalPlaced) {
snapshotFlow { isBallInFlight }.collect { inFlight ->
if (inFlight) {
val s = gameScale
var pos = ballPositionState
var velX = ballVelocityX
var velY = ballVelocityY
var velZ = ballVelocityZ

// Todas las constantes fÃƒÂ­sicas escaladas por gameScale
val g = 9.8f * s
val dt = 0.016f
var bounces = 0

val ballRadius = 0.11f * s
val goalHalfWidth = 1.0f * s
val goalHeight = 1.2f * s
val keeperHalfWidth = 0.125f * s
val keeperHeight = 0.8f * s
val postThreshold = 0.12f * s
val keeperZone = 0.1f * s

var ballActive = true
while (ballActive) {
// Aplicar gravedad
velY -= g * dt

// Actualizar posiciÃƒÂ³n
pos = Position(
x = pos.x + velX * dt,
y = pos.y + velY * dt,
z = pos.z + velZ * dt
)

// 1. ColisiÃƒÂ³n con el suelo
if (pos.y <= ballRadius) {
pos = Position(pos.x, ballRadius, pos.z)
velY = -velY * 0.5f
bounces++
if (bounces > 4 || abs(velY) < 0.2f * s) {
velY = 0f
}
}

// 2. ColisiÃƒÂ³n con el Arquero
if (pos.z <= keeperZone && pos.z >= -keeperZone) {
if (pos.x >= keeperX - (keeperHalfWidth + ballRadius) &&
pos.x <= keeperX + (keeperHalfWidth + ballRadius) &&
pos.y <= keeperHeight + ballRadius
) {
saves++
shotsLeft--
statusText = "¡ATAJADA DEL ARQUERO! 🧤❌"
ballActive = false
break
}
}

// 3. ColisiÃƒÂ³n con la porterÃƒÂ­a o lÃƒÂ­nea de gol
if (pos.z <= 0f) {
val inWidth = pos.x >= -goalHalfWidth && pos.x <= goalHalfWidth
val inHeight = pos.y <= goalHeight && pos.y >= 0.0f

val hitsPost = (abs(pos.x - goalHalfWidth) < postThreshold ||
abs(pos.x + goalHalfWidth) < postThreshold) && pos.y <= goalHeight + postThreshold
val hitsCrossbar = abs(pos.y - goalHeight) < postThreshold &&
pos.x >= -(goalHalfWidth + 0.02f * s) && pos.x <= (goalHalfWidth + 0.02f * s)

if (hitsPost || hitsCrossbar) {
statusText = "¡AL PALO! 🪵😱"
misses++
shotsLeft--

// AnimaciÃƒÂ³n de rebote en el palo
var bouncePos = pos
var bounceVelX = -velX * 0.4f
var bounceVelY = velY * 0.3f
var bounceVelZ = -velZ * 0.4f

for (i in 0..25) {
bounceVelY -= g * dt
bouncePos = Position(
bouncePos.x + bounceVelX * dt,
Math.max(ballRadius, bouncePos.y + bounceVelY * dt),
bouncePos.z + bounceVelZ * dt
)
ballPositionState = bouncePos
delay(16)
}
} else if (inWidth && inHeight) {
goals++
shotsLeft--
statusText = "¡GOOOOOOOL! ⚽🔥"
} else {
misses++
shotsLeft--
statusText = "¡FUERA DE LA PORTERÍA! 😭"
}
ballActive = false
break
}

ballPositionState = pos
delay(16)
}

// Esperar 2 segundos antes del siguiente tiro o fin de juego
delay(2000)

if (shotsLeft <= 0) {
isGameOver = true
statusText = "¡Tanda de penales terminada!"
} else {
// Resetear balón al punto de penalización (escalado)
ballPositionState = Position(0f, 0.11f * s, 2.2f * s)
statusText = "Penal ${5 - shotsLeft + 1} de 5. ¡Desliza para tirar!"
}
isBallInFlight = false
}
}
}
}

// Ã¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢Â
// 8. INTERFAZ Y ESCENA AR (O FALLBACK 2D)
// Ã¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢Â
Box(modifier = Modifier.fillMaxSize()) {
if (hasCameraPermission) {
if (!supportsAr) {
// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
// FALLBACK 2D: Sin ARCore. PorterÃƒÂ­a sobrepuesta en 2D
// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
CameraPreview(modifier = Modifier.fillMaxSize())

// Banner superior de aviso de fallback
Card(
colors = CardDefaults.cardColors(containerColor = Color(0xE60D0D0F)),
shape = RoundedCornerShape(12.dp),
modifier = Modifier
.fillMaxWidth(0.9f)
.align(Alignment.TopCenter)
.padding(top = 160.dp),
border = BorderStroke(1.dp, Color(0xFF332211))
) {
Row(
modifier = Modifier.padding(12.dp),
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.spacedBy(10.dp)
) {
Text("⚠️Ã¯Â¸Â", fontSize = 20.sp)
Text(
text = "ARCore no disponible en este dispositivo. Mostrando porterÃƒÂ­a 2D sobrepuesta.",
color = Color(0xFFFFCC66),
fontSize = 11.sp,
lineHeight = 15.sp
)
}
}

// Escena 2D en el centro
Box(
modifier = Modifier
.fillMaxWidth()
.fillMaxHeight(0.65f)
.align(Alignment.Center)
) {
// 1. PorterÃƒÂ­a 2D
Box(
modifier = Modifier
.size(width = 240.dp, height = 150.dp)
.align(Alignment.TopCenter)
.padding(top = 50.dp)
.border(BorderStroke(4.dp, Color.White), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
.background(Color(0x22FFFFFF))
) {
// Red de porterÃƒÂ­a dibujada con lÃƒÂ­neas finas
Box(
modifier = Modifier
.fillMaxSize()
.background(
Brush.linearGradient(
colors = listOf(Color(0x11FFFFFF), Color(0x33FFFFFF))
)
)
)

// Arquero 2D
val s = gameScale
val keeperWidth = 40.dp
val keeperHeight = 84.dp

// Mapear keeperX (-0.8f a 0.8f) al espacio horizontal de la porterÃƒÂ­a
val adjustedKeeperX = if (s > 0f) keeperX / s else keeperX
val xOffset = (adjustedKeeperX * 105f).dp

Box(
modifier = Modifier
.size(width = keeperWidth, height = keeperHeight)
.align(Alignment.BottomCenter)
.offset(x = xOffset)
.clip(RoundedCornerShape(8.dp))
.background(team.primaryColor)
.border(BorderStroke(1.dp, Color.White), RoundedCornerShape(8.dp)),
contentAlignment = Alignment.Center
) {
Text(team.emoji, fontSize = 16.sp)
}
}

// 2. BalÃƒÂ³n 2D
val s = gameScale
val ballZ = ballPositionState.z
val ballX = ballPositionState.x
val ballY = ballPositionState.y

val adjustedBallZ = if (s > 0f) ballZ / s else ballZ
val adjustedBallX = if (s > 0f) ballX / s else ballX
val adjustedBallY = if (s > 0f) ballY / s else ballY

// Proyecciones a pantalla
val progress = (2.2f - adjustedBallZ) / 2.2f
val ballSize = (46f - 26f * progress).dp

val startY = 320f
val targetY = 150f - (adjustedBallY * 80f)
val currentY = startY + progress * (targetY - startY)
val currentX = adjustedBallX * 110f

Box(
modifier = Modifier
.offset(x = currentX.dp, y = currentY.dp)
.size(ballSize)
.clip(RoundedCornerShape(25.dp))
.background(Color.Red)
.border(BorderStroke(1.5.dp, Color.White), RoundedCornerShape(25.dp))
)
}
} else {
// MODO AR: Realidad Aumentada Real
// Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
val engine = rememberEngine()
val materialLoader = rememberMaterialLoader(engine)
val modelLoader = rememberModelLoader(engine)
val goalModelInstance = rememberModelInstance(modelLoader, "porteria.glb")
val ballModelInstance = rememberModelInstance(modelLoader, "balon.glb")
val keeperModelInstance = rememberModelInstance(modelLoader, "portero.glb")

val whiteMaterial = remember(materialLoader) {
materialLoader.createColorInstance(color = MaterialColor(1f, 1f, 1f, 1f), metallic = 0.2f, roughness = 0.8f)
}
val teamPrimaryMaterial = remember(materialLoader, team) {
materialLoader.createColorInstance(color = MaterialColor(team.primaryColor.red, team.primaryColor.green, team.primaryColor.blue, 1f), metallic = 0.5f, roughness = 0.4f)
}
val ballMaterial = remember(materialLoader) {
materialLoader.createColorInstance(color = MaterialColor(1f, 0f, 0f, 1f), metallic = 0.1f, roughness = 0.9f)
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

val scene = rememberScene(engine)

ARScene(
modifier = Modifier.fillMaxSize(),
engine = engine,
scene = scene,
planeRenderer = false,
sessionConfiguration = { session, config ->
// ConfiguraciÃ³n avanzada de enfoque y escaneo
config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
config.focusMode = Config.FocusMode.AUTO // Forzar autoenfoque para mitigar el desenfoque en azulejos

// Habilitar la estimaciÃ³n de iluminaciÃ³n HDR ambiental avanzada
config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR

// ConfiguraciÃ³n avanzada de cámara trasera para control de exposiciÃ³n
try {
val filter = com.google.ar.core.CameraConfigFilter(session)
filter.facingDirection = com.google.ar.core.CameraConfig.FacingDirection.BACK
val cameraConfigs = session.getSupportedCameraConfigs(filter)
if (cameraConfigs.isNotEmpty()) {
session.cameraConfig = cameraConfigs[0]
}
} catch (e: Exception) {
// Ignorar si el dispositivo no soporta filtrado de configuraciÃ³n de cámara
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
    val frame = frameState.value
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
                    statusText = "🏆 ¡Mesa Detectada - Modo Mini! Portería y arquero adaptados a escala de escritorio."
                } else {
                    gameScale = 0.8f
                    statusText = "¡Portería colocada en el piso a escala real! Desliza para tirar."
                }
                val s = gameScale
                
                anchorState = session.createAnchor(Pose.makeTranslation(targetPos.x, targetPos.y, targetPos.z))
                ballPositionState = Position(0f, 0.11f * s, 2.2f * s)
                dotPositions = emptyList()
            }
        }
    }
    true
} else {
    false
}
},
onSessionUpdated = { session, updatedFrame ->
arSession = session
frameState.value = updatedFrame

if (!isCalibrated && calibrationStartTime > 0L) {
    val timeElapsed = System.currentTimeMillis() - calibrationStartTime
    val pointCloud = try { updatedFrame.acquirePointCloud() } catch (e: Exception) { null }
    val pointCount = if (pointCloud != null) {
        val count = pointCloud.points.remaining() / 4
        pointCloud.release()
        count
    } else 0
    if (timeElapsed >= 2000 || pointCount >= 3) {
        isCalibrated = true;
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

// Downsampling ultra-rÃ¡pido a 80x60 en el hilo principal (toma menos de 0.5ms)
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
} catch (e: Exception) {
// Omitir en caso de buffers vacÃ­os o bloqueados
} finally {
cameraImage.close()
}
}

// ESTIMACIÃ“N DE ILUMINACIÃ“N Y FILTRADO POR COEFICIENTE:
// Obtenemos el coeficiente de intensidad de pÃ­xel ambiental y calculamos la penalizaciÃ³n por contraste
val lightEstimate = updatedFrame.lightEstimate
val currentPixelIntensity = if (lightEstimate.state == com.google.ar.core.LightEstimate.State.VALID) {
lightEstimate.pixelIntensity
} else {
0.5f
}
val lightDeviation = abs(currentPixelIntensity - 0.5f)
val contrastPenalty = if (isLowContrast) 0.5f else 0.0f

// CorrecciÃ³n matemÃ¡tica: a peor luz o menor contraste, aumentamos el buffer de estabilidad
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

// Actualizar el historial temporal con tamaÃ±o dinÃ¡mico basado en las condiciones de luz
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

// Paso B: Lanzar el procesamiento matemÃ¡tico (Sobel y estabilidad) en hilo secundario
coroutineScope.launch(Dispatchers.Default) {
var detectedIsLowContrast = isLowContrast
val detectedEdges = mutableListOf<Pair<Float, Float>>()

if (yGrid != null) {
val gridW = 80
val gridH = 60
val gradients = FloatArray(gridW * gridH)
var sumGradients = 0f

// Ejecutar filtro de Sobel para estimaciÃ³n de bordes y gradientes
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

// DetecciÃ³n de bordes estables
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
GameVisualPoint(pos, result.finalDotStable[i])
}

// Actualización dinámica del mensaje de estado
if (stabilityLevel == "ALTA") {
    statusText = if (targetedSurfaceType == "table") {
        "🏆 ¡Mesa Detectada - Modo Mini! Toca para colocar la portería."
    } else {
        "🏆 ¡Espacio Listo! Toca para colocar la portería."
    }
} else {
    if (!statusText.startsWith("⚠")) {
        statusText = "Busca una superficie plana y tójala para colocar la portería."
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

// Elementos 3D del juego
anchorState?.let { anchor ->
val s = gameScale
AnchorNode(anchor = anchor) {
// PorterÃƒÂ­a
if (goalModelInstance != null) {
ModelNode(
modelInstance = goalModelInstance,
position = GOAL_POSITION_OFFSET,
rotation = Rotation(y = GOAL_ROTATION_Y),
scale = Scale(s * GOAL_SCALE_MULT)
)
} else {
// Poste Izquierdo
CubeNode(
position = Position(-1.0f * s, 0.6f * s, 0.0f),
size = Size(0.08f * s, 1.2f * s, 0.08f * s),
materialInstance = whiteMaterial
)
// Poste Derecho
CubeNode(
position = Position(1.0f * s, 0.6f * s, 0.0f),
size = Size(0.08f * s, 1.2f * s, 0.08f * s),
materialInstance = whiteMaterial
)
// TravesaÃƒÂ±o
CubeNode(
position = Position(0.0f, 1.2f * s, 0.0f),
size = Size(2.08f * s, 0.08f * s, 0.08f * s),
materialInstance = whiteMaterial
)
}

// Arquero
if (keeperModelInstance != null) {
ModelNode(
modelInstance = keeperModelInstance,
position = Position(keeperX + KEEPER_POSITION_OFFSET.x, KEEPER_POSITION_OFFSET.y, KEEPER_POSITION_OFFSET.z),
rotation = Rotation(y = KEEPER_ROTATION_Y),
scale = Scale(s * KEEPER_SCALE_MULT)
)
} else {
CubeNode(
position = Position(keeperX, 0.4f * s, 0.0f),
size = Size(0.25f * s, 0.8f * s, 0.15f * s),
materialInstance = teamPrimaryMaterial
)
}

// BalÃƒÂ³n
if (ballModelInstance != null) {
ModelNode(
modelInstance = ballModelInstance,
position = Position(ballPositionState.x + BALL_POSITION_OFFSET.x, ballPositionState.y + BALL_POSITION_OFFSET.y, ballPositionState.z + BALL_POSITION_OFFSET.z),
scale = Scale(s * BALL_SCALE_MULT)
)
} else {
SphereNode(
position = ballPositionState,
radius = 0.11f * s,
materialInstance = ballMaterial
)
}
}
}
}
}
} else {
// Solicitar permisos de cÃƒÂ¡mara
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
Text("Permiso Requerido", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
Text("RealKick necesita acceso a tu cámara para proyectar la portería y el arquero en tu entorno real.", color = Color.LightGray, fontSize = 14.sp, textAlign = TextAlign.Center)
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
Text("Volver atrás", fontWeight = FontWeight.Bold, fontSize = 16.sp)
}
}
}
}
}

// Ã¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢Â
// 9. ZONA DE DESLIZAMIENTO PARA PENALES (MEJORADA PARA TIROS ESQUINADOS)
// Ã¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢Â
if (isGoalPlaced && !isBallInFlight && !isGameOver) {
Box(
modifier = Modifier
.fillMaxWidth()
.fillMaxHeight(0.4f)
.align(Alignment.BottomCenter)
.pointerInput(Unit) {
awaitPointerEventScope {
while (true) {
val down = awaitFirstDown()
val startX = down.position.x
val startY = down.position.y
val startTime = System.currentTimeMillis()

var lastX = startX
var lastY = startY

do {
val event = awaitPointerEvent()
val pointer = event.changes.firstOrNull()
if (pointer != null) {
lastX = pointer.position.x
lastY = pointer.position.y
pointer.consume()
}
} while (event.changes.any { it.pressed })

val endTime = System.currentTimeMillis()
val duration = (endTime - startTime).toFloat() / 1000f

val diffX = lastX - startX
val diffY = startY - lastY

// Deslizamiento de penal vÃƒÂ¡lido hacia arriba
if (diffY > 100f && duration < 1.0f) {
val s = gameScale
val speedFactor = (diffY / duration).coerceIn(100f, 4000f)

// Aumentar la sensibilidad horizontal (X) para permitir tiros esquinados de forma natural
ballVelocityX = ((diffX / 150f) * 2.2f * s).coerceIn(-3.2f * s, 3.2f * s)
ballVelocityY = ((diffY / 1000f) * 2.2f * s + 1.0f * s).coerceIn(1.0f * s, 4.0f * s)
ballVelocityZ = (-((speedFactor / 2000f) * 4.5f * s + 3.0f * s)).coerceIn(-8.0f * s, -3.5f * s)

isBallInFlight = true
}
}
}
}
)
}

// Ã¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢Â
// 10. PANEL SUPERIOR DE PUNTUACIÃƒâ€œN E INFORMACIÃƒâ€œN
// Ã¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢Â
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
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.SpaceBetween
) {
IconButton(
onClick = onBack,
colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0x80000000))
) {
Text("←", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
}

Card(
colors = CardDefaults.cardColors(containerColor = Color(0xE60A0A0A)),
shape = RoundedCornerShape(12.dp),
border = BorderStroke(1.dp, Color(0xFF1E1E22))
) {
Row(
modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
verticalAlignment = Alignment.CenterVertically
) {
Text(
text = "${team.emoji} ${team.name}",
color = Color.White,
fontWeight = FontWeight.Bold,
fontSize = 16.sp
)
}
}

Card(
colors = CardDefaults.cardColors(containerColor = Color(0xE60A0A0A)),
shape = RoundedCornerShape(12.dp),
border = BorderStroke(1.dp, Color(0xFF1E1E22))
) {
Text(
text = "Penales: ${5 - shotsLeft}/5",
color = Color.White,
modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
fontWeight = FontWeight.Bold,
fontSize = 16.sp
)
}
}

Spacer(modifier = Modifier.height(16.dp))

// Scoreboard
Card(
colors = CardDefaults.cardColors(containerColor = Color(0xD90A0A0A)),
shape = RoundedCornerShape(16.dp),
modifier = Modifier.fillMaxWidth(0.9f),
border = BorderStroke(
width = 1.dp,
brush = Brush.horizontalGradient(colors = listOf(Color.White, Color(0xFF333333)))
)
) {
Row(
modifier = Modifier.fillMaxWidth().padding(12.dp),
horizontalArrangement = Arrangement.SpaceEvenly,
verticalAlignment = Alignment.CenterVertically
) {
Column(horizontalAlignment = Alignment.CenterHorizontally) {
Text("GOLES", color = Color.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
Text("$goals", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
}
Divider(modifier = Modifier.height(30.dp).width(1.dp), color = Color.Gray)
Column(horizontalAlignment = Alignment.CenterHorizontally) {
Text("ATAJADOS", color = Color(0xFF3B82F6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
Text("$saves", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
}
Divider(modifier = Modifier.height(30.dp).width(1.dp), color = Color.Gray)
Column(horizontalAlignment = Alignment.CenterHorizontally) {
Text("FALLADOS", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
Text("$misses", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
}
}
}

if (supportsAr && anchorState == null) {
Spacer(modifier = Modifier.height(8.dp))
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

// Banner de advertencia si no tiene sensor ToF
if (supportsAr && !isDepthSupported) {
Card(
colors = CardDefaults.cardColors(containerColor = Color(0xE60D0D0F)),
shape = RoundedCornerShape(12.dp),
modifier = Modifier.fillMaxWidth(0.9f).padding(top = 12.dp),
border = BorderStroke(1.dp, Color(0xFF332211))
) {
Row(
modifier = Modifier.padding(12.dp),
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.spacedBy(10.dp)
) {
Text("⚠️Ã¯Â¸Â", fontSize = 18.sp)
Text(
text = "Tu celular no cuenta con sensor de profundidad (ToF) de hardware. El juego funcionará con menor precisión en el suelo.",
color = Color(0xFFFFCC66),
fontSize = 11.sp,
lineHeight = 15.sp
)
}
}
}

Spacer(modifier = Modifier.height(12.dp))

// Cartel de Estado/Instrucciones
Card(
colors = CardDefaults.cardColors(containerColor = Color(0xE60A0A0A)),
shape = RoundedCornerShape(12.dp),
modifier = Modifier.fillMaxWidth(0.9f),
border = BorderStroke(1.dp, Color(0xFF1E1E22))
) {
Text(
text = statusText,
color = Color.White,
modifier = Modifier.padding(12.dp),
textAlign = TextAlign.Center,
fontSize = 14.sp,
lineHeight = 18.sp
)
}
}

// Ã¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢Â
// 11. PANTALLA DE FIN DE JUEGO (GAME OVER OVERLAY)
// Ã¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢ÂÃ¢â€¢Â
if (isGameOver) {
Box(
modifier = Modifier.fillMaxSize().background(Color(0xE6000000)).clickable { },
contentAlignment = Alignment.Center
) {
Card(
modifier = Modifier.fillMaxWidth(0.85f).padding(16.dp),
shape = RoundedCornerShape(24.dp),
colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F12)),
elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
border = BorderStroke(
width = 2.dp,
brush = Brush.horizontalGradient(colors = listOf(Color.White, Color(0xFF333333)))
)
) {
Column(
modifier = Modifier.fillMaxWidth().padding(28.dp),
horizontalAlignment = Alignment.CenterHorizontally,
verticalArrangement = Arrangement.spacedBy(16.dp)
) {
Text("¡FIN DEL JUEGO!", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
Text("Resultado final con ${team.name} ${team.emoji}:", color = Color.LightGray, fontSize = 14.sp, textAlign = TextAlign.Center)
Spacer(modifier = Modifier.height(8.dp))

Row(
modifier = Modifier.fillMaxWidth(),
horizontalArrangement = Arrangement.SpaceAround
) {
Column(horizontalAlignment = Alignment.CenterHorizontally) {
Text("⚽ Goles", color = Color.Green, fontSize = 14.sp)
Text("$goals", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
}
Column(horizontalAlignment = Alignment.CenterHorizontally) {
Text("🧤 Atajadas", color = Color(0xFF3B82F6), fontSize = 14.sp)
Text("$saves", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
}
Column(horizontalAlignment = Alignment.CenterHorizontally) {
Text("❌ Fallas", color = Color.Red, fontSize = 14.sp)
Text("$misses", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
}
}

val ratingText = when (goals) {
5 -> "🏆 ¡Pichichi Perfecto! Leyenda total."
4 -> "🔥 ¡Goleador implacable! Qué clase."
3 -> "⚽ ¡Buen partido! Tienes gol."
2, 1 -> "⚠️ Falta entrenamiento en los penales."
else -> "😭 ¡Zapatero! Hoy no traías botines."
}

Text(
text = ratingText,
color = Color.Yellow,
fontWeight = FontWeight.Bold,
fontSize = 16.sp,
textAlign = TextAlign.Center,
modifier = Modifier.padding(vertical = 8.dp)
)

Spacer(modifier = Modifier.height(8.dp))

Button(
onClick = {
shotsLeft = 5
goals = 0
saves = 0
misses = 0
isGameOver = false
anchorState = null
isBallInFlight = false
ballPositionState = if (supportsAr) Position(0f, 0.11f, 2.2f) else Position(0f, 0.11f, 2.2f)
statusText = if (supportsAr) "Busca una superficie plana y tócala para colocar la portería."
else "¡Portería 2D lista sobre la cámara! Desliza para tirar."
},
modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(25.dp)),
colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
) {
Text("Volver a Jugar 🔄", fontWeight = FontWeight.Bold, fontSize = 16.sp)
}

OutlinedButton(
onClick = onBack,
modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(25.dp)),
border = BorderStroke(1.dp, Color.White),
colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
) {
Text("Cambiar Equipo 👥", fontWeight = FontWeight.Bold, fontSize = 16.sp)
}
}
}
}
}
}
}

// Variables globales para persistir la velocidad del tiro
private var ballVelocityX by mutableStateOf(0f)
private var ballVelocityY by mutableStateOf(0f)
private var ballVelocityZ by mutableStateOf(0f)

private fun getDepthMm(u: Int, v: Int, byteBuffer: java.nio.ByteBuffer, rowStride: Int, pixelStride: Int): Int {
val index = v * rowStride + u * pixelStride
if (index < 0 || index >= byteBuffer.limit() - 1) return 0
return byteBuffer.getShort(index).toInt() and 0xFFFF
}



