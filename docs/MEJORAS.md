# 🌱 Propuestas de mejora — PlantCare

Lista priorizada de mejoras de UI y funcionalidad inspiradas en apps similares (**Planta**, **PictureThis**, **Greg**, **Plantum**) y en redes sociales con feed/comunidad (**Reddit**, **Discord**, **Instagram**).

Cada item incluye **impacto** (alto/medio/bajo) y **esfuerzo** estimado (S/M/L), para priorizar.

---

## 🔝 Top 10 — Lo que aportaría más en menos tiempo

| # | Mejora | Impacto | Esfuerzo |
|---|---|---|---|
| 1 | **Notificación al recibir mensaje + badge en pestaña Mensajes** | Alto | S |
| 2 | **Tarjeta de cuidado expandible en ficha de planta** (acordeón por sección) | Alto | S |
| 3 | **Tareas más allá del riego**: abonar, trasplantar, podar, fumigar | Alto | M |
| 4 | **"Sugerido para ti"** en Inicio basado en tus plantas | Alto | M |
| 5 | **Detalle de comunidad con cover hero + about + miembros** (como Reddit) | Alto | M |
| 6 | **Búsqueda global** (plantas + comunidades + usuarios) | Alto | M |
| 7 | **Onboarding inicial** de 3 slides + permiso notif + identifica tu primera planta | Medio | S |
| 8 | **Streaks / racha de cuidado** ("llevas 12 días seguidos regando a tiempo") | Medio | S |
| 9 | **Compartir planta como tarjeta** (intent share con imagen generada) | Medio | M |
| 10 | **Modo oscuro reactivo a la hora del día** (auto-switch entre 20:00–07:00) | Bajo | S |

---

## 🎨 Mejoras de UI / UX

### Inicio
- **Stories / Carrusel arriba**: "Plantas que necesitan atención hoy", "Tu agenda de la semana", "Nuevos posts en tus comunidades". Estilo Instagram Stories pero con tarjetas grandes (no circulares).
- **Pull-to-refresh** en el feed con `SwipeRefresh`.
- **Skeleton loaders** (shimmer) mientras carga el feed en lugar de pantalla vacía.
- **Empty state mejorado** cuando no sigues comunidades: mostrar 3-5 comunidades sugeridas en línea ("Únete a Suculentas, Bonsáis, Huerto urbano…").
- **FAB de acceso rápido** para "Identificar planta" superpuesto sobre el feed (común en apps fotográficas).

### Plantas
- **Vista en lista vs grid** (toggle en la TopAppBar). Algunas personas prefieren lista densa para colecciones grandes.
- **Ordenar por**: añadidas reciente, alfabético, próximo riego, estado.
- **Selección múltiple** + acciones en lote (regar varias, borrar varias).
- **Agrupar por ubicación / habitación** (salón, cocina, oficina) — campo nuevo en `Plant`.
- **Detalle de planta con TabRow**: Cuidados / Diario / Historial / Notas, en lugar de scroll vertical infinito.
- **Notas libres** del usuario por planta (un campo `notes` que se muestra como sticky note).

### Comunidad
- **Pantalla de comunidad** (al entrar) con cover hero, tabs: "Publicaciones" / "Miembros" / "Sobre".
- **Tabs en el feed**: "Para ti" (algoritmo: comunidades unidas) / "Recientes" / "Mejor valoradas".
- **Filtros por etiqueta** en posts (categorías: "Mi planta", "Pregunta", "Plaga / SOS", "Antes/después").
- **"Trending now"** — comunidades con más actividad en las últimas 24h.
- **Vista de admin** para gestionar comunidad: editar nombre/desc/portada, expulsar miembros, marcar posts como destacados.

### Mensajes
- **Indicadores de visto** (read receipts) con `readBy: [uid]` en cada mensaje.
- **"Está escribiendo…"** con `typing: { uid: timestamp }` en el doc de conversación.
- **Reacciones a mensajes** (long-press → emoji).
- **Envío de fotos** en chat (ya tenemos Storage).
- **Búsqueda dentro de la conversación**.

### Perfil
- **Editar perfil**: añadir bio (texto corto), localización opcional, "plantas favoritas".
- **Estadísticas**: número de plantas, posts, comentarios, antigüedad, racha de riego.
- **Insignias / Logros**: "Primera planta", "100 riegos a tiempo", "Comunidad fundadora", etc.

### General
- **Animaciones de listas** (Compose `animateItemPlacement`).
- **Haptic feedback** sutil en likes, riegos, navegación importante (`HapticFeedbackType.LongPress`).
- **Mejor soporte de tablets**: pantalla maestro-detalle con `WindowSizeClass`.
- **Theming personalizable**: que el user elija paleta (verde por defecto, pero también ocre, lavanda, etc.).
- **Idiomas**: español ↔ inglés (string resources para los textos hardcoded).

---

## ⚡ Funcionalidades nuevas

### Tareas de cuidado más completas (inspirado en **Planta** y **Greg**)
- Estado actual: solo riego.
- Añadir tipos: **abonar**, **trasplantar**, **podar**, **rotar**, **limpiar hojas**, **fumigar** preventivo.
- Cada tipo con su periodicidad y notificación.
- Vista "Tareas de hoy" mostrando todos los tipos, no solo riegos.

### Recordatorios inteligentes
- **Riego dinámico**: ajustar el periodo según la estación (riego menos en invierno) y el sensor de luz.
- **Snooze** desde la notificación: "Recordar en 1h / mañana".
- **Riego histórico**: gráfica con cadencia real vs sugerida.

### Diagnóstico IA (inspirado en **PictureThis**)
- Botón "Diagnosticar mi planta" en la ficha: foto → análisis con modelo (PlantNet ya identifica especie; añadir Health API o entrenar uno propio para plagas).
- Integración con el catálogo de diagnósticos local (`diagnosis`) para mostrar coincidencias.

### Comunidad
- **Sistema de reputación**: karma estilo Reddit (suma de likes recibidos − reportes).
- **Reportar posts/comentarios** con motivo. Cola de moderación para admins.
- **Encuestas (polls)** en posts: "¿Qué fertilizante usas?".
- **Eventos** dentro de comunidades: "Quedada de aficionados al bonsái — Sábado 15".
- **Markdown / formato rich** en posts (negrita, listas, links).

### Marketplace (futuro grande)
- Sección "Intercambio" para regalar esquejes / vender plantas / pedir.
- Sistema de reseñas vendedor-comprador.

### Plant Hub IoT (futuro muy grande)
- Integración con sensores Bluetooth (Xiaomi Mi Flora, Parrot Flower Power) para leer humedad real del sustrato.
- Esto sí marcaría diferencia frente a Planta.

### Wiki colaborativa
- Que los usuarios puedan **añadir cuidados** para especies que no están en el catálogo (con validación admin).
- Voto de cuidados: "Lo riego cada 7 días", "yo cada 5"; mediana como sugerencia.

### Búsqueda y descubrimiento
- **Búsqueda global** con autocompletado: especies, comunidades, usuarios, posts (Firestore con índices o Algolia).
- **"Quizás te interese"**: comunidades sugeridas según tus plantas (si tienes orquídeas → comunidad "Orquidíarios").

### Notificaciones push
- Like en tu post.
- Comentario en tu post.
- Respuesta a tu comentario.
- Nuevo miembro en tu comunidad (si la creaste).
- Centro de notificaciones dentro de la app (icono campana en TopAppBar global).

### Compartir externo
- Compartir post / planta como **imagen generada** con tu marca PlantCare (parte virtual del marketing).
- Deep links `https://plantcare.app/post/xxx` → Android App Links.

### Backup / sync
- Exportar colección en CSV / JSON.
- Sync automático de plantas locales (Room) a Firestore privado del user, para no perder datos al cambiar de móvil.

---

## ⚙️ Mejoras técnicas / deuda

### Performance
- **Paginación** en posts de comunidad y feed (Paging 3). Hoy cargamos 30-50 por comunidad sin paginar.
- **Image preloading** con Coil para el carrusel.
- **Reducir recomposiciones** marcando data classes con `@Immutable` cuando proceda.
- **Pruebas con `BaselineProfile`** para mejorar el tiempo de arranque tras app update.

### Calidad de código
- **Tests**: actualmente solo hay esqueletos. Añadir:
  - Unit tests de ViewModels con `MainDispatcherRule` y `Turbine` para flows.
  - Repository tests con Firebase Emulator Suite.
  - Compose UI tests para flujos críticos (login → identificar → añadir).
- **Detekt** + **Ktlint** para estilo consistente.
- **Firebase Crashlytics** para capturar crashes en producción.
- **Firebase Analytics / GA4** para entender uso real (qué pantallas, qué flujos abandonan).
- **Migrar Icons.Outlined a `Icons.AutoMirrored.Outlined`** (warnings actuales). Trivial pero pendiente.
- **WorkManager: configuración con `setExpedited`** para el botón "Probar notificación" → más responsivo.

### Cloud Functions
- Función adicional **onLikeCreated** y **onCommentCreated** para notificar al autor del post.
- Función **onMemberJoined** para notificar al creador de la comunidad.
- Función **cleanupOrphanStorage** semanal: barre `community_posts/` y `communities/` y borra blobs cuyo doc Firestore ya no existe.

### Seguridad
- Endurecer reglas Firestore con `request.resource.data.keys().hasOnly([...])` para que el cliente no pueda meter campos arbitrarios (ej. en posts).
- Rate limiting en likes / comments (App Check + reglas).
- App Check con **Play Integrity** para evitar requests desde APKs modificados.

### Offline-first
- **Cachear posts y comunidades** en Room para tener feed offline.
- Mostrar banner "Estás offline — vista cacheada" cuando no hay red.

### Accesibilidad
- Auditar `contentDescription` en iconos.
- Asegurar contrastes WCAG AA.
- Soporte `LiveRegion` para mensajes de estado dinámicos.

---

## 📋 Plan sugerido por sprints

### Sprint 1 (semana) — Pulido fácil de alto impacto
- Onboarding inicial (3 slides).
- Notificación de like/comentario en tus posts.
- Streaks de riego en el header de Plantas.
- Migrar deprecaciones `AutoMirrored`.

### Sprint 2 — Tareas más completas
- Modelo de tareas (TaskType: water, fertilize, prune, repot…).
- UI calendario con todos los tipos.
- Recordatorios separados por tipo.

### Sprint 3 — Comunidad pro
- Pantalla de comunidad con tabs y cover hero.
- Sistema de etiquetas en posts.
- Moderación: reportar + cola para admins.

### Sprint 4 — Discovery + búsqueda
- Búsqueda global.
- "Sugerido para ti" en Inicio.
- Paginación con Paging 3.

### Sprint 5 — Polish técnico
- Tests (VMs, repos, Compose UI).
- Crashlytics + Analytics.
- Baseline Profile.
- App Check + Play Integrity.

---

## 🎯 Cosas que **NO** haría (aún)

- **Marketplace** completo: enorme y requiere KYC, pagos, mensajería protegida. Validar primero con MVP de "intercambio gratuito".
- **Tienda interna de productos** (fertilizantes, macetas…): cambia totalmente el modelo de la app.
- **Streaming en directo** desde la comunidad: muy ambicioso, mejor cuando haya base de usuarios.
- **Reescribir a Compose Multiplatform** para iOS: con Firebase + Hilt + WorkManager + CameraX no sale rentable; mejor SwiftUI nativo en iOS compartiendo solo backend.

---

_Documento mantenido junto al código del proyecto. Última actualización: 2026-05._
