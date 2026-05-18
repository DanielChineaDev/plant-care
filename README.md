# 🌱 PlantCare

Aplicación Android para identificar plantas por foto, llevar el seguimiento de sus cuidados (riego, trasplante, abono…) y descubrir nuevas especies. Pensada para que tus plantas estén siempre felices 😊 y nunca se te olvide regarlas.

> **Estado:** MVP funcional completo, en desarrollo activo.
> **Repo:** [github.com/DanielChineaDev/PlantCare](https://github.com/DanielChineaDev/PlantCare)

---

## ✨ Features

### 🔍 Identificación
- Identificación de plantas por foto con **PlantNet API** (gratis, 500 IDs/día)
- Captura con cámara (CameraX) o elección desde **galería del sistema** (Photo Picker moderno)
- Lista de sugerencias con thumbnail, nombre común, nombre científico, familia y barra de confianza
- Botón "Añadir a mis plantas" directamente desde cada sugerencia

### 🪴 Mis plantas
- Persistencia local con **Room** (con migraciones reales, sin destructive fallback)
- Grid 2-col con foto, nombre, próximo riego e **indicador de estado**:
  - 😊 Feliz · 😐 Atenta · 🥵 Sedienta · 🌱 Sin regar aún
- Botón rápido de gota para marcar como regada desde la tarjeta
- **Snackbar "Deshacer"** al borrar un riego del historial

### 📋 Ficha de planta
- Hero con la foto del usuario o referencia de PlantNet
- Edición de alias con dialog
- **Cuidados** (luz, humedad, riego, sustrato, abono, trasplante, toxicidad para mascotas, curiosidad) cuando la especie está en el catálogo
- **Match por género**: si tu especie no está en el catálogo pero hay otra del mismo género, muestra los cuidados con un aviso de "datos aproximados"
- **Diario fotográfico** con thumbnails horizontales y visor fullscreen (zoom + swipe)
- **Historial de riegos** completo con timeline y opción de borrar entradas erróneas
- **Sobre esta planta**: descripción cargada de Wikipedia (español → fallback inglés) con thumbnail y enlace al artículo
- Eliminar planta con confirmación (borra también fotos y logs en cascada)

### 📅 Calendario
- Vista mensual con **Kizitonwose Calendar Compose**
- Card "Tareas de hoy" arriba con lista de riegos pendientes
- Puntos verdes en cada día con eventos
- Tap en un día → eventos detallados (regados pasados / pendientes / atrasados)

### 🔎 Buscador
- Catálogo navegable con búsqueda por texto
- Filtros: ubicación (interior/exterior), dificultad, nivel de luz
- Ficha de catálogo con foto de Wikipedia + cuidados + botón "Añadir a mis plantas"

### 🔔 Recordatorios
- Notificación diaria con WorkManager listando las plantas que toca regar hoy
- **Hora configurable** desde Perfil (dropdown 00:00..23:00)
- Switch ON/OFF para activar/desactivar
- Botón "Probar notificación" para verificar sin esperar

### 🏠 Widget de pantalla de inicio
- Widget Glance que muestra "Hoy toca regar: X plantas" + lista
- Refresco al instante cuando riegas/añades/borras desde la app
- Refresco automático cada 30 min por el sistema y a la hora del recordatorio
- Toque → abre la app

### 🎨 UI/UX
- Material 3 con paleta vegetal (light + dark) — verde hoja, marrón tierra, dorado sol
- Bottom nav con **badges numéricos** en Mis plantas y Calendario
- Transiciones suaves entre pantallas (cross-fade tabs, slide horizontal detalles)
- Hero CTA en empty states ("Identificar mi primera planta")

---

## 🛠 Stack técnico

| Área | Tecnología |
|------|-----------|
| Lenguaje | **Kotlin 2.0.21** |
| UI | **Jetpack Compose** + Material 3 |
| Arquitectura | MVVM + Clean Architecture (data / domain / ui) |
| DI | **Hilt** 2.52 (KSP) |
| Navegación | Navigation Compose 2.8 |
| Persistencia | **Room** 2.6 (con migraciones manuales) + DataStore Preferences |
| Red | Retrofit + OkHttp + Kotlinx Serialization |
| Imágenes | **Coil 3** + CameraX |
| Background | **WorkManager** + Hilt-Work |
| Widget | **Glance** 1.1 (AppWidget + Material3) |
| Calendario | Kizitonwose Calendar Compose 2.5 |
| Permisos | Accompanist Permissions |
| Build | AGP 8.7.3 + Gradle 8.10.2 |

### APIs externas (todas gratuitas)
- **PlantNet** — identificación de plantas por foto
- **Wikipedia REST** — descripción enciclopédica (español + inglés fallback)

---

## 🏗 Arquitectura

```
com.BPO.plantcare/
├── PlantCareApplication.kt        # @HiltAndroidApp, registra canal y arranca scheduler
├── MainActivity.kt                 # @AndroidEntryPoint con Scaffold + bottom nav
├── core/
│   ├── notification/               # NotificationChannel + WateringNotifier
│   ├── storage/                    # PhotoStorage (filesDir/plant_photos) + UriExt
│   ├── widget/                     # Glance AppWidget + Hilt EntryPoint
│   └── work/                       # HiltWorker + Scheduler + Manager
├── data/
│   ├── local/                      # Room DB, DAOs, entities, migrations
│   ├── preferences/                # DataStore impl
│   ├── remote/                     # PlantNet + Wikipedia Retrofit APIs + DTOs
│   └── repository/                 # Implementaciones de los repositorios
├── domain/
│   ├── model/                      # Plant, PlantCareGuide, WateringLog, etc.
│   ├── repository/                 # Interfaces puras (sin Android)
│   └── usecase/                    # Casos de uso por intent
├── di/                             # Modulos Hilt (Network, Database, Repository)
└── ui/
    ├── components/                 # CareGuideCard, WikipediaCard (compartidos)
    ├── navigation/                 # NavHost + bottom bar + destinations
    ├── screens/
    │   ├── home/                   # CTA principal
    │   ├── identify/               # Cámara + galería + resultados
    │   ├── myplants/               # Grid 2-col con badges
    │   ├── plantdetail/            # Ficha completa
    │   ├── calendar/               # Vista mensual
    │   ├── search/                 # Catálogo con filtros
    │   ├── catalogdetail/          # Ficha de especie del catálogo
    │   ├── photoviewer/            # Visor fullscreen con zoom
    │   ├── profile/                # Ajustes de notificaciones
    │   └── common/                 # State holders compartidos
    └── theme/                      # Colores + tipografía + tema M3
```

---

## ⚙️ Configuración local

1. Clona el repo:
   ```bash
   git clone https://github.com/DanielChineaDev/PlantCare.git
   ```
2. Abre el proyecto en **Android Studio Narwhal** (2025.3.4 Patch 1 o superior).
3. Edita `local.properties` y añade tu API key de PlantNet:
   ```properties
   sdk.dir=...
   PLANTNET_API_KEY=tu_api_key_aqui
   ```
   Consigue una key gratuita (500 IDs/día) en [my.plantnet.org](https://my.plantnet.org).
4. **Firebase** (necesario para Fase 3 — social):
   - Crea un proyecto en [Firebase Console](https://console.firebase.google.com)
   - Añade una app Android con package `com.BPO.plantcare`
   - Habilita **Authentication → Google** y **Firestore Database** (eur3 si España)
   - Descarga `google-services.json` y colócalo en `app/`
   - Habilita **Storage** (modo test inicialmente)
5. **Sync** y **Run** sobre un dispositivo Android 8.0+ (API 26+).

---

## 🔒 Reglas de seguridad de Firebase

Los archivos `firestore.rules` y `storage.rules` en la raíz del repo contienen las reglas reales para producción. **No se despliegan automáticamente** desde el código de la app — hay que aplicarlas en la consola de Firebase.

### Despliegue manual (recomendado)

**Firestore:**
1. Firebase Console → **Firestore Database** → pestaña **Rules**
2. Reemplaza el contenido por el de `firestore.rules` del repo
3. Publicar

**Storage:**
1. Firebase Console → **Storage** → pestaña **Rules**
2. Reemplaza el contenido por el de `storage.rules` del repo
3. Publicar

### Despliegue automático con Firebase CLI (opcional)

```bash
npm install -g firebase-tools
firebase login
firebase init   # marcar Firestore + Storage, usar los .rules existentes
firebase deploy --only firestore:rules,storage
```

---

## ☁️ Cloud Functions (FCM push para mensajes nuevos)

La carpeta `functions/` contiene una Cloud Function en Node.js que:

- Se dispara cada vez que se crea un documento en `conversations/{cid}/messages/{mid}`.
- Lee los participantes de la conversación, busca los tokens FCM del destinatario (`users/{uid}/fcmTokens/*`) y le envía un push.
- Limpia automáticamente los tokens inválidos (dispositivos desinstalados).

### Requisitos

- El proyecto Firebase tiene que estar en **plan Blaze (pago por uso)**. Las Cloud Functions ya no funcionan en Spark/gratis. El tier gratuito de Blaze cubre ~125 K invocaciones/mes — más que suficiente para un proyecto pequeño.
- Node.js 20 instalado localmente.

### Despliegue

```bash
cd functions
npm install
cd ..
firebase deploy --only functions
```

La primera vez Firebase pedirá habilitar las APIs necesarias (Cloud Build, Artifact Registry, etc.). Acepta y vuelve a lanzar el deploy si hace falta.

### Cómo verificar que funciona

1. Inicia sesión con dos cuentas distintas en dos dispositivos.
2. Cierra la app en el dispositivo destinatario.
3. Envía un mensaje desde el otro dispositivo.
4. Debería aparecer una notificación con el nombre y el texto. Al tocarla, abre directamente el chat con ese usuario.

Los logs de la función están en `firebase functions:log` o en la consola de Firebase → **Functions → Logs**.

### Qué garantizan las reglas

| Recurso | Lectura | Escritura |
|---|---|---|
| `users/{uid}` | Cualquier logueado (para mostrar avatares/nombres en posts) | Solo el dueño |
| `users/{uid}/joinedCommunities`, `postLikes` | Solo el dueño | Solo el dueño |
| `communities/{id}` | Público | Crear: cualquier logueado. Update: solo contador `memberCount` ±1. Borrar: nadie |
| `communities/{c}/members/{uid}` | Cualquier logueado | Solo el propio usuario |
| `communities/{c}/posts/{id}` | Público | Crear con `authorUid = caller`. Update: solo contadores ±1. Borrar: solo autor |
| `communities/{c}/posts/{p}/likes/{uid}` | Público | Solo el propio usuario |
| `communities/{c}/posts/{p}/comments/{id}` | Público | Crear con `authorUid = caller`. Borrar: solo autor. Editar: nadie |
| `community_posts/{c}/*.jpg` en Storage | Público | Logueado, máx 5MB, content-type imagen |
| `conversations/{id}` | Solo los 2 participantes | Solo los 2 participantes |
| `conversations/{id}/messages/{mid}` | Solo participantes (vía `get()` del doc padre) | Crear con `senderUid = caller`. Editar/borrar: nadie |
| `users/{uid}/publicPlants/{plantId}` | Público | Solo el dueño |
| `users/{uid}/fcmTokens/{token}` | **Nadie** (solo Admin SDK del backend) | Solo el dueño |
| Cualquier otro path | Denegado | Denegado |

Defensa en profundidad: el código de los repositorios ya valida muchas de estas mismas reglas (p. ej. `deleteComment` lanza error si el caller no es el autor), pero las **reglas son la barrera final** que protege contra clientes maliciosos o bugs.

---

## 🚀 Roadmap

### ✅ Fase 1 — MVP completo
- Identificación con PlantNet (cámara y galería)
- Mis plantas con Room + estados visuales
- Ficha de planta con Wikipedia + cuidados (catálogo de 30 especies con match por género)
- Calendario funcional con historial completo de riegos
- Recordatorios diarios configurables con WorkManager
- Diario fotográfico con visor fullscreen
- Widget de pantalla de inicio con Glance
- UI/UX pulida: transiciones, badges, undo, empty states

### 🔄 Fase 2 — En consideración
- Catálogo ampliado a 60+ especies
- Detección de plagas/enfermedades por foto
- Integración con clima (saltar riego si llovió)
- Sensor de luz para verificar si un sitio es bueno para una planta
- Modo "estoy de viaje" para pausar/compartir recordatorios

### ✅ Fase 3 — Social
- Login con Google + perfil en Firebase
- Comunidades / foros temáticos con posts, likes y comentarios
- Chat 1-a-1 entre usuarios (DM en tiempo real)
- Perfiles públicos con colecciones compartibles
- **Notificaciones push (FCM)** para mensajes nuevos vía Cloud Functions

### 💰 Fase 4 — Monetización + iOS
- Freemium con RevenueCat (Android + iOS unificado)
- Versión iOS en SwiftUI compartiendo backend

---

## 📜 Licencia

Proyecto personal de [@DanielChineaDev](https://github.com/DanielChineaDev). Sin licencia pública por el momento.
