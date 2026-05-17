package com.BPO.plantcare.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.BPO.plantcare.domain.model.CalendarEvent
import com.BPO.plantcare.domain.model.CalendarEventType
import com.BPO.plantcare.ui.theme.StatusHealthy
import com.BPO.plantcare.ui.theme.StatusThirsty
import com.BPO.plantcare.ui.theme.StatusWarning
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }

    // Importante: HorizontalCalendar de Kizitonwose NO se puede meter dentro
    // de un Column.verticalScroll (constraints de altura infinita -> crash).
    // Usamos LazyColumn como contenedor scrollable seguro.
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            TodayTasksCard(
                tasks = events[today].orEmpty().filter { it.type == CalendarEventType.WateringDue },
                onWatered = viewModel::onWatered,
            )
        }
        item {
            MonthCalendar(
                events = events,
                selectedDate = selectedDate,
                today = today,
                onDateSelected = { selectedDate = it },
            )
        }
        item {
            DayEventsCard(
                date = selectedDate,
                events = events[selectedDate].orEmpty(),
                onWatered = viewModel::onWatered,
            )
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun TodayTasksCard(
    tasks: List<CalendarEvent>,
    onWatered: (Long) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (tasks.isEmpty())
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.tertiaryContainer,
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Tareas de hoy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (tasks.isEmpty()) {
                Text(
                    text = "✨ No hay riegos pendientes hoy. ¡Buen trabajo!",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                tasks.forEach { event ->
                    EventRow(
                        event = event,
                        onWatered = { onWatered(event.plant.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthCalendar(
    events: Map<LocalDate, List<CalendarEvent>>,
    selectedDate: LocalDate,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
) {
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(12) }
    val endMonth = remember { currentMonth.plusMonths(12) }
    val daysOfWeek = remember { daysOfWeek(firstDayOfWeek = firstDayOfWeekFromLocale()) }

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = daysOfWeek.first(),
    )
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            MonthHeader(
                yearMonth = state.firstVisibleMonth.yearMonth,
                onPrev = { scope.launch { state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.minusMonths(1)) } },
                onNext = { scope.launch { state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.plusMonths(1)) } },
                onToday = {
                    onDateSelected(today)
                    scope.launch { state.animateScrollToMonth(YearMonth.now()) }
                },
            )
            DaysOfWeekHeader(daysOfWeek)
            HorizontalCalendar(
                state = state,
                dayContent = { day ->
                    Day(
                        day = day,
                        isSelected = day.date == selectedDate,
                        isToday = day.date == today,
                        eventCount = events[day.date]?.size ?: 0,
                        onClick = { if (day.position == DayPosition.MonthDate) onDateSelected(day.date) },
                    )
                },
            )
        }
    }
}

@Composable
private fun MonthHeader(
    yearMonth: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Outlined.ChevronLeft, contentDescription = "Mes anterior")
        }
        Text(
            text = yearMonth.month
                .getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
                .replaceFirstChar { it.titlecase(Locale.getDefault()) } + " " + yearMonth.year,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = "Mes siguiente")
        }
        TextButton(onClick = onToday) { Text("Hoy") }
    }
}

@Composable
private fun DaysOfWeekHeader(daysOfWeek: List<java.time.DayOfWeek>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        daysOfWeek.forEach { dow ->
            Text(
                text = dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Day(
    day: CalendarDay,
    isSelected: Boolean,
    isToday: Boolean,
    eventCount: Int,
    onClick: () -> Unit,
) {
    val inMonth = day.position == DayPosition.MonthDate
    Box(
        modifier = Modifier
            .padding(2.dp)
            .height(44.dp)
            .fillMaxWidth(1f / 7f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.primaryContainer
                        else -> Color.Transparent
                    }
                )
                .let { if (inMonth) it.clickable(onClick = onClick) else it },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
                if (inMonth && eventCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.primary
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayEventsCard(
    date: LocalDate,
    events: List<CalendarEvent>,
    onWatered: (Long) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = formatDate(date),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (events.isEmpty()) {
                Text(
                    text = "Sin eventos este dia.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                events.forEach { event ->
                    EventRow(event = event, onWatered = { onWatered(event.plant.id) })
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: CalendarEvent, onWatered: () -> Unit) {
    val today = remember { LocalDate.now() }
    val (icon, tint, label) = when (event.type) {
        CalendarEventType.Watered -> Triple(Icons.Outlined.WaterDrop, StatusHealthy, "Regada")
        CalendarEventType.WateringDue -> {
            val isOverdue = event.date.isBefore(today)
            val color = if (isOverdue) StatusThirsty else StatusWarning
            Triple(Icons.Outlined.LocalFlorist, color, if (isOverdue) "Atrasada" else "Toca regar")
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.plant.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (event.type == CalendarEventType.WateringDue && !event.date.isAfter(today)) {
            IconButton(onClick = onWatered) {
                Icon(
                    Icons.Outlined.WaterDrop,
                    contentDescription = "Marcar como regada",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun formatDate(date: LocalDate): String {
    val locale = Locale.getDefault()
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL_STANDALONE, locale)
        .replaceFirstChar { it.titlecase(locale) }
    val monthName = date.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
        .replaceFirstChar { it.titlecase(locale) }
    return "$dayName ${date.dayOfMonth} de $monthName"
}
