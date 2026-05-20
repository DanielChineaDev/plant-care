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
 * Insignia/logro. [unlocked] indica si el usuario ya lo ha conseguido segun
 * sus estadisticas.
 */
data class Achievement(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val unlocked: Boolean,
)

/**
 * Calcula la lista de logros (desbloqueados o no) a partir de las
 * estadisticas del usuario. La definicion vive en el dominio para que la
 * UI solo tenga que pintarla.
 */
fun ProfileStats.achievements(): List<Achievement> = listOf(
    Achievement(
        id = "first_plant",
        emoji = "🌱",
        title = "Primera planta",
        description = "Anade tu primera planta",
        unlocked = plantCount >= 1,
    ),
    Achievement(
        id = "collector",
        emoji = "🪴",
        title = "Coleccionista",
        description = "Ten 10 plantas",
        unlocked = plantCount >= 10,
    ),
    Achievement(
        id = "watering_100",
        emoji = "💧",
        title = "100 riegos",
        description = "Registra 100 riegos",
        unlocked = totalWaterings >= 100,
    ),
    Achievement(
        id = "streak_7",
        emoji = "🔥",
        title = "Racha de 7 dias",
        description = "Riega 7 dias seguidos",
        unlocked = wateringStreak >= 7,
    ),
    Achievement(
        id = "first_post",
        emoji = "✍️",
        title = "Primer post",
        description = "Publica en una comunidad",
        unlocked = postCount >= 1,
    ),
    Achievement(
        id = "conversador",
        emoji = "💬",
        title = "Conversador",
        description = "Escribe 25 comentarios",
        unlocked = commentCount >= 25,
    ),
    Achievement(
        id = "querido",
        emoji = "❤️",
        title = "Querido",
        description = "Consigue 50 de karma",
        unlocked = karma >= 50,
    ),
    Achievement(
        id = "veterano",
        emoji = "🏆",
        title = "Veterano",
        description = "Lleva un ano en PlantCare",
        unlocked = memberSinceDays >= 365,
    ),
)
