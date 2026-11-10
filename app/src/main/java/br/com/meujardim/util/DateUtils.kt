package br.com.meujardim.util

import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

private val fullDate = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PT_BR)
private val shortDate = DateTimeFormatter.ofPattern("dd/MM/yyyy", PT_BR)
private val dayMonth = DateTimeFormatter.ofPattern("d 'de' MMM", PT_BR)
private val time = DateTimeFormatter.ofPattern("HH:mm", PT_BR)
private val monthYear = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", PT_BR)

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
fun Long.toLocalDateTime(): LocalDateTime = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()
fun LocalDateTime.toEpochMillis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
fun LocalDate.atTimeMillis(t: LocalTime): Long = atTime(t).toEpochMillis()

fun String.capitalizeFirst(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(PT_BR) else it.toString() }

fun LocalDate.formatFull(): String = format(fullDate).capitalizeFirst()
fun LocalDate.formatShort(): String = format(shortDate)
fun LocalDate.formatDayMonth(): String = format(dayMonth)
fun LocalDate.formatMonthYear(): String = format(monthYear).capitalizeFirst()
fun Long.formatTime(): String = toLocalDateTime().format(time)
fun Long.formatDateTime(): String = "${toLocalDate().formatShort()} às ${formatTime()}"
fun formatHourMinute(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

/** "hoje", "amanhã", "em 3 dias", "ontem", "há 2 dias". */
fun LocalDate.relativeLabel(today: LocalDate = LocalDate.now()): String {
    val diff = ChronoUnit.DAYS.between(today, this)
    return when {
        diff == 0L -> "hoje"
        diff == 1L -> "amanhã"
        diff == -1L -> "ontem"
        diff > 1 -> "em $diff dias"
        else -> "há ${-diff} dias"
    }
}

fun greetingFor(now: LocalTime = LocalTime.now()): String = when (now.hour) {
    in 5..11 -> "Bom dia"
    in 12..17 -> "Boa tarde"
    else -> "Boa noite"
}

/** Remove acentos e caixa para comparações de busca ("Maranta" casa com "marânta"). */
fun String.normalizedForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(PT_BR)
        .trim()
