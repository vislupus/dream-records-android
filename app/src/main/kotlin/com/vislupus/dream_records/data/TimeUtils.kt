package com.vislupus.dream_records.data

import com.vislupus.dream_records.config.AppConfig
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Date/time helpers: 24-hour clock, Monday-first weeks, night grouping. */
object TimeUtils {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val timeFmt = DateTimeFormatter.ofPattern(AppConfig.Calendar.TIME_PATTERN)
    private val dateFmt = DateTimeFormatter.ofPattern(AppConfig.Calendar.DATE_PATTERN)
    private val dateTimeFmt = DateTimeFormatter.ofPattern(AppConfig.Calendar.DATE_TIME_PATTERN)
    private val isoDate = DateTimeFormatter.ISO_LOCAL_DATE

    fun now(): Long = System.currentTimeMillis()

    fun toLocal(epochMs: Long): LocalDateTime = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDateTime()

    /** "yyyy-MM-dd" key of the night a recording belongs to. See AppConfig.Nights. */
    fun nightDateFor(epochMs: Long): String {
        val ldt = toLocal(epochMs)
        val date = if (ldt.hour >= AppConfig.Nights.NIGHT_BOUNDARY_HOUR) ldt.toLocalDate().plusDays(1) else ldt.toLocalDate()
        return date.format(isoDate)
    }

    fun parseNightDate(key: String): LocalDate = LocalDate.parse(key, isoDate)
    fun nightKey(date: LocalDate): String = date.format(isoDate)

    fun formatTime(epochMs: Long): String = toLocal(epochMs).format(timeFmt)
    fun formatDate(epochMs: Long): String = toLocal(epochMs).format(dateFmt)
    fun formatDateTime(epochMs: Long): String = toLocal(epochMs).format(dateTimeFmt)
    fun formatDate(date: LocalDate): String = date.format(dateFmt)

    fun formatMonth(date: LocalDate, locale: Locale): String =
        date.format(DateTimeFormatter.ofPattern(AppConfig.Calendar.MONTH_PATTERN, locale))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

    /** mm:ss or h:mm:ss */
    fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.ROOT, "%02d:%02d", m, s)
    }

    fun isoNow(): String = Instant.now().toString()
}
