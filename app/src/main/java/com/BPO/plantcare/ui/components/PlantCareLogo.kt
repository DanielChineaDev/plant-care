package com.BPO.plantcare.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.BPO.plantcare.R

/**
 * Marca grafica de PlantCare. Por defecto incluye logo + texto debajo.
 * Pasando [showText] = false sirve para situaciones compactas (drawer
 * header donde el avatar del user va al lado).
 */
@Composable
fun PlantCareLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    showText: Boolean = true,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_plantcare_logo),
            contentDescription = "PlantCare",
            modifier = Modifier.size(size),
        )
        if (showText) {
            Text(
                text = "PlantCare",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
