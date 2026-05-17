package com.BPO.plantcare.core.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.BPO.plantcare.MainActivity
import com.BPO.plantcare.domain.model.Plant
import com.BPO.plantcare.domain.model.needsWatering
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

class WateringWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java,
        )
        val plantsDue = entryPoint.plantRepository()
            .observeAll()
            .first()
            .filter { it.needsWatering() }

        provideContent {
            GlanceTheme {
                WidgetContent(plantsDue)
            }
        }
    }
}

@Composable
private fun WidgetContent(plants: List<Plant>) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.primaryContainer)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<MainActivity>())
            .padding(14.dp),
    ) {
        if (plants.isEmpty()) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "🌱 Sin riegos hoy",
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "Tus plantas estan al dia",
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontSize = 12.sp,
                    ),
                )
            }
        } else {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Text(
                    text = "🌱 Hoy toca regar (${plants.size})",
                    style = TextStyle(
                        color = GlanceTheme.colors.onPrimaryContainer,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                LazyColumn(modifier = GlanceModifier.fillMaxWidth()) {
                    val visible = plants.take(MAX_LINES)
                    items(visible) { plant ->
                        Text(
                            text = "• ${plant.displayName}",
                            style = TextStyle(
                                color = GlanceTheme.colors.onPrimaryContainer,
                                fontSize = 13.sp,
                            ),
                            modifier = GlanceModifier.padding(vertical = 2.dp),
                        )
                    }
                    if (plants.size > MAX_LINES) {
                        item {
                            Text(
                                text = "y ${plants.size - MAX_LINES} mas...",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onPrimaryContainer,
                                    fontSize = 12.sp,
                                ),
                                modifier = GlanceModifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val MAX_LINES = 5
