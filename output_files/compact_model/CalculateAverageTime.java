package output_files.compact_model;

import java.io.*;
import java.util.*;

public class CalculateAverageTime {
    public static void main(String[] args) {
        // String inputLabel = "NL4"; // e.g., NL4, NL6, NL8
        // String fileName = inputLabel + "-times.jsonl";
        String fileName = "output_files/compact_model/NL6-times.jsonl";

        List<Long> durations = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                long duration = extractDuration(line);
                if (duration >= 0) {
                    durations.add(duration);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read file: " + e.getMessage());
            return;
        }

        if (durations.isEmpty()) {
            System.out.println("No durations found in file.");
            return;
        }

        long averageNs = durations.stream().mapToLong(Long::longValue).sum() / durations.size();

        System.out.println("Average duration:");
        System.out.println("  " + formatDuration(averageNs) + " (" + averageNs + " ns)");
    }

    // Extract duration_ns from JSONL line
    private static long extractDuration(String jsonLine) {
        try {
            String key = "\"duration_ns\":";
            int index = jsonLine.indexOf(key);
            if (index == -1) return -1;

            int start = index + key.length();
            int end = jsonLine.indexOf(",", start);
            if (end == -1) end = jsonLine.indexOf("}", start);

            String number = jsonLine.substring(start, end).trim();
            return Long.parseLong(number);
        } catch (Exception e) {
            return -1; // skip bad lines
        }
    }

    // Format nanoseconds to hh:mm:ss:ms:µs
    public static String formatDuration(long nanos) {
        long micros = (nanos / 1_000) % 1_000;
        long millis = (nanos / 1_000_000) % 1_000;
        long seconds = (nanos / 1_000_000_000) % 60;
        long minutes = (nanos / (60L * 1_000_000_000)) % 60;
        long hours = nanos / (60L * 60L * 1_000_000_000);

        return String.format("%02dh %02dm %02ds %03dms %03dµs",
                hours, minutes, seconds, millis, micros);
    }
}
