# 🌱 PlantCare

App Android para identificar plantas por foto, llevar el cuidado de tu colección, descubrir nuevas especies y conectar con una comunidad de aficionados a las plantas.

> **Estado:** MVP funcional + Fase 3 social completa, en desarrollo activo.
> **Repo:** [github.com/DanielChineaDev/plant-care](https://github.com/DanielChineaDev/plant-care)

---

## ✨ Features

### 🔍 Identificación
- Identificación por foto con **PlantNet API** (gratis, 500 IDs/día).
- Captura con cámara (CameraX) o galería del sistema (Photo Picker moderno).
- Sugerencias con thumbnail, nombre común, científico, familia y barra de confianza.
- Botón "Añadir a mis plantas" desde cada sugerencia.

### 🪴 Plantas (antes "Mis plantas")
- Persistencia local con **Room** (migraciones manuales reales, sin destructive fallback).
- Grid 2-col con foto, nombre, próximo riego e **indicador de estado**:
  - 😊 Feliz · 😐 Atenta · 🥵 Sedienta · 🌱 Sin regar aún
- **Buscador** por nombre común o científico.
- **Filtros** rápidos: Todas / Necesitan atención / Sanas / Sin regar.
- Botón rápido de gota para marcar como regada desde la tarjeta.
- **Snackbar "Deshacer"** al borrar un riego del historial.

### 📋 Ficha de planta
- Hero con la foto del usuario o referencia de PlantNet.
- Edición de alias con dialog.
- Cambio de foto principal desde cámara o galería.
- **Cuidados** (luz, humedad, riego, sustrato, abono, trasplante, toxicidad para mascotas, curiosidad) cuando la especie está en el catálogo.
- **Match por género**: si tu especie no está en el catálogo pero hay otra del mismo género, muestra los cuidados con un aviso de "datos aproximados".
- **Diario fotográfico** con thumbnails horizontales y visor fullscreen (zoom + swipe).
- **Historial de riegos** completo con timeline y opción de borrar entradas erróneas.
- **Sobre esta planta**: descripción cargada de Wikipedia (español → fallback inglés) con thumbnail y enlace al artículo.
- Eliminar planta con confirmación (borra también fotos y logs en cascada).

### 📅 Calendario
- Vista mensual con **Kizitonwose Calendar Compose**.
- Card "Tareas de hoy" arriba con lista de riegos pendientes.
- Puntos verdes en cada día con eventos.
- Tap en un día → eventos detallados (regados pasados / pendientes / atrasados).

### 🔎 Buscador (catálogo de especies)
- Catálogo navegable con búsqueda por texto.
- Filtros: ubicación (interior/exterior), dificultad, nivel de luz.
- Ficha de catálogo con foto de Wikipedia + cuidados + "Añadir a mis plantas".

### 🔔 Recordatorios de riego
- Notificación diaria con WorkManager listando las plantas que toca regar hoy.
- **Hora configurable** desde Configuración (dropdown 00:00..23:00).
- Switch ON/OFF para activar/desactivar.
- Botón "Probar notificación" para verificar sin esperar.
- Al cambiar la hora se aplica **ya hoy** (CANCEL_AND_REENQUEUE en lugar de UPDATE).

### 🏠 Widget de pantalla de inicio
- Widget Glance que muestra "Hoy toca regar: X plantas" + lista.
- Refresco al instante cuando riegas / añades / borras desde la app.
- Refresco automático cada 30 min por el sistema y a la hora del recordatorio.
- Toque → abre la app.

### 🌦 Clima
- Integración con **Open-Meteo** (gratis, sin API key).
- Si llovió ≥5 mm en las últimas 24h en tu zona, las plantas marcadas como exterior se omiten del recordatorio.
- Permisos de ubicación tras toggle explícito.

### ✈ Modo viaje
- Pausa de notificaciones durante un rango de fechas que elijas.

### 💡 Medidor de luz
- Usa el sensor de luz ambiental del móvil para evaluar si una zona tiene la luz adecuada para una planta del catálogo.

### 🩺 Diagnóstico
- Catálogo de 16 plagas y enfermedades comunes con síntomas y tratamientos.

### 🔐 Autenticación (gate obligatorio)
- Splash → Login / Registro **antes** de poder entrar a la app.
- **Email + contraseña** y **Google Sign-In** (Credentials Manager moderno).
- Recuperación de contraseña por email.
- Cierre de sesión desde el drawer; el gate detecta `SignedOut` y vuelve a Login solo.

### 👤 Perfil y edición
- **Mi perfil** muestra tu vista pública (lo que ven los demás) con tus plantas.
- **Editar perfil**: cambiar foto (subida a Firebase Storage `avatars/{uid}/`), nombre, contraseña con confirmación.
- Coleccionables públicos: switch para hacer tu colección visible y botón de resincronización.

### 👥 Comunidades
- Comunidades temáticas con descripción, emoji y **foto de portada** opcional.
- Solo **administradores** pueden crear comunidades (controlado por `users/{uid}/isAdmin` en Firestore).
- Lista con **carrusel de "Comunidades populares"** (tarjetas de tamaño uniforme) + lista "Otras comunidades".
- Bloque "Publicaciones destacadas" con los posts con más engagement de todas las comunidades.
- Unirse / salir con contadores de miembros que se actualizan en tiempo real (transacciones Firestore).
- Posts con texto + foto opcional (Storage), likes y comentarios anidados.

### 💬 Chat 1-a-1
- DMs en tiempo real con `addSnapshotListener`.
- ID determinístico de conversación: `sorted([uidA, uidB]).joinToString("_")`.
- Avatar y nombre del otro usuario en la cabecera, scroll automático al fondo cuando llega mensaje nuevo.

### 🔔 Push notifications (FCM)
- Cloud Function (Node.js v2) que se dispara al crear un mensaje y envía multicast.
- Token FCM registrado por dispositivo en `users/{uid}/fcmTokens` y borrado + invalidado a nivel servicio al cerrar sesión (evita filtración entre usuarios del mismo device).
- Deep link: al tocar la notificación se abre el chat correspondiente.

### 🧭 Navegación
- **Bottom nav** de 5 pestañas: Inicio · Plantas · Comunidad · Buscar · Mensajes.
- **Drawer lateral** con: Mi perfil, Calendario, Identificar planta, Herramientas, tus comunidades unidas, Configuración y Cerrar sesión.
- Badge en Plantas: **punto** discreto cuando hay plantas que necesitan atención (sin número).
- Bottom nav siempre visible y gesto del drawer activo en cualquier pantalla (excepto visor de fotos).

### 🎨 UI/UX
- Material 3 con paleta vegetal (light + dark): verde hoja, marrón tierra, dorado sol.
- Transiciones suaves entre pantallas (cross-fade tabs, slide horizontal detalles).
- Hero CTAs en empty states.
- Coil 3 con engine OkHttp explícito + User-Agent (Wikipedia bloquea peticiones sin él).

---

## 🛠 Stack técnico

| Área | Tecnología |
|------|-----------|
| Lenguaje | **Kotlin 2.0.21** |
| UI | **Jetpack Compose** + Material 3 (BOM 2024.10.01) |
| Arquitectura | MVVM + Clean Architecture (data / domain / ui) |
| DI | **Hilt** 2.52 (KSP) |
| Navegación | Navigation Compose 2.8 |
| Persistencia | **Room** 2.6 (migraciones manuales) + DataStore Preferences |
| Red | Retrofit + OkHttp + Kotlinx Serialization |
| Imágenes | **Coil 3** (con engine OkHttp custom) |
| Cámara | CameraX 1.4 |
| Background | **WorkManager** 2.10 + Hilt-Work |
| Widget | **Glance** 1.1 (AppWidget + Material3) |
| Calendario | Kizitonwose Calendar Compose 2.6.2 |
| Permisos | Accompanist Permissions |
| Backend | **Firebase BOM 33.7.0**: Auth, Firestore, Storage, Messaging + Cloud Functions v2 (Node 20, europe-west1) |
| Auth | Credentials Manager + Google ID + email/password |
| Build | AGP 8.7.3 + Gradle 8.10.2 |

### APIs externas
- **PlantNet** — identificación de plantas (gratis, 500 IDs/día).
- **Wikipedia REST** — descripción enciclopédica (español + inglés fallback).
- **Open-Meteo** — clima (sin API key).

---

## 🏗 Arquitectura

```
com.BPO.plantcare/
├── PlantCareApplication.kt        # @HiltAndroidApp + SingletonImageLoader (Coil 3 engine)
├── MainActivity.kt                # @AndroidEntryPoint, gate AuthState, drawer + bottom nav
├── core/
│   ├── notification/              # WateringNotifier + FcmService
│   ├── storage/                   # PhotoStorage + UriExt
│   ├── widget/                    # Glance AppWidget + Hilt EntryPoint
│   └── work/                      # HiltWorker + Scheduler + Manager
├── data/
│   ├── local/                     # Room DB, DAOs, entities, migrations
│   ├── preferences/               # DataStore impl
│   ├── remote/                    # PlantNet + Wikipedia + Open-Meteo APIs + DTOs
│   └── repository/                # AuthFlow.uidFlow() + implementaciones
├── domain/
│   ├── model/                     # Plant, UserProfile (isAdmin), Community (photoUrl), etc.
│   ├── repository/                # Interfaces puras (sin Android)
│   └── usecase/                   # Casos de uso por intent
├── di/                            # Modulos Hilt (Network, Database, Firebase, Repository)
└── ui/
    ├── auth/                      # AuthGateViewModel (gate raiz)
    ├── components/                # FeedPostCard, DrawerActionButton, etc.
    ├── navigation/                # NavHost + bottom bar + drawer
    └── screens/
        ├── auth/                  # Login / registro (email + Google)
        ├── home/                  # Feed agregado + recientes
        ├── myplants/              # Grid con buscador y filtros
        ├── plantdetail/           # Ficha completa
        ├── calendar/              # Vista mensual
        ├── search/                # Catalogo con filtros
        ├── catalogdetail/         # Ficha de especie
        ├── identify/              # Camara + galeria + resultados
        ├── photoviewer/           # Visor fullscreen con zoom
        ├── communities/           # Carrusel + lista + featured
        ├── communityfeed/         # Posts de una comunidad
        ├── postdetail/            # Post + comentarios
        ├── chatslist/             # Lista de conversaciones
        ├── chat/                  # Mensajes 1-a-1
        ├── myprofile/             # Vista propia + "Editar perfil"
        ├── editprofile/           # Foto + nombre + cambio contrasena
        ├── publicprofile/         # Perfil publico de otro usuario
        ├── tools/                 # Atajos a medidor de luz y diagnostico
        ├── lightmeter/            # Sensor de luz
        ├── diagnosis/             # Catalogo de plagas
        └── profile/               # "Configuracion": notif, viaje, clima, publico, logout
```

---

## ⚙️ Configuración local

1. Clona el repo:
   ```bash
   git clone https://github.com/DanielChineaDev/plant-care.git
   ```
2. Abre el proyecto en **Android Studio Narwhal** (2025.3.4 Patch 1 o superior).
3. Edita `local.properties` y añade tu API key de PlantNet:
   ```properties
   sdk.dir=...
   PLANTNET_API_KEY=tu_api_key_aqui
   ```
   Consigue una key gratuita (500 IDs/día) en [my.plantnet.org](https://my.plantnet.org).
4. **Firebase** (necesario para todo lo social):
   - Crea un proyecto en [Firebase Console](https://console.firebase.google.com).
   - Añade una app Android con package `com.BPO.plantcare`.
   - Habilita **Authentication → Email/Password Y Google**.
   - Habilita **Firestore Database** (región `eur3` si estás en Europa).
   - Habilita **Storage**.
   - Descarga `google-services.json` y colócalo en `app/`.
5. **Sync** y **Run** sobre un dispositivo Android 8.0+ (API 26+).

---

## 🔒 Reglas de seguridad de Firebase

Los archivos `firestore.rules` y `storage.rules` en la raíz del repo contienen las reglas reales para producción.

### Despliegue con Firebase CLI (recomendado)

```bash
npm install -g firebase-tools
firebase login
firebase deploy --only firestore:rules,storage
```

(El proyecto ya tiene `firebase.json` y `.firebaserc` configurados apuntando a `plant-care-fc702`.)

### Despliegue manual desde la consola

**Firestore:** Console → Firestore Database → Rules → pegar `firestore.rules` → Publicar.
**Storage:** Console → Storage → Rules → pegar `storage.rules` → Publicar.

### Qué garantizan las reglas

| Recurso | Lectura | Escritura |
|---|---|---|
| `users/{uid}` | Cualquier logueado | Solo el dueño. **`isAdmin` NO puede modificarse desde la app** (validado en update). |
| `users/{uid}/joinedCommunities`, `postLikes` | Solo el dueño | Solo el dueño |
| `users/{uid}/publicPlants/{plantId}` | Público | Solo el dueño |
| `users/{uid}/fcmTokens/{token}` | **Nadie** (solo Admin SDK del backend) | Solo el dueño |
| `communities/{id}` | Público | Crear: **solo admins** (`isAdmin == true` en Firestore). Update: solo contador `memberCount` ±1. Borrar: nadie. |
| `communities/{c}/members/{uid}` | Cualquier logueado | Solo el propio usuario |
| `communities/{c}/posts/{id}` | Público | Crear con `authorUid = caller`. Update: solo contadores ±1. Borrar: solo autor |
| `communities/{c}/posts/{p}/likes/{uid}` | Público | Solo el propio usuario |
| `communities/{c}/posts/{p}/comments/{id}` | Público | Crear con `authorUid = caller`. Borrar: solo autor. Editar: nadie |
| `conversations/{id}` | Solo los 2 participantes | Solo los 2 participantes |
| `conversations/{id}/messages/{mid}` | Solo participantes (vía `get()` del doc padre) | Crear con `senderUid = caller`. Editar/borrar: nadie |
| `community_posts/{c}/*.jpg` en Storage | Público | Logueado, máx 5 MB, content-type imagen |
| `communities/{c}/cover.jpg` en Storage | Público | Logueado, máx 5 MB, content-type imagen |
| `avatars/{uid}/*.jpg` en Storage | Público | Solo el dueño, máx 2 MB, content-type imagen |
| Cualquier otro path | Denegado | Denegado |

### Asignar admin a un usuario

Solo desde Firebase Console:
1. Firestore Database → `users` → documento del usuario.
2. Añadir / editar campo `isAdmin` = `true` (boolean).

A partir de ese momento ese usuario verá el FAB de "+" en Comunidad y podrá crear nuevas.

---

## ☁️ Cloud Functions (FCM push para mensajes nuevos)

La carpeta `functions/` contiene una Cloud Function en Node.js que:

- Se dispara cada vez que se crea un documento en `conversations/{cid}/messages/{mid}`.
- Lee los participantes, obtiene los tokens FCM del destinatario y envía un push.
- Limpia automáticamente los tokens inválidos (apps desinstaladas).
- Región: `europe-west1` (colocada con Firestore `eur3` para minimizar latencia).

### Requisitos

- Plan **Blaze** (pago por uso). El tier gratuito de Blaze cubre ~125 K invocaciones/mes — suficiente para un proyecto pequeño.
- Node.js 20.

### Despliegue

```bash
cd functions
npm install
cd ..
firebase deploy --only functions
```

### Cómo verificar

1. Inicia sesión con dos cuentas distintas en dos dispositivos.
2. Cierra la app en el destinatario.
3. Envía un mensaje desde el otro.
4. Debería aparecer notificación con nombre + texto. Al tocarla, abre el chat.

Logs: `firebase functions:log --only onChatMessageCreated` o Console → Functions → Logs.

---

## 🚀 Roadmap

### ✅ Fase 1 — MVP completo
- Identificación con PlantNet (cámara y galería).
- Mis plantas con Room + estados visuales.
- Ficha de planta con Wikipedia + cuidados (catálogo de 30 especies con match por género).
- Calendario funcional con historial completo de riegos.
- Recordatorios diarios configurables con WorkManager.
- Diario fotográfico con visor fullscreen.
- Widget de pantalla de inicio con Glance.

### ✅ Fase 2 — Mejoras de cuidado
- Catálogo de plagas/enfermedades.
- Sensor de luz para verificar si un sitio es bueno para una planta.
- Integración con clima (saltar riego si llovió).
- Modo "estoy de viaje" para pausar recordatorios.

### ✅ Fase 3 — Social
- Login con Google + email/contraseña (gate obligatorio).
- Comunidades / foros temáticos con posts, likes y comentarios.
- Chat 1-a-1 entre usuarios (DM en tiempo real).
- Perfiles públicos con colecciones compartibles.
- Notificaciones push (FCM) para mensajes nuevos vía Cloud Functions.
- Sistema de admin para creación de comunidades.

### ✅ Rediseño UI
- Bottom nav de 5 pestañas + drawer lateral.
- Inicio = feed agregado de las comunidades unidas con filtro últimas/destacadas.
- Comunidad rediseñada con carrusel de populares (tarjetas uniformes con foto).
- Buscador + filtros en Plantas.
- Editar perfil (foto, nombre, contraseña).

### 🔮 Próximas ideas
Ver [`docs/MEJORAS.md`](docs/MEJORAS.md) para la lista detallada de propuestas (UI + features) inspiradas en apps similares (Planta, PictureThis, Reddit, Discord, Instagram).

### 💰 Fase futura — Monetización + iOS
- Freemium con RevenueCat (Android + iOS unificado).
- Versión iOS en SwiftUI compartiendo backend Firebase.

---

## 📜 Licencia

Proyecto personal de [@DanielChineaDev](https://github.com/DanielChineaDev). Sin licencia pública por el momento.
