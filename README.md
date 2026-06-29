# RealKick AR ⚽🏆

* [Leer en Español 🇪🇸](./README.es.md)

**RealKick AR** is a next-generation interactive Augmented Reality mobile application built natively in **Kotlin** and **Jetpack Compose**. The app utilizes a combined pipeline of **ARCore** and **Google ML Kit** to classify surfaces, apply advanced geometric noise filtering, and deliver a stable and optimized 60 FPS interactive experience.

The app features two main modes:
1. **AR Penalty Shootout**: An interactive game where you place a 3D goal and take penalty kicks against an intelligent goalkeeper with real physics.
2. **AR Football History**: An interactive augmented reality museum that allows you to explore iconic 3D models (Uruguay 1930 Ball, Golden Boot, Silver Whistle) with detailed historical descriptions.

---

## 📸 Screenshots (Operation)

Below are real screenshots of the application running with elevated surface detection and augmented reality rendering:

| ⚽ Game Mode (Penalty Shootout) | 🏆 AR Football History Museum |
|:---:|:---:|
|


https://github.com/user-attachments/assets/f2b7cdda-2831-4b29-b396-e98ec4985984



https://github.com/user-attachments/assets/e5b8c56f-f92b-4a47-9a2d-5683846e0cca


| 

|
| *Placing the 3D goal on real flat surfaces* | *Exploring football relics in real-time* |

---

## 🛠️ Technical AR Features

To ensure a 100% stable out-of-the-box experience and resolve common issues in reflective environments or cluttered spaces (such as laptops or controllers on desks), we implemented a semi-assisted heuristic geometric pipeline:

### 1. Height-Based Plane Segregation (Delta-Y Isolation)
If the user scans the floor and then points at a table, ARCore tends to deform or stretch the original floor plane upwards. To prevent this:
- The system monitors the initial detection height of each plane (`initialHeight`).
- If a plane's height changes by more than **15 centimeters** (`deltaY > 0.15f`), the system anchors the original plane to its ground level and forces the creation of a **completely new and independent virtual plane** for the elevated surface.

### 2. Outlier Removal (RANSAC)
Prevents 3D models (like the Trophy or Goal) from floating or snapping upwards due to physical objects resting on the table (such as laptops, mice, or keyboards):
- The system calculates the statistical median height of the point cloud.
- If a group of points deviates more than **3 centimeters upwards** from the initial median, they are classified as *outliers* and discarded on the fly for the base surface calculation.

### 3. Plane Merging Control (Coalescence)
Prevents ARCore from fragmenting a single continuous surface into multiple floating mini-planes at slightly offset heights:
- If two adjacent horizontal planes in space have a height difference of less than **5 centimeters**, they are merged into a single master plane using the height of the surface with the highest count of high-confidence points.

### 4. Distance-Based Raycast (Raycast Layering)
- When touching the screen, instead of picking the first plane hit by the ray (which might be the floor in the background due to incorrect projection), the Raycast evaluates all planes intersected along its path.
- If it detects a plane whose height (Y) is closer to the physical camera (like a table), the system grants it **absolute priority** and discards the background plane.

### 5. Pitch-Driven Reset
- The system calculates the camera's tilt angle (**Pitch**) in real-time.
- If the user shifts from pointing downwards (floor) to pointing forwards/diagonally (table) with a rapid rotation greater than **15 degrees**, the unstable buffer of feature points is temporarily cleared. This forces a clean recalculation of the table's edges without bringing over visual interference from the floor.

### 6. Optimized Asynchronous Flow
- All heavy mathematical calculations for gradients (Sobel filter), median height, and coalescence run in the background using `Dispatchers.Default` at strict **300 ms** intervals.
- This ensures that the rendering thread for the 3D models and Compose UI never stutters, maintaining a stable **60 FPS**.

---

## 📂 Core Code Structure

The core logic of the application resides in the following Kotlin components under `app/src/main/java/com/example/realkick/ui/`:

- **[ARPlaneUtils.kt](file:///C:/Users/tonyp/.gemini/antigravity/scratch/RealKick/app/src/main/java/com/example/realkick/ui/ARPlaneUtils.kt)**: Contains all the mathematical logic for filtering, coordinate unprojection (`getSimulatedTouchRaycast`), plane merging, and RANSAC outlier removal.
- **[VisualAIAnalyzer.kt](file:///C:/Users/tonyp/.gemini/antigravity/scratch/RealKick/app/src/main/java/com/example/realkick/ui/VisualAIAnalyzer.kt)**: Processes camera frames using Google ML Kit Object Detection & Segmentation in real-time.
- **[ARGameScreen.kt](file:///C:/Users/tonyp/.gemini/antigravity/scratch/RealKick/app/src/main/java/com/example/realkick/ui/ARGameScreen.kt)**: The penalty shootout screen. Manages placement state, ball physics, kicks, and the goalkeeper's movement.
- **[HistoryARScreen.kt](file:///C:/Users/tonyp/.gemini/antigravity/scratch/RealKick/app/src/main/java/com/example/realkick/ui/HistoryARScreen.kt)**: The football history museum screen. Loads and rotates 3D models and shows detailed historical info cards.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or higher.
- A physical Android device with **Google Play Services for AR (ARCore)** installed.
- Android SDK 33+.

### Compiling and Running
1. Clone this repository to your local machine.
2. Open the project in Android Studio.
3. Clean and compile the project using Gradle:
   ```powershell
   ./gradlew clean compileDebugKotlin
   ```
4. Build and install the debug APK on your device:
   ```powershell
   ./gradlew assembleDebug
   ```

---

## 🌐 Technologies Used
- **Language**: Kotlin (100% native)
- **UI Toolkit**: Jetpack Compose (Material Design 3)
- **AR Engine**: Sceneview AR (based on Google Filament and ARCore)
- **Computer Vision**: Google ML Kit Object Detection & Segmentation
- **Concurrency**: Kotlin Coroutines & Flow

---

## 🔒 License
This project is open-source. Feel free to use the code for educational purposes and AR application development.
