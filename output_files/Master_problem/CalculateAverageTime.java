package output_files.Master_problem;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class CalculateAverageTime {
    public static void main(String[] args) {
        Path folder = Paths.get("output_files/Master_problem");

        try {
            Files.list(folder)
                    .filter(path -> path.toString().endsWith(".jsonl"))
                    .sorted()
                    .forEach(path -> {
                        List<Long> columnGenDurations = new ArrayList<>();
                        List<Long> fullDurations = new ArrayList<>();

                        try (BufferedReader reader = Files.newBufferedReader(path)) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                long colGenNs = extractElapsedNs(line, "elapsedColumnGen");
                                long fullNs = extractElapsedNs(line, "elapsedFull");

                                if (colGenNs > 0) columnGenDurations.add(colGenNs);
                                if (fullNs > 0) fullDurations.add(fullNs);
                            }
                        } catch (IOException e) {
                            System.err.println("Error reading file " + path + ": " + e.getMessage());
                            return;
                        }

                        String fileName = path.getFileName().toString();
                        System.out.println("======== " + fileName + " ========");

                        if (!columnGenDurations.isEmpty()) {
                            long avgColGen = columnGenDurations.stream().mapToLong(Long::longValue).sum() / columnGenDurations.size();
                            System.out.println("Column Generation avg: " + formatDuration(avgColGen) + " (" + avgColGen + " ns)");
                        } else {
                            System.out.println("Column Generation avg: no data");
                        }

                        if (!fullDurations.isEmpty()) {
                            long avgFull = fullDurations.stream().mapToLong(Long::longValue).sum() / fullDurations.size();
                            System.out.println("Full Runtime avg:       " + formatDuration(avgFull) + " (" + avgFull + " ns)");
                        } else {
                            System.out.println("Full Runtime avg: no data");
                        }

                        System.out.println();
                    });

        } catch (IOException e) {
            System.err.println("Failed to list files: " + e.getMessage());
        }
    }

    // Extracts nanoseconds from a duration field like "elapsedColumnGen" or "elapsedFull"
    private static long extractElapsedNs(String line, String fieldName) {
        String regex = "\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"(\\d{2})h (\\d{2})m (\\d{2})s (\\d{3})ms (\\d{3})µs\"";
        Matcher matcher = Pattern.compile(regex).matcher(line);
        if (matcher.find()) {
            long hours = Long.parseLong(matcher.group(1));
            long minutes = Long.parseLong(matcher.group(2));
            long seconds = Long.parseLong(matcher.group(3));
            long millis = Long.parseLong(matcher.group(4));
            long micros = Long.parseLong(matcher.group(5));

            return hours * 3_600_000_000_000L +
                    minutes * 60_000_000_000L +
                    seconds * 1_000_000_000L +
                    millis * 1_000_000L +
                    micros * 1_000L;
        }
        return -1;
    }

    // Converts nanoseconds into a formatted string
    private static String formatDuration(long nanos) {
        long micros = (nanos / 1_000) % 1_000;
        long millis = (nanos / 1_000_000) % 1_000;
        long seconds = (nanos / 1_000_000_000) % 60;
        long minutes = (nanos / (60L * 1_000_000_000)) % 60;
        long hours = nanos / (60L * 60L * 1_000_000_000);

        return String.format("%02dh %02dm %02ds %03dms %03dµs",
                hours, minutes, seconds, millis, micros);
    }
}
