package com.blackrock.challenge.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Centralized datetime utilities for the BlackRock Challenge API.
 * All timestamps follow the format: "YYYY-MM-DD HH:mm:ss"
 */
public final class DateTimeUtil {

    public static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateTimeUtil() {
        // Utility class — no instantiation
    }

    /**
     * Parses a datetime string in "YYYY-MM-DD HH:mm:ss" format.
     *
     * @param dateStr the date string to parse
     * @return parsed LocalDateTime
     * @throws IllegalArgumentException if the format is invalid
     */
    public static LocalDateTime parse(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            throw new IllegalArgumentException("Date string must not be null or blank");
        }
        try {
            return LocalDateTime.parse(dateStr.trim(), FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid date format: '" + dateStr + "'. Expected: YYYY-MM-DD HH:mm:ss", e);
        }
    }

    /**
     * Formats a LocalDateTime to "YYYY-MM-DD HH:mm:ss" string.
     *
     * @param dateTime the datetime to format
     * @return formatted string
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            throw new IllegalArgumentException("DateTime must not be null");
        }
        return dateTime.format(FORMATTER);
    }

    /**
     * Returns true if the given point falls within [start, end] (inclusive).
     *
     * @param point the datetime to check
     * @param start range start (inclusive)
     * @param end   range end (inclusive)
     * @return true if in range
     */
    public static boolean isInRange(LocalDateTime point, LocalDateTime start, LocalDateTime end) {
        return !point.isBefore(start) && !point.isAfter(end);
    }
}
