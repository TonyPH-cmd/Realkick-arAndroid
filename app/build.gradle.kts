plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

import java.net.URLClassLoader
import java.lang.reflect.Method
import java.util.zip.ZipFile
import java.io.File

android {
    namespace = "com.example.realkick"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.realkick"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // AR and 3D Rendering (SceneView)
  implementation("io.github.sceneview:arsceneview:4.18.0")
  implementation("com.google.ar:core:1.54.0")
  implementation("com.google.mlkit:object-detection:17.0.2")

  // CameraX dependencies for fallback
  val cameraxVersion = "1.3.4"
  implementation("androidx.camera:camera-camera2:$cameraxVersion")
  implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
  implementation("androidx.camera:camera-view:$cameraxVersion")
}

tasks.register("inspectMaterialLoader") {
    doLast {
        val classpathFiles = configurations.getByName("debugCompileClasspath").files
        val urls = classpathFiles.map { it.toURI().toURL() }.toTypedArray()
        val classLoader = URLClassLoader(urls, ClassLoader.getSystemClassLoader())
        try {
            val clazz = classLoader.loadClass("io.github.sceneview.loaders.MaterialLoader")
            println("=== io.github.sceneview.loaders.MaterialLoader Methods ===")
            for (method in clazz.declaredMethods) {
                val name: String = method.name
                val params: String = method.parameterTypes.map { it.simpleName }.joinToString()
                val ret: String = method.returnType.simpleName
                println("Method: $name($params) -> $ret")
            }
            println("==========================================================")
        } catch (e: Exception) {
            println("Failed to load class: ${e.message}")
            e.printStackTrace()
        }
    }
}

tasks.register("inspectHitResult") {
    doLast {
        val classpathFiles = configurations.getByName("debugCompileClasspath").files
        val urls = classpathFiles.map { it.toURI().toURL() }.toTypedArray()
        val classLoader = URLClassLoader(urls, ClassLoader.getSystemClassLoader())
        
        println("=== Searching for classes with HitResult ===")
        val searchClasses = listOf(
            "com.google.ar.core.HitResult",
            "io.github.sceneview.collision.HitResult",
            "io.github.sceneview.ar.arcore.ArHitResult",
            "io.github.sceneview.ar.ArHitResult"
        )
        for (className in searchClasses) {
            try {
                val clazz = classLoader.loadClass(className)
                println("Class found: $className")
                for (method in clazz.declaredMethods) {
                    val name: String = method.name
                    val params: String = method.parameterTypes.map { it.simpleName }.joinToString()
                    val ret: String = method.returnType.simpleName
                    println("  Method: $name($params) -> $ret")
                }
            } catch (e: Exception) {
                println("Class NOT found: $className")
            }
        }
        
        try {
            val clazz = classLoader.loadClass("io.github.sceneview.ar.ARSceneKt")
            println("=== io.github.sceneview.ar.ARSceneKt Methods ===")
            for (method in clazz.declaredMethods) {
                if (method.name.startsWith("ARScene")) {
                    println("Method: ${method.name}")
                    for (i in method.parameterTypes.indices) {
                        println("  Param $i: ${method.parameterTypes[i].name}")
                    }
                }
            }
        } catch (e: Exception) {
            println("Failed to load ARSceneKt: ${e.message}")
        }
    }
}

tasks.register("findSceneViewClasses") {
    doLast {
        val classpathFiles = configurations.getByName("debugCompileClasspath").files
        val outputFile = File("C:/Users/tonyp/.gemini/antigravity/brain/1d5fa4d8-1a27-4cab-8bde-b6b7d24c218d/scratch/classes.txt")
        outputFile.parentFile.mkdirs()
        outputFile.printWriter().use { writer ->
            writer.println("=== Scanning dependencies for classes ===")
            for (file in classpathFiles) {
                if (file.name.endsWith(".jar")) {
                    ZipFile(file).use { zip ->
                        val entries = zip.entries()
                        while (entries.hasMoreElements()) {
                            val entry = entries.nextElement()
                            if (entry.name.endsWith(".class")) {
                                val className = entry.name.replace('/', '.').removeSuffix(".class")
                                writer.println("Found in JAR (${file.name}): $className")
                            }
                        }
                    }
                } else if (file.name.endsWith(".aar")) {
                    ZipFile(file).use { aarZip ->
                        val classesJarEntry = aarZip.getEntry("classes.jar")
                        if (classesJarEntry != null) {
                            aarZip.getInputStream(classesJarEntry).use { input ->
                                val tempFile = File.createTempFile("classes", ".jar")
                                tempFile.deleteOnExit()
                                tempFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                                ZipFile(tempFile).use { zip ->
                                    val entries = zip.entries()
                                    while (entries.hasMoreElements()) {
                                        val entry = entries.nextElement()
                                        if (entry.name.endsWith(".class")) {
                                            val className = entry.name.replace('/', '.').removeSuffix(".class")
                                            writer.println("Found in AAR (${file.name}): $className")
                                        }
                                    }
                                }
                                tempFile.delete()
                            }
                        }
                    }
                }
            }
            writer.println("=== Scan Complete ===")
        }
        println("Scan complete. Output written to ${outputFile.absolutePath}")
    }
}





