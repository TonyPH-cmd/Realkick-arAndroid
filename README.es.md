# RealKick AR ⚽🏆

* [Read in English 🇺🇸](./README.md)

**RealKick AR** es una aplicación móvil de Realidad Aumentada interactiva de última generación construida de forma nativa en **Kotlin** y **Jetpack Compose**. La aplicación utiliza el pipeline combinado de **ARCore** y **Google ML Kit** para clasificar superficies, aplicar filtros avanzados contra ruido geométrico, y ofrecer una experiencia interactiva estable y optimizada a 60 FPS.

La aplicación cuenta con dos modalidades principales:
1. **Tanda de Penales AR**: Un juego interactivo donde colocas una portería 3D y disparas penales contra un arquero inteligente con físicas reales.
2. **Historia del Fútbol AR**: Un museo interactivo en realidad aumentada que te permite explorar modelos tridimensionales icónicos (Balón Uruguay 1930, Botín de Oro, Silbato de Plata) con información histórica detallada.

---

## 📸 Capturas de Pantalla (Funcionamiento)

A continuación se muestran capturas reales de la aplicación funcionando con detección de superficies elevadas y visualización en Realidad Aumentada:

| ⚽ Modo Juego (Tanda de Penales) | 🏆 Museo Histórico AR |
|:---:|:---:|
| 

https://github.com/user-attachments/assets/f2b7cdda-2831-4b29-b396-e98ec4985984



https://github.com/user-attachments/assets/e5b8c56f-f92b-4a47-9a2d-5683846e0cca


| *Colocación de portería 3D en superficies planas reales* | *Exploración de reliquias del fútbol en tiempo real* |

---

## 🛠️ Características Técnicas de Realidad Aumentada

Para garantizar un funcionamiento 100% estable de inmediato y solucionar los problemas habituales en entornos reflectantes o saturados de objetos (como laptops o controles), se implementó un pipeline geométrico heurístico semi-asistido:

### 1. Segregación de Planos por Capas de Altura (Delta-Y Isolation)
Si el usuario escanea el piso y luego apunta a una mesa, ARCore tiende a deformar o estirar el plano original del piso hacia arriba. Para evitar esto:
- El sistema monitorea la altura de detección inicial de cada plano (`initialHeight`).
- Si se detecta un cambio de altura en el plano superior a los **15 centímetros** (`deltaY > 0.15f`), el sistema ancla el plano original a su nivel de suelo y obliga a crear un **plano virtual totalmente nuevo e independiente** para la superficie elevada.

### 2. Filtrado de Valores Atípicos (RANSAC / Outlier Removal)
Evita que los modelos 3D (la Copa o la portería) floten o se desplacen hacia arriba debido a objetos depositados sobre la mesa (como laptops o ratones):
- Se calcula la mediana estadística de la nube de puntos.
- Si un grupo de puntos se desvía más de **3 centímetros hacia arriba** de la mediana inicial, se clasifican como *outliers* y se descartan en el acto para el cálculo del plano base.

### 3. Coalescencia de Planos (Plane Merging Control)
Evita que ARCore fragmente una sola superficie continua en múltiples mini-planos flotantes a alturas ligeramente desfasadas:
- Si dos planos horizontales adyacentes en el espacio tienen una diferencia de altura menor a **5 centímetros**, se fusionan en un único plano maestro utilizando la altura de la superficie con mayor cantidad de puntos de confianza de la nube de puntos.

### 4. Raycast Clasificado por Distancia (Raycast Layering)
- Al tocar la pantalla, en lugar de tomar el primer plano que impacte el rayo (que puede ser el piso de fondo debido a una mala proyección), el Raycast evalúa todos los planos impactados en su trayectoria.
- Si detecta un plano cuya altura (Y) esté más cerca de la posición física de la cámara (la mesa), el sistema le otorga **prioridad absoluta** y descarta el plano del fondo.

### 5. Reseteo Temporal por Cambios de Ángulo (Pitch-Driven Reset)
- El sistema calcula el ángulo de inclinación (**Pitch**) de la cámara del dispositivo en tiempo real.
- Si el usuario pasa de apuntar hacia abajo (piso) a apuntar hacia el frente/diagonal (mesa) con un cambio rápido superior a los **15 grados**, se vacía temporalmente el búfer inestable de puntos característicos. Esto fuerza a recalcular los bordes de la mesa sin arrastrar la interferencia visual del piso.

### 6. Flujo Asíncrono Optimizado
- Todos los cálculos matemáticos de gradientes (filtro de Sobel), mediana y coalescencia se ejecutan en segundo plano dentro de una corrutina utilizando `Dispatchers.Default` en intervalos de **300 ms**.
- Esto garantiza que el hilo de renderizado de los modelos 3D y la interfaz gráfica de usuario no sufra tirones, manteniendo los **60 FPS estables**.

---

## 📂 Estructura Principal del Código

El núcleo de lógica de la aplicación se encuentra en los siguientes componentes Kotlin dentro de `app/src/main/java/com/example/realkick/ui/`:

- **[ARPlaneUtils.kt](file:///C:/Users/tonyp/.gemini/antigravity/scratch/RealKick/app/src/main/java/com/example/realkick/ui/ARPlaneUtils.kt)**: Contiene toda la lógica matemática de filtrado, unproyección de coordenadas (`getSimulatedTouchRaycast`), fusión de planos y RANSAC para remover outliers.
- **[VisualAIAnalyzer.kt](file:///C:/Users/tonyp/.gemini/antigravity/scratch/RealKick/app/src/main/java/com/example/realkick/ui/VisualAIAnalyzer.kt)**: Se encarga de procesar los frames de cámara usando la segmentación y clasificación de Google ML Kit en tiempo real.
- **[ARGameScreen.kt](file:///C:/Users/tonyp/.gemini/antigravity/scratch/RealKick/app/src/main/java/com/example/realkick/ui/ARGameScreen.kt)**: Pantalla de la tanda de penales. Maneja el estado de colocación, físicas del balón, disparos y el arquero dinámico.
- **[HistoryARScreen.kt](file:///C:/Users/tonyp/.gemini/antigravity/scratch/RealKick/app/src/main/java/com/example/realkick/ui/HistoryARScreen.kt)**: Pantalla del museo histórico de fútbol. Permite cargar y rotar los modelos 3D y muestra tarjetas de información histórica detallada.

---

## 🚀 Cómo Empezar

### Prerrequisitos
- Android Studio Ladybug o superior.
- Dispositivo físico Android con soporte para **Google Play Services para RA (ARCore)**.
- Android SDK 33+.

### Compilación y Ejecución
1. Clona este repositorio en tu máquina local.
2. Abre el proyecto en Android Studio.
3. Ejecuta una limpieza y compilación mediante Gradle:
   ```powershell
   ./gradlew clean compileDebugKotlin
   ```
4. Construye e instala el APK de depuración en tu dispositivo:
   ```powershell
   ./gradlew assembleDebug
   ```

---

## 🌐 Tecnologías Utilizadas
- **Lenguaje**: Kotlin (100% nativo)
- **UI Toolkit**: Jetpack Compose (Material Design 3)
- **RA Engine**: Sceneview AR (basado en Google Filament y ARCore)
- **Computer Vision**: Google ML Kit Object Detection & Segmentation
- **Concurrencia**: Kotlin Coroutines & Flow

---

## 🔒 Licencia
Este proyecto es de código abierto. Siéntete libre de utilizar el código para fines educativos y de desarrollo de aplicaciones de RA.
