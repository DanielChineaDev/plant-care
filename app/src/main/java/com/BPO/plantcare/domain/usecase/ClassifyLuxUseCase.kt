package com.BPO.plantcare.domain.usecase

import com.BPO.plantcare.domain.model.LightLevel
import javax.inject.Inject

/**
 * Mapea un valor en lux (medido por el sensor) al nivel de luz mas cercano
 * de los que entiende nuestro catalogo de cuidados.
 *
 * Umbrales aproximados (interior tipico):
 *   0..50      -> Poca luz (rincon oscuro, banno sin ventana)
 *   50..500    -> Luz media (habitacion con ventana indirecta)
 *   500..2000  -> Indirecta brillante (al lado de una ventana sin sol directo)
 *   2000..10k  -> Sol directo (algunas horas)
 *   10k+       -> Pleno sol (exterior a mediodia)
 */
class ClassifyLuxUseCase @Inject constructor() {
    operator fun invoke(lux: Float): LightLevel = when {
        lux < 50 -> LightLevel.LOW
        lux < 500 -> LightLevel.MEDIUM
        lux < 2000 -> LightLevel.INDIRECT_BRIGHT
        lux < 10_000 -> LightLevel.DIRECT
        else -> LightLevel.FULL_SUN
    }
}
