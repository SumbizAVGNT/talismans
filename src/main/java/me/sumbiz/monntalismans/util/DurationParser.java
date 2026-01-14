package me.sumbiz.monntalismans.util;

public final class DurationParser {

    private DurationParser() {}

    public static long parseToMillis(String s) {
        if (s == null || s.isBlank()) return 0L;
        String raw = s.trim().toLowerCase();

        try {
            if (raw.equals("infinity") || raw.equals("infinite") || raw.equals("inf") || raw.equals("unlimited")) {
                return Long.MAX_VALUE;
            }
            if (raw.endsWith("ms")) return Long.parseLong(raw.substring(0, raw.length() - 2));
            if (raw.endsWith("s")) return (long) (Double.parseDouble(raw.substring(0, raw.length() - 1)) * 1000L);
            if (raw.endsWith("m")) return (long) (Double.parseDouble(raw.substring(0, raw.length() - 1)) * 60_000L);
            if (raw.endsWith("h")) return (long) (Double.parseDouble(raw.substring(0, raw.length() - 1)) * 3_600_000L);
            return (long) (Double.parseDouble(raw) * 1000L);
        } catch (Exception e) {
            return 0L;
        }
    }
}
