package ru.vlyashuk.roadbuddy.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

object DateTimeConverter {

    enum class Pattern {
        SHORT,      // 15.08.2026 14:30
        LONG,       // 15 Aug 2026 14:30
        ISO,        // 2026-08-15 14:30
        TIME_ONLY   // 14:30
    }

    private val timeZone: TimeZone
        get() = TimeZone.currentSystemDefault()

    private val shortFormat = LocalDateTime.Format {
        day(); char('.'); monthNumber(); char('.'); year()
        char(' ')
        hour(); char(':'); minute()
    }

    private val longFormat = LocalDateTime.Format {
        day(); char(' '); monthName(MonthNames.ENGLISH_ABBREVIATED); char(' '); year()
        char(' ')
        hour(); char(':'); minute()
    }

    private val isoFormat = LocalDateTime.Format {
        date(LocalDate.Formats.ISO)
        char(' ')
        hour(); char(':'); minute()
    }

    private val timeOnlyFormat = LocalDateTime.Format {
        hour(); char(':'); minute()
    }

    fun format(
        instant: Instant,
        pattern: Pattern = Pattern.SHORT
    ): String {
        val format: DateTimeFormat<LocalDateTime> = when (pattern) {
            Pattern.SHORT -> shortFormat
            Pattern.LONG -> longFormat
            Pattern.ISO -> isoFormat
            Pattern.TIME_ONLY -> timeOnlyFormat
        }
        return instant.toLocalDateTime(timeZone).format(format)
    }
}