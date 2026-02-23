import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.kotlinSerialization)
  alias(libs.plugins.spotless)
  alias(libs.plugins.room)
  alias(libs.plugins.ksp)
}

kotlin {
  @OptIn(ExperimentalKotlinGradlePluginApi::class) @Suppress("DEPRECATION")
  androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }

  listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
    iosTarget.binaries.framework {
      baseName = "ComposeApp"
      isStatic = true
    }
  }

  sourceSets {
    androidMain.dependencies {
      implementation(libs.compose.uiToolingPreview)
      implementation(libs.androidx.activity.compose)
    }
    commonMain.dependencies {
      implementation(libs.compose.runtime)
      implementation(libs.compose.foundation)
      implementation(libs.compose.material3)
      implementation(libs.compose.ui)
      implementation(libs.compose.components.resources)
      implementation(libs.compose.uiToolingPreview)
      implementation(libs.androidx.lifecycle.viewmodelCompose)
      implementation(libs.androidx.lifecycle.runtimeCompose)
      implementation(libs.navigation.compose)

      implementation(libs.napier)

      implementation(libs.koog)

      implementation(libs.koin.core)
      implementation(libs.koin.compose.viewmodel)

      implementation(libs.orbit.core)
      implementation(libs.orbit.viewmodel)
      implementation(libs.orbit.compose)

      implementation(libs.multiplatform.settings)
      implementation(libs.multiplatform.settings.no.arg)

      implementation(libs.room.runtime)
      implementation(libs.sqlite.bundled)
    }
    commonTest.dependencies { implementation(libs.kotlin.test) }
  }
}

android {
  namespace = "io.ashkay.talon"
  compileSdk = libs.versions.android.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "io.ashkay.talon"
    minSdk = libs.versions.android.minSdk.get().toInt()
    targetSdk = libs.versions.android.targetSdk.get().toInt()
    versionCode = 1
    versionName = "1.0"
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      excludes += "META-INF/INDEX.LIST"
      excludes += "META-INF/io.netty.versions.properties"
      excludes += "META-INF/DEPENDENCIES"
    }
  }
  buildTypes { getByName("release") { isMinifyEnabled = false } }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

dependencies {
  debugImplementation(libs.compose.uiTooling)
  add("kspCommonMainMetadata", libs.room.compiler)
  add("kspAndroid", libs.room.compiler)
  //  add("kspIosX64", libs.room.compiler)
  add("kspIosArm64", libs.room.compiler)
  add("kspIosSimulatorArm64", libs.room.compiler)
}

room { schemaDirectory("$projectDir/schemas") }

spotless {
  kotlin {
    target("**/*.kt")
    ktfmt(libs.ktfmt.get().version) // Uses version from TOML
  }
  kotlinGradle {
    target("**/*.gradle.kts")
    ktfmt()
  }
}
