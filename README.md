# PlantCare

Aplicación Android para identificar plantas por foto, llevar el seguimiento de sus cuidados (riego, trasplante, abono…) y aprender sobre ellas.

## Stack técnico

- **Kotlin 2.2** + **Jetpack Compose** (Material 3)
- **Hilt** para inyección de dependencias
- **Navigation Compose** (bottom navigation)
- **Room** + **DataStore** (persistencia local)
- **Retrofit** + **OkHttp** + **Kotlinx Serialization** (red)
- **CameraX** + **Coil 3** (cámara e imágenes)
- **WorkManager** (recordatorios de riego)
- **PlantNet API** para identificación de especies
- **Firebase** *(pendiente)*: Auth, Firestore, Storage, FCM, Crashlytics

## Arquitectura

MVVM + Clean Architecture (capas `data` / `domain` / `ui`).

```
com.BPO.plantcare/
├── PlantCareApplication.kt
├── MainActivity.kt
├── core/        # utils, theme transversal
├── data/        # repos, Room, APIs, Firebase
├── domain/      # modelos, use cases
├── di/          # módulos Hilt
└── ui/
    ├── theme/   # Color, Typography, Theme
    ├── navigation/
    └── screens/
        ├── home/
        ├── myplants/
        ├── calendar/
        ├── search/
        └── profile/
```

## Configuración local

1. Abre el proyecto en **Android Studio** (Narwhal 2025.3.4 Patch 1 o superior).
2. Tu `local.properties` debe contener:
   ```
   sdk.dir=...
   PLANTNET_API_KEY=tu_api_key
   ```
   *(Pide una key gratuita en https://my.plantnet.org)*
3. *Gradle sync* y *Run* sobre un dispositivo Android 8.0+ (API 26+).

## Roadmap

- **Fase 1 — MVP**: cámara, identificación con PlantNet, ficha de planta, mis plantas, recordatorios de riego.
- **Fase 2 — Cuidado avanzado**: calendario completo, diario fotográfico, diagnóstico por foto, catálogo y buscador con filtros.
- **Fase 3 — Social**: comunidades, foro, chat, perfiles públicos.
- **Fase 4 — Monetización + iOS**: freemium (RevenueCat) y versión SwiftUI.
