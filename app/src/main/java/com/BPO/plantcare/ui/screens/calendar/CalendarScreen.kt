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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.BPO.plantcare.R
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.drawer_calendar)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        // Importante: HorizontalCalendar de Kizitonwose NO se puede meter dentro
        // de un Column.verticalScroll (constraints de altura infinita -> crash).
        // Usamos LazyColumn como contenedor scrollable seguro.
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                TodayTasksCard(
                    tasks = events[today].orEmpty().filter {
                        it.type == CalendarEventType.WateringDue ||
                            it.type == CalendarEventType.TaskDue
                    },
                    onWatered = viewModel::onWatered,
                    onTaskDone = viewModel::onTaskDone,
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
                    onTaskDone = viewModel::onTaskDone,
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun TodayTasksCard(
    tasks: List<CalendarEvent>,
    onWatered: (Long) -> Unit,
    onTaskDone: (Long) -> Unit,
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
                text = stringResource(R.string.cal_today_tasks),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (tasks.isEmpty()) {
                Text(
                    text = stringResource(R.string.cal_nothing_today),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                tasks.forEach { event ->
                    EventRow(
                        event = event,
                        onWatered = { onWatered(event.plant.id) },
                        onTaskDone = { event.task?.id?.let(onTaskDone) },
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
            Icon(Icons.Outlined.ChevronLeft, contentDescription = stringResource(R.string.cal_prev_month))
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
            Icon(Icons.Outlined.ChevronRight, contentDescription = stringResource(R.string.cal_next_month))
        }
        TextButton(onClick = onToday) { Text(stringResource(R.string.cal_today)) }
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
    // Cada slot lo posiciona la lib internamente en una grid 7-col;
    // aqui solo necesitamos altura fija. NUNCA usar fillMaxWidth aqui.
    Box(
        modifier = Modifier
            .padding(2.dp)
            .height(44.dp),
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
    onTaskDone: (Long) -> Unit,
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
                    text = stringResource(R.string.cal_no_events),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                events.forEach { event ->
                    EventRow(
                        event = event,
                        onWatered = { onWatered(event.plant.id) },
                        onTaskDone = { event.task?.id?.let(onTaskDone) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EventRow(
    event: CalendarEvent,
    onWatered: () -> Unit,
    onTaskDone: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val (icon, tint, label) = when (event.type) {
        CalendarEventType.Watered -> Triple(Icons.Outlined.WaterDrop, StatusHealthy, stringResource(R.string.cal_watered))
        CalendarEventType.WateringDue -> {
            val isOverdue = event.date.isBefore(today)
            val color = if (isOverdue) StatusThirsty else StatusWarning
            Triple(
                Icons.Outlined.LocalFlorist,
                color,
                if (isOverdue) stringResource(R.string.cal_overdue) else stringResource(R.string.cal_water_due),
            )
        }
        CalendarEventType.TaskDue -> {
            val isOverdue = event.date.isBefore(today)
            val color = if (isOverdue) StatusThirsty else StatusWarning
            val labelText = event.task?.type?.labelRes?.let { stringResource(it) }
                ?: stringResource(R.string.cal_task_fallback)
            Triple(
                Icons.Outlined.LocalFlorist,
                color,
                if (isOverdue) stringResource(R.string.cal_task_overdue, labelText) else labelText,
            )
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
            if (event.type == CalendarEventType.TaskDue) {
                Text(text = event.task?.type?.emoji.orEmpty(), fontSize = 18.sp)
            } else {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
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
        when {
            event.type == CalendarEventType.WateringDue && !event.date.isAfter(today) -> {
                IconButton(onClick = onWatered) {
                    Icon(
                        Icons.Outlined.WaterDrop,
                        contentDescription = stringResource(R.string.cal_mark_watered),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            event.type == CalendarEventType.TaskDue && !event.date.isAfter(today) -> {
                IconButton(onClick = onTaskDone) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = stringResource(R.string.cal_mark_task_done),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun formatDate(date: LocalDate): String {
    val locale = Locale.getDefault()
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL_STANDALONE, locale)
        .replaceFirstChar { it.titlecase(locale) }
    val monthName = date.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
        .replaceFirstChar { it.titlecase(locale) }
    return stringResource(R.string.cal_date_format, dayName, date.dayOfMonth, monthName)
}
