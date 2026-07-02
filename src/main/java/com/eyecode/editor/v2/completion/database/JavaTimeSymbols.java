package com.eyecode.editor.v2.completion.database;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;

import java.util.List;

public final class JavaTimeSymbols {

    private JavaTimeSymbols() {}

    public static List<CompletionItem> getAll() {
        return List.of(
                // ── Classes ──
                CompletionItem.builder("LocalDate", "LocalDate", CompletionItemKind.CLASS)
                        .detail("java.time.LocalDate")
                        .owner("java.time")
                        .category("Class")
                        .documentation("Represents a date without a time zone, such as 2024-01-15.")
                        .example("LocalDate today = LocalDate.now();")
                        .build(),

                CompletionItem.builder("LocalTime", "LocalTime", CompletionItemKind.CLASS)
                        .detail("java.time.LocalTime")
                        .owner("java.time")
                        .category("Class")
                        .documentation("Represents a time without a time zone, such as 14:30:45.")
                        .build(),

                CompletionItem.builder("LocalDateTime", "LocalDateTime", CompletionItemKind.CLASS)
                        .detail("java.time.LocalDateTime")
                        .owner("java.time")
                        .category("Class")
                        .documentation("Represents a date-time without a time zone, combining LocalDate and LocalTime.")
                        .build(),

                CompletionItem.builder("ZonedDateTime", "ZonedDateTime", CompletionItemKind.CLASS)
                        .detail("java.time.ZonedDateTime")
                        .owner("java.time")
                        .category("Class")
                        .documentation("Represents a date-time with a time zone.")
                        .build(),

                CompletionItem.builder("Instant", "Instant", CompletionItemKind.CLASS)
                        .detail("java.time.Instant")
                        .owner("java.time")
                        .category("Class")
                        .documentation("Represents a single point in time on the timeline in UTC.")
                        .build(),

                CompletionItem.builder("Duration", "Duration", CompletionItemKind.CLASS)
                        .detail("java.time.Duration")
                        .owner("java.time")
                        .category("Class")
                        .documentation("Represents a time-based amount of time, such as seconds and nanoseconds.")
                        .build(),

                CompletionItem.builder("Period", "Period", CompletionItemKind.CLASS)
                        .detail("java.time.Period")
                        .owner("java.time")
                        .category("Class")
                        .documentation("Represents a date-based amount of time in years, months, and days.")
                        .build(),

                CompletionItem.builder("ZoneId", "ZoneId", CompletionItemKind.CLASS)
                        .detail("java.time.ZoneId")
                        .owner("java.time")
                        .category("Class")
                        .documentation("Represents a time zone identifier, such as America/Sao_Paulo or UTC.")
                        .build(),

                CompletionItem.builder("DateTimeFormatter", "DateTimeFormatter", CompletionItemKind.CLASS)
                        .detail("java.time.format.DateTimeFormatter")
                        .owner("java.time.format")
                        .category("Class")
                        .documentation("Formats and parses dates and times using patterns.")
                        .example("String formatted = LocalDateTime.now().format(DateTimeFormatter.ofPattern(\"yyyy-MM-dd\"));")
                        .build(),

                // ── LocalDate Methods ──
                CompletionItem.builder("now", "now", CompletionItemKind.METHOD)
                        .detail("LocalDate.now")
                        .signature("LocalDate.now()")
                        .returnType("LocalDate")
                        .owner("java.time.LocalDate")
                        .category("Method")
                        .documentation("Returns the current date from the system clock in the default time zone.")
                        .build(),

                CompletionItem.builder("of", "of", CompletionItemKind.METHOD)
                        .detail("LocalDate.of")
                        .signature("LocalDate.of(int year, int month, int dayOfMonth)")
                        .returnType("LocalDate")
                        .owner("java.time.LocalDate")
                        .category("Method")
                        .documentation("Creates a LocalDate instance with the given year, month, and day.")
                        .build(),

                CompletionItem.builder("parse", "parse", CompletionItemKind.METHOD)
                        .detail("LocalDate.parse")
                        .signature("LocalDate.parse(CharSequence text)")
                        .returnType("LocalDate")
                        .owner("java.time.LocalDate")
                        .category("Method")
                        .documentation("Parses a date string in ISO-8601 format (yyyy-MM-dd) into a LocalDate.")
                        .build(),

                CompletionItem.builder("getYear", "getYear", CompletionItemKind.METHOD)
                        .detail("LocalDate.getYear")
                        .signature("LocalDate.getYear()")
                        .returnType("int")
                        .owner("java.time.LocalDate")
                        .category("Method")
                        .documentation("Returns the year field of this date.")
                        .build(),

                CompletionItem.builder("getMonth", "getMonth", CompletionItemKind.METHOD)
                        .detail("LocalDate.getMonth")
                        .signature("LocalDate.getMonth()")
                        .returnType("Month")
                        .owner("java.time.LocalDate")
                        .category("Method")
                        .documentation("Returns the month-of-year field as a Month enum.")
                        .build(),

                CompletionItem.builder("getDayOfMonth", "getDayOfMonth", CompletionItemKind.METHOD)
                        .detail("LocalDate.getDayOfMonth")
                        .signature("LocalDate.getDayOfMonth()")
                        .returnType("int")
                        .owner("java.time.LocalDate")
                        .category("Method")
                        .documentation("Returns the day-of-month field (1-31).")
                        .build(),

                CompletionItem.builder("getDayOfWeek", "getDayOfWeek", CompletionItemKind.METHOD)
                        .detail("LocalDate.getDayOfWeek")
                        .signature("LocalDate.getDayOfWeek()")
                        .returnType("DayOfWeek")
                        .owner("java.time.LocalDate")
                        .category("Method")
                        .documentation("Returns the day-of-week field as a DayOfWeek enum.")
                        .build(),

                CompletionItem.builder("plusDays", "plusDays", CompletionItemKind.METHOD)
                        .detail("LocalDate.plusDays")
                        .signature("LocalDate.plusDays(long daysToAdd)")
                        .returnType("LocalDate")
                        .owner("java.time.LocalDate")
                        .category("Method")
                        .documentation("Returns a copy of this date with the specified number of days added.")
                        .build(),

                CompletionItem.builder("minusDays", "minusDays", CompletionItemKind.METHOD)
                        .detail("LocalDate.minusDays")
                        .signature("LocalDate.minusDays(long daysToSubtract)")
                        .returnType("LocalDate")
                        .owner("java.time.LocalDate")
                        .category("Method")
                        .documentation("Returns a copy of this date with the specified number of days subtracted.")
                        .build(),

                CompletionItem.builder("plusMonths", "plusMonths", CompletionItemKind.METHOD)
                        .detail("LocalDate.plusMonths")
                        .signature("LocalDate.plusMonths(long monthsToAdd)")
                        .returnType("LocalDate")
                        .owner("java.time.LocalDate")
                        .category("Method")
                        .documentation("Returns a copy of this date with the specified number of months added.")
                        .build(),

                CompletionItem.builder("minusMonths", "minusMonths", CompletionItemKind.METHOD)
                        .detail("LocalDate.minusMonths")
                        .signature("LocalDate.minusMonths(long monthsToSubtract)")
                        .returnType("LocalDate")
                        .owner("java.time.LocalDate")
                        .category("Method")
                        .documentation("Returns a copy of this date with the specified number of months subtracted.")
                        .build(),

                CompletionItem.builder("isBefore", "isBefore", CompletionItemKind.METHOD)
                        .detail("LocalDate.isBefore")
                        .signature("LocalDate.isBefore(ChronoLocalDate other)")
                        .returnType("boolean")
                        .owner("java.time.LocalDate")
                        .category("Method")
                        .documentation("Tests if this date is before the specified date.")
                        .build(),

                CompletionItem.builder("isAfter", "isAfter", CompletionItemKind.METHOD)
                        .detail("LocalDate.isAfter")
                        .signature("LocalDate.isAfter(ChronoLocalDate other)")
                        .returnType("boolean")
                        .owner("java.time.LocalDate")
                        .category("Method")
                        .documentation("Tests if this date is after the specified date.")
                        .build(),

                CompletionItem.builder("atTime", "atTime", CompletionItemKind.METHOD)
                        .detail("LocalDate.atTime")
                        .signature("LocalDate.atTime(int hour, int minute)")
                        .returnType("LocalDateTime")
                        .owner("java.time.LocalDate")
                        .category("Method")
                        .documentation("Combines this date with a time to create a LocalDateTime.")
                        .build(),

                CompletionItem.builder("format", "format", CompletionItemKind.METHOD)
                        .detail("LocalDate.format")
                        .signature("LocalDate.format(DateTimeFormatter formatter)")
                        .returnType("String")
                        .owner("java.time.LocalDate")
                        .category("Method")
                        .documentation("Formats this date using the specified formatter.")
                        .build(),

                // ── LocalTime Methods ──
                CompletionItem.builder("now", "now", CompletionItemKind.METHOD)
                        .detail("LocalTime.now")
                        .signature("LocalTime.now()")
                        .returnType("LocalTime")
                        .owner("java.time.LocalTime")
                        .category("Method")
                        .documentation("Returns the current time from the system clock in the default time zone.")
                        .build(),

                CompletionItem.builder("of", "of", CompletionItemKind.METHOD)
                        .detail("LocalTime.of")
                        .signature("LocalTime.of(int hour, int minute)")
                        .returnType("LocalTime")
                        .owner("java.time.LocalTime")
                        .category("Method")
                        .documentation("Creates a LocalTime instance with the given hour and minute.")
                        .build(),

                CompletionItem.builder("parse", "parse", CompletionItemKind.METHOD)
                        .detail("LocalTime.parse")
                        .signature("LocalTime.parse(CharSequence text)")
                        .returnType("LocalTime")
                        .owner("java.time.LocalTime")
                        .category("Method")
                        .documentation("Parses a time string in ISO-8601 format (HH:mm:ss) into a LocalTime.")
                        .build(),

                CompletionItem.builder("getHour", "getHour", CompletionItemKind.METHOD)
                        .detail("LocalTime.getHour")
                        .signature("LocalTime.getHour()")
                        .returnType("int")
                        .owner("java.time.LocalTime")
                        .category("Method")
                        .documentation("Returns the hour-of-day field (0-23).")
                        .build(),

                CompletionItem.builder("getMinute", "getMinute", CompletionItemKind.METHOD)
                        .detail("LocalTime.getMinute")
                        .signature("LocalTime.getMinute()")
                        .returnType("int")
                        .owner("java.time.LocalTime")
                        .category("Method")
                        .documentation("Returns the minute-of-hour field (0-59).")
                        .build(),

                CompletionItem.builder("getSecond", "getSecond", CompletionItemKind.METHOD)
                        .detail("LocalTime.getSecond")
                        .signature("LocalTime.getSecond()")
                        .returnType("int")
                        .owner("java.time.LocalTime")
                        .category("Method")
                        .documentation("Returns the second-of-minute field (0-59).")
                        .build(),

                CompletionItem.builder("plusHours", "plusHours", CompletionItemKind.METHOD)
                        .detail("LocalTime.plusHours")
                        .signature("LocalTime.plusHours(long hoursToAdd)")
                        .returnType("LocalTime")
                        .owner("java.time.LocalTime")
                        .category("Method")
                        .documentation("Returns a copy of this time with the specified number of hours added.")
                        .build(),

                CompletionItem.builder("minusHours", "minusHours", CompletionItemKind.METHOD)
                        .detail("LocalTime.minusHours")
                        .signature("LocalTime.minusHours(long hoursToSubtract)")
                        .returnType("LocalTime")
                        .owner("java.time.LocalTime")
                        .category("Method")
                        .documentation("Returns a copy of this time with the specified number of hours subtracted.")
                        .build(),

                CompletionItem.builder("atDate", "atDate", CompletionItemKind.METHOD)
                        .detail("LocalTime.atDate")
                        .signature("LocalTime.atDate(LocalDate date)")
                        .returnType("LocalDateTime")
                        .owner("java.time.LocalTime")
                        .category("Method")
                        .documentation("Combines this time with a date to create a LocalDateTime.")
                        .build(),

                // ── LocalDateTime Methods ──
                CompletionItem.builder("now", "now", CompletionItemKind.METHOD)
                        .detail("LocalDateTime.now")
                        .signature("LocalDateTime.now()")
                        .returnType("LocalDateTime")
                        .owner("java.time.LocalDateTime")
                        .category("Method")
                        .documentation("Returns the current date-time from the system clock.")
                        .build(),

                CompletionItem.builder("of", "of", CompletionItemKind.METHOD)
                        .detail("LocalDateTime.of")
                        .signature("LocalDateTime.of(int year, int month, int dayOfMonth, int hour, int minute)")
                        .returnType("LocalDateTime")
                        .owner("java.time.LocalDateTime")
                        .category("Method")
                        .documentation("Creates a LocalDateTime instance from date and time fields.")
                        .build(),

                CompletionItem.builder("parse", "parse", CompletionItemKind.METHOD)
                        .detail("LocalDateTime.parse")
                        .signature("LocalDateTime.parse(CharSequence text)")
                        .returnType("LocalDateTime")
                        .owner("java.time.LocalDateTime")
                        .category("Method")
                        .documentation("Parses a date-time string in ISO-8601 format (yyyy-MM-ddTHH:mm:ss).")
                        .build(),

                CompletionItem.builder("toLocalDate", "toLocalDate", CompletionItemKind.METHOD)
                        .detail("LocalDateTime.toLocalDate")
                        .signature("LocalDateTime.toLocalDate()")
                        .returnType("LocalDate")
                        .owner("java.time.LocalDateTime")
                        .category("Method")
                        .documentation("Returns the LocalDate part of this date-time.")
                        .build(),

                CompletionItem.builder("toLocalTime", "toLocalTime", CompletionItemKind.METHOD)
                        .detail("LocalDateTime.toLocalTime")
                        .signature("LocalDateTime.toLocalTime()")
                        .returnType("LocalTime")
                        .owner("java.time.LocalDateTime")
                        .category("Method")
                        .documentation("Returns the LocalTime part of this date-time.")
                        .build(),

                CompletionItem.builder("plusDays", "plusDays", CompletionItemKind.METHOD)
                        .detail("LocalDateTime.plusDays")
                        .signature("LocalDateTime.plusDays(long days)")
                        .returnType("LocalDateTime")
                        .owner("java.time.LocalDateTime")
                        .category("Method")
                        .documentation("Returns a copy of this date-time with the specified number of days added.")
                        .build(),

                CompletionItem.builder("minusDays", "minusDays", CompletionItemKind.METHOD)
                        .detail("LocalDateTime.minusDays")
                        .signature("LocalDateTime.minusDays(long days)")
                        .returnType("LocalDateTime")
                        .owner("java.time.LocalDateTime")
                        .category("Method")
                        .documentation("Returns a copy of this date-time with the specified number of days subtracted.")
                        .build(),

                CompletionItem.builder("atZone", "atZone", CompletionItemKind.METHOD)
                        .detail("LocalDateTime.atZone")
                        .signature("LocalDateTime.atZone(ZoneId zone)")
                        .returnType("ZonedDateTime")
                        .owner("java.time.LocalDateTime")
                        .category("Method")
                        .documentation("Combines this date-time with a time zone to create a ZonedDateTime.")
                        .build(),

                CompletionItem.builder("format", "format", CompletionItemKind.METHOD)
                        .detail("LocalDateTime.format")
                        .signature("LocalDateTime.format(DateTimeFormatter formatter)")
                        .returnType("String")
                        .owner("java.time.LocalDateTime")
                        .category("Method")
                        .documentation("Formats this date-time using the specified formatter.")
                        .build(),

                // ── Instant Methods ──
                CompletionItem.builder("now", "now", CompletionItemKind.METHOD)
                        .detail("Instant.now")
                        .signature("Instant.now()")
                        .returnType("Instant")
                        .owner("java.time.Instant")
                        .category("Method")
                        .documentation("Returns the current instant from the system clock in UTC.")
                        .build(),

                CompletionItem.builder("ofEpochSecond", "ofEpochSecond", CompletionItemKind.METHOD)
                        .detail("Instant.ofEpochSecond")
                        .signature("Instant.ofEpochSecond(long epochSecond)")
                        .returnType("Instant")
                        .owner("java.time.Instant")
                        .category("Method")
                        .documentation("Creates an Instant from the number of seconds since the Unix epoch.")
                        .build(),

                CompletionItem.builder("ofEpochMilli", "ofEpochMilli", CompletionItemKind.METHOD)
                        .detail("Instant.ofEpochMilli")
                        .signature("Instant.ofEpochMilli(long epochMilli)")
                        .returnType("Instant")
                        .owner("java.time.Instant")
                        .category("Method")
                        .documentation("Creates an Instant from the number of milliseconds since the Unix epoch.")
                        .build(),

                CompletionItem.builder("getEpochSecond", "getEpochSecond", CompletionItemKind.METHOD)
                        .detail("Instant.getEpochSecond")
                        .signature("Instant.getEpochSecond()")
                        .returnType("long")
                        .owner("java.time.Instant")
                        .category("Method")
                        .documentation("Returns the number of seconds from the Unix epoch.")
                        .build(),

                CompletionItem.builder("toEpochMilli", "toEpochMilli", CompletionItemKind.METHOD)
                        .detail("Instant.toEpochMilli")
                        .signature("Instant.toEpochMilli()")
                        .returnType("long")
                        .owner("java.time.Instant")
                        .category("Method")
                        .documentation("Converts this instant to the number of milliseconds since the Unix epoch.")
                        .build(),

                // ── Duration Methods ──
                CompletionItem.builder("ofDays", "ofDays", CompletionItemKind.METHOD)
                        .detail("Duration.ofDays")
                        .signature("Duration.ofDays(long days)")
                        .returnType("Duration")
                        .owner("java.time.Duration")
                        .category("Method")
                        .documentation("Creates a Duration representing the specified number of days.")
                        .build(),

                CompletionItem.builder("ofHours", "ofHours", CompletionItemKind.METHOD)
                        .detail("Duration.ofHours")
                        .signature("Duration.ofHours(long hours)")
                        .returnType("Duration")
                        .owner("java.time.Duration")
                        .category("Method")
                        .documentation("Creates a Duration representing the specified number of hours.")
                        .build(),

                CompletionItem.builder("ofMinutes", "ofMinutes", CompletionItemKind.METHOD)
                        .detail("Duration.ofMinutes")
                        .signature("Duration.ofMinutes(long minutes)")
                        .returnType("Duration")
                        .owner("java.time.Duration")
                        .category("Method")
                        .documentation("Creates a Duration representing the specified number of minutes.")
                        .build(),

                CompletionItem.builder("ofSeconds", "ofSeconds", CompletionItemKind.METHOD)
                        .detail("Duration.ofSeconds")
                        .signature("Duration.ofSeconds(long seconds)")
                        .returnType("Duration")
                        .owner("java.time.Duration")
                        .category("Method")
                        .documentation("Creates a Duration representing the specified number of seconds.")
                        .build(),

                CompletionItem.builder("ofMillis", "ofMillis", CompletionItemKind.METHOD)
                        .detail("Duration.ofMillis")
                        .signature("Duration.ofMillis(long millis)")
                        .returnType("Duration")
                        .owner("java.time.Duration")
                        .category("Method")
                        .documentation("Creates a Duration representing the specified number of milliseconds.")
                        .build(),

                CompletionItem.builder("between", "between", CompletionItemKind.METHOD)
                        .detail("Duration.between")
                        .signature("Duration.between(Temporal startInclusive, Temporal endExclusive)")
                        .returnType("Duration")
                        .owner("java.time.Duration")
                        .category("Method")
                        .documentation("Creates a Duration representing the time between two temporal objects.")
                        .build(),

                CompletionItem.builder("toDays", "toDays", CompletionItemKind.METHOD)
                        .detail("Duration.toDays")
                        .signature("Duration.toDays()")
                        .returnType("long")
                        .owner("java.time.Duration")
                        .category("Method")
                        .documentation("Converts this duration to the number of days.")
                        .build(),

                CompletionItem.builder("toHours", "toHours", CompletionItemKind.METHOD)
                        .detail("Duration.toHours")
                        .signature("Duration.toHours()")
                        .returnType("long")
                        .owner("java.time.Duration")
                        .category("Method")
                        .documentation("Converts this duration to the number of hours.")
                        .build(),

                CompletionItem.builder("toMinutes", "toMinutes", CompletionItemKind.METHOD)
                        .detail("Duration.toMinutes")
                        .signature("Duration.toMinutes()")
                        .returnType("long")
                        .owner("java.time.Duration")
                        .category("Method")
                        .documentation("Converts this duration to the number of minutes.")
                        .build(),

                CompletionItem.builder("toMillis", "toMillis", CompletionItemKind.METHOD)
                        .detail("Duration.toMillis")
                        .signature("Duration.toMillis()")
                        .returnType("long")
                        .owner("java.time.Duration")
                        .category("Method")
                        .documentation("Converts this duration to the number of milliseconds.")
                        .build(),

                // ── Period Methods ──
                CompletionItem.builder("of", "of", CompletionItemKind.METHOD)
                        .detail("Period.of")
                        .signature("Period.of(int years, int months, int days)")
                        .returnType("Period")
                        .owner("java.time.Period")
                        .category("Method")
                        .documentation("Creates a Period with the specified years, months, and days.")
                        .build(),

                CompletionItem.builder("ofDays", "ofDays", CompletionItemKind.METHOD)
                        .detail("Period.ofDays")
                        .signature("Period.ofDays(int days)")
                        .returnType("Period")
                        .owner("java.time.Period")
                        .category("Method")
                        .documentation("Creates a Period representing the specified number of days.")
                        .build(),

                CompletionItem.builder("ofWeeks", "ofWeeks", CompletionItemKind.METHOD)
                        .detail("Period.ofWeeks")
                        .signature("Period.ofWeeks(int weeks)")
                        .returnType("Period")
                        .owner("java.time.Period")
                        .category("Method")
                        .documentation("Creates a Period representing the specified number of weeks.")
                        .build(),

                CompletionItem.builder("ofMonths", "ofMonths", CompletionItemKind.METHOD)
                        .detail("Period.ofMonths")
                        .signature("Period.ofMonths(int months)")
                        .returnType("Period")
                        .owner("java.time.Period")
                        .category("Method")
                        .documentation("Creates a Period representing the specified number of months.")
                        .build(),

                CompletionItem.builder("ofYears", "ofYears", CompletionItemKind.METHOD)
                        .detail("Period.ofYears")
                        .signature("Period.ofYears(int years)")
                        .returnType("Period")
                        .owner("java.time.Period")
                        .category("Method")
                        .documentation("Creates a Period representing the specified number of years.")
                        .build(),

                CompletionItem.builder("between", "between", CompletionItemKind.METHOD)
                        .detail("Period.between")
                        .signature("Period.between(LocalDate startDateInclusive, LocalDate endDateExclusive)")
                        .returnType("Period")
                        .owner("java.time.Period")
                        .category("Method")
                        .documentation("Creates a Period representing the date-based amount between two dates.")
                        .build(),

                CompletionItem.builder("getDays", "getDays", CompletionItemKind.METHOD)
                        .detail("Period.getDays")
                        .signature("Period.getDays()")
                        .returnType("int")
                        .owner("java.time.Period")
                        .category("Method")
                        .documentation("Returns the days component of this Period.")
                        .build(),

                CompletionItem.builder("getMonths", "getMonths", CompletionItemKind.METHOD)
                        .detail("Period.getMonths")
                        .signature("Period.getMonths()")
                        .returnType("int")
                        .owner("java.time.Period")
                        .category("Method")
                        .documentation("Returns the months component of this Period.")
                        .build(),

                CompletionItem.builder("getYears", "getYears", CompletionItemKind.METHOD)
                        .detail("Period.getYears")
                        .signature("Period.getYears()")
                        .returnType("int")
                        .owner("java.time.Period")
                        .category("Method")
                        .documentation("Returns the years component of this Period.")
                        .build(),

                // ── ZoneId Methods ──
                CompletionItem.builder("of", "of", CompletionItemKind.METHOD)
                        .detail("ZoneId.of")
                        .signature("ZoneId.of(String zoneId)")
                        .returnType("ZoneId")
                        .owner("java.time.ZoneId")
                        .category("Method")
                        .documentation("Creates a ZoneId from a zone identifier string, such as America/Sao_Paulo or +05:00.")
                        .build(),

                CompletionItem.builder("systemDefault", "systemDefault", CompletionItemKind.METHOD)
                        .detail("ZoneId.systemDefault")
                        .signature("ZoneId.systemDefault()")
                        .returnType("ZoneId")
                        .owner("java.time.ZoneId")
                        .category("Method")
                        .documentation("Returns the system default time zone.")
                        .build(),

                CompletionItem.builder("getAvailableZoneIds", "getAvailableZoneIds", CompletionItemKind.METHOD)
                        .detail("ZoneId.getAvailableZoneIds")
                        .signature("ZoneId.getAvailableZoneIds()")
                        .returnType("Set<String>")
                        .owner("java.time.ZoneId")
                        .category("Method")
                        .documentation("Returns the set of available time zone IDs.")
                        .build(),

                // ── ZonedDateTime Methods ──
                CompletionItem.builder("now", "now", CompletionItemKind.METHOD)
                        .detail("ZonedDateTime.now")
                        .signature("ZonedDateTime.now()")
                        .returnType("ZonedDateTime")
                        .owner("java.time.ZonedDateTime")
                        .category("Method")
                        .documentation("Returns the current date-time with the system default time zone.")
                        .build(),

                CompletionItem.builder("of", "of", CompletionItemKind.METHOD)
                        .detail("ZonedDateTime.of")
                        .signature("ZonedDateTime.of(LocalDateTime dateTime, ZoneId zone)")
                        .returnType("ZonedDateTime")
                        .owner("java.time.ZonedDateTime")
                        .category("Method")
                        .documentation("Creates a ZonedDateTime from a LocalDateTime and a ZoneId.")
                        .build(),

                CompletionItem.builder("toLocalDate", "toLocalDate", CompletionItemKind.METHOD)
                        .detail("ZonedDateTime.toLocalDate")
                        .signature("ZonedDateTime.toLocalDate()")
                        .returnType("LocalDate")
                        .owner("java.time.ZonedDateTime")
                        .category("Method")
                        .documentation("Returns the LocalDate part of this ZonedDateTime.")
                        .build(),

                CompletionItem.builder("toLocalTime", "toLocalTime", CompletionItemKind.METHOD)
                        .detail("ZonedDateTime.toLocalTime")
                        .signature("ZonedDateTime.toLocalTime()")
                        .returnType("LocalTime")
                        .owner("java.time.ZonedDateTime")
                        .category("Method")
                        .documentation("Returns the LocalTime part of this ZonedDateTime.")
                        .build(),

                CompletionItem.builder("toInstant", "toInstant", CompletionItemKind.METHOD)
                        .detail("ZonedDateTime.toInstant")
                        .signature("ZonedDateTime.toInstant()")
                        .returnType("Instant")
                        .owner("java.time.ZonedDateTime")
                        .category("Method")
                        .documentation("Converts this ZonedDateTime to an Instant (epoch time in UTC).")
                        .build(),

                CompletionItem.builder("getZone", "getZone", CompletionItemKind.METHOD)
                        .detail("ZonedDateTime.getZone")
                        .signature("ZonedDateTime.getZone()")
                        .returnType("ZoneId")
                        .owner("java.time.ZonedDateTime")
                        .category("Method")
                        .documentation("Returns the time zone of this ZonedDateTime.")
                        .build(),

                CompletionItem.builder("withZoneSameInstant", "withZoneSameInstant", CompletionItemKind.METHOD)
                        .detail("ZonedDateTime.withZoneSameInstant")
                        .signature("ZonedDateTime.withZoneSameInstant(ZoneId zone)")
                        .returnType("ZonedDateTime")
                        .owner("java.time.ZonedDateTime")
                        .category("Method")
                        .documentation("Converts this ZonedDateTime to another time zone preserving the same instant.")
                        .build(),

                CompletionItem.builder("plusDays", "plusDays", CompletionItemKind.METHOD)
                        .detail("ZonedDateTime.plusDays")
                        .signature("ZonedDateTime.plusDays(long days)")
                        .returnType("ZonedDateTime")
                        .owner("java.time.ZonedDateTime")
                        .category("Method")
                        .documentation("Returns a copy of this date-time with the specified number of days added.")
                        .build(),

                CompletionItem.builder("format", "format", CompletionItemKind.METHOD)
                        .detail("ZonedDateTime.format")
                        .signature("ZonedDateTime.format(DateTimeFormatter formatter)")
                        .returnType("String")
                        .owner("java.time.ZonedDateTime")
                        .category("Method")
                        .documentation("Formats this date-time using the specified formatter.")
                        .build()
        );
    }
}
