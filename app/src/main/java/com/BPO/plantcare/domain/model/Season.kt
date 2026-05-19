package com.BPO.plantcare.domain.model

import java.time.LocalDate
import java.time.Month

/**
 * Estacion del ano segun la fecha y el hemisferio. Usada para ajustar
 * dinamicamente el intervalo de riego: en invierno se riega menos, en
 * verano mas.
 */
enum class Season(val wateringFactor: Double) {
    /** Riega ~15% mas frecuente. */
    Summer(0.85),
    /** Igual que el intervalo base. */
    SpringOrAutumn(1.0),
    /** Riega 50% menos frecuente. */
    Winter(1.5);
}

enum class Hemisphere { Northern, Southern }

/**
 * Calcula la estacion en la que estamos. Por defecto hemisferio norte
 * (donde vive la mayoria de usuarios del MVP); si en el futuro
 * detectamos latitud negativa via la ubicacion del user, pasamos
 * Southern y se invierten primavera/otono y verano/invierno.
 */
fun seasonOf(
    date: LocalDate = LocalDate.now(),
    hemisphere: Hemisphere = Hemisphere.Northern,
): Season {
    val month = date.month
    val northern = when (month) {
        Month.DECEMBER, Month.JANUARY, Month.FEBRUARY -> Season.Winter
        Month.JUNE, Month.JULY, Month.AUGUST -> Season.Summer
        else -> Season.SpringOrAutumn
    }
    return if (hemisphere == Hemisphere.Northern) {
        northern
    } else {
        // En el sur estan invertidas verano e invierno; las intermedias
        // siguen siendo intermedias.
        when (northern) {
            Season.Winter -> Season.Summer
            Season.Summer -> Season.Winter
            Season.SpringOrAutumn -> Season.SpringOrAutumn
        }
    }
}

/**
 * Aplica el factor estacional al intervalo de riego base. El resultado
 * se acota a 1..120 dias para que no se vaya de madre en plantas con
 * intervalos largos en invierno.
 */
fun seasonAdjusted(baseIntervalDays: Int, season: Season = seasonOf()): Int =
    (baseIntervalDays * season.wateringFactor).toInt().coerceIn(1, 120)
