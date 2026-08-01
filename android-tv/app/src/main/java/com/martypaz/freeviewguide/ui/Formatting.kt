package com.martypaz.freeviewguide.ui

import com.martypaz.freeviewguide.data.model.Programme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val dayFormat = DateTimeFormatter.ofPattern("EEE")

/** "Today 21:00", "Tomorrow 09:15", or "Wed 21:00". */
fun formatStart(startMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val dateTime = Instant.ofEpochMilli(startMillis).atZone(zone)
    val day = when (dateTime.toLocalDate()) {
        LocalDate.now(zone) -> "Today"
        LocalDate.now(zone).plusDays(1) -> "Tomorrow"
        else -> dayFormat.format(dateTime)
    }
    return "$day ${timeFormat.format(dateTime)}"
}

/** "(S6 Ep1)" style series metadata, or null when unknown. */
fun formatSeasonEpisode(programme: Programme): String? {
    val season = programme.season
    val episode = programme.episode
    return when {
        season != null && episode != null -> "(S$season Ep$episode)"
        episode != null -> "(Ep$episode)"
        else -> null
    }
}
