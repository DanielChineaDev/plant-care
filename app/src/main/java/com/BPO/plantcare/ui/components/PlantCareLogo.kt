package com.BPO.plantcare.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
        Surface(
            shape = RoundedCornerShape(size * 0.22f),
            color = Color(0xFFF1F3F0),
            shadowElevation = 6.dp,
            modifier = Modifier.size(size),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_plantcare_logo),
                contentDescription = "PlantCare",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
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
