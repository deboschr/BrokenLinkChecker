package com.unpar.skripsi.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Utility class untuk format waktu dan durasi di UI.
 */
public final class TimeUtil {

    // Formatter untuk Access Time (contoh: 2025-08-13 14:32:05)
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private TimeUtil() {
        // utility class, no instance
    }

    /**
     * Format Instant menjadi string dengan zona waktu lokal.
     * @param instant waktu dalam Instant, boleh null
     * @return string format waktu atau "-" jika null
     */
    public static String format(Instant instant) {
        return (instant == null) ? "-" : DATE_TIME_FORMATTER.format(instant);
    }

    /**
     * Format durasi dalam detik menjadi "mm:ss" atau "hh:mm:ss".
     * @param seconds total detik
     * @return string format durasi
     */
    public static String formatDuration(long seconds) {
        if (seconds < 0) seconds = 0;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%02d:%02d", minutes, secs);
        }
    }

    /**
     * Hitung durasi antara start dan end dalam detik.
     * @param start waktu mulai (Instant)
     * @param end waktu akhir (Instant)
     * @return total detik
     */
    public static long secondsBetween(Instant start, Instant end) {
        Objects.requireNonNull(start, "start cannot be null");
        Objects.requireNonNull(end, "end cannot be null");
        return Duration.between(start, end).toSeconds();
    }
}
