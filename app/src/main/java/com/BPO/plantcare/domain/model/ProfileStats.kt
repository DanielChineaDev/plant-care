package com.BPO.plantcare.domain.model

/**
 * Estadisticas agregadas del usuario para mostrar en su perfil.
 */
data class ProfileStats(
    val plantCount: Int = 0,
    val postCount: Int = 0,
    val commentCount: Int = 0,
    val totalWaterings: Int = 0,
    /** Antiguedad de la cuenta en dias. */
    val memberSinceDays: Int = 0,
    /** Dias consecutivos (terminando hoy o ayer) con al menos un riego. */
    val wateringStreak: Int = 0,
    val karma: Long = 0,
)

/**
 * Definicion de un logro: como conseguirlo. [unlock] evalua si las
 * estadisticas dadas lo desbloquean.
 */
data class AchievementDef(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val unlock: (ProfileStats) -> Boolean,
)

/**
 * Logro listo para pintar. [unlocked] indica si esta conseguido y
 * [unlockedAt] cuando se consiguio (epoch millis, 0 si desconocido).
 */
data class Achievement(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val unlocked: Boolean,
    val unlockedAt: Long = 0L,
)

/** Catalogo completo de logros disponibles en la app. */
val ACHIEVEMENTS: List<AchievementDef> = listOf(
    AchievementDef("first_plant", "🌱", "Primera planta", "Añade tu primera planta") {
        it.plantCount >= 1
    },
    AchievementDef("collector", "🪴", "Coleccionista", "Ten 10 plantas") {
        it.plantCount >= 10
    },
    AchievementDef("collector_pro", "🌳", "Jardín en casa", "Ten 25 plantas") {
        it.plantCount >= 25
    },
    AchievementDef("watering_100", "💧", "100 riegos", "Registra 100 riegos") {
        it.totalWaterings >= 100
    },
    AchievementDef("watering_500", "🌊", "500 riegos", "Registra 500 riegos") {
        it.totalWaterings >= 500
    },
    AchievementDef("streak_7", "🔥", "Racha de 7 días", "Riega 7 días seguidos") {
        it.wateringStreak >= 7
    },
    AchievementDef("streak_30", "⚡", "Racha de 30 días", "Riega 30 días seguidos") {
        it.wateringStreak >= 30
    },
    AchievementDef("first_post", "✍️", "Primer post", "Publica en una comunidad") {
        it.postCount >= 1
    },
    AchievementDef("conversador", "💬", "Conversador", "Escribe 25 comentarios") {
        it.commentCount >= 25
    },
    AchievementDef("comentarista", "🗣️", "Comentarista", "Escribe 100 comentarios") {
        it.commentCount >= 100
    },
    AchievementDef("querido", "❤️", "Querido", "Consigue 50 de karma") {
        it.karma >= 50
    },
    AchievementDef("influencer", "⭐", "Influencer", "Consigue 200 de karma") {
        it.karma >= 200
    },
    AchievementDef("veterano", "🏆", "Veterano", "Lleva un año en PlantCare") {
        it.memberSinceDays >= 365
    },
)

/**
 * Logros del usuario calculados desde sus estadisticas (perfil propio).
 * [dates] aporta la fecha de desbloqueo si se conoce.
 */
fun ProfileStats.achievements(dates: Map<String, Long> = emptyMap()): List<Achievement> =
    ACHIEVEMENTS.map { def ->
        Achievement(
            id = def.id,
            emoji = def.emoji,
            title = def.title,
            description = def.description,
            unlocked = def.unlock(this),
            unlockedAt = dates[def.id] ?: 0L,
        )
    }

/**
 * Logros de OTRO usuario: no tenemos sus estadisticas, asi que el estado
 * "desbloqueado" se deduce de los registros guardados ([dates]).
 */
fun achievementsFromDates(dates: Map<String, Long>): List<Achievement> =
    ACHIEVEMENTS.map { def ->
        Achievement(
            id = def.id,
            emoji = def.emoji,
            title = def.title,
            description = def.description,
            unlocked = dates.containsKey(def.id),
            unlockedAt = dates[def.id] ?: 0L,
        )
    }
