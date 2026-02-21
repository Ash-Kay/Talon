# Copilot Instructions for Compose Multiplatform Project

These are the coding rules and architectural guidelines to **always follow** when generating code in
this project.

---

## 🏗 Architecture & State Management

### Use **Orbit-MVI** for state management.

    - Always structure code using `ContainerHost<State, SideEffect>`.
    - State updates must go through `reduce {}`.
    - Side effects should be emitted via `postSideEffect()`.

### Follow **Clean Architecture principles**:

    - **Domain Layer**: Use cases (pure Kotlin, no Android/iOS dependencies).
    - **Data Layer**: Repository implementations, networking, persistence.
    - **Presentation Layer**: ViewModels (Orbit-MVI), UI (Compose).
    - Dependencies flow inward → UI → Presentation → Domain → Data.

### Add **Napier logs** on state changes:

    - Log when entering a reducer, side effect, or major ViewModel event.
    - Example:
      ```kotlin
      Napier.d("State updated: $state", tag = "MyViewModel")
      ```

---

## 📱 UI & Navigation

### Use **Jetpack Compose Navigation** for screen routing.

    - Define all routes centrally in a `NavGraph`.
    - Avoid manual navigation logic scattered across screens.

---

## 📱 Resources & Strings

### **Never use hardcoded strings**.

    - All strings must be defined in:
      ```
      composeApp/src/commonMain/composeResources/values/strings.xml
      ```
    - Always use the generated imports:
      ```kotlin
      import myproject.composeapp.generated.resources.Res
      import myproject.composeapp.generated.resources.my_string
      ```
      Example:
      ```kotlin
      Text(stringResource(Res.string.my_string))
      ```
    - Always use %1$s format for string formatting in XML.
    - instead of String.format(...) / "%.1f".format(), use Float/Double.formatToOneDecimal()
      extension function.

---

## ⚙️ Dependency Injection

### Use **Koin** for DI:

    - Define all modules (`viewModelModule`, `repositoryModule`, `networkModule`, etc.) in `commonMain`.
    - Prefer constructor injection for ViewModels and classes.
    - Example:
      ```kotlin
      val viewModelModule = module {
          viewModel { MyViewModel(get()) }
      }
      ```

---

## 📦 Dependency Management

### Always manage dependencies via **`libs.versions.toml`**.

    - Do not hardcode versions in `build.gradle.kts`.
    - Example (in `libs.versions.toml`):
      ```toml
      [versions]
      ktor = "2.3.7"
      
      [libraries]
      ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
      ```
    - Then use:
      ```kotlin
      implementation(libs.ktor.client.core)
      ```

---