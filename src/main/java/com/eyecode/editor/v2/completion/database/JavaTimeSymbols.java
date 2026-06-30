package com.eyecode.editor.v2.completion.database;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;

import java.util.List;

public final class JavaTimeSymbols {

    private JavaTimeSymbols() {}

    public static List<CompletionItem> getAll() {
        return List.of(
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

                CompletionItem.builder("DateTimeFormatter", "DateTimeFormatter", CompletionItemKind.CLASS)
                        .detail("java.time.format.DateTimeFormatter")
                        .owner("java.time.format")
                        .category("Class")
                        .documentation("Formats and parses dates and times using patterns.")
                        .example("String formatted = LocalDateTime.now().format(DateTimeFormatter.ofPattern(\"yyyy-MM-dd\"));")
                        .build()
        );
    }
}
