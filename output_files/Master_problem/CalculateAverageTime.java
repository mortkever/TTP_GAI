package output_files.Master_problem;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class CalculateAverageTime {

    private static final String[] FIELDS_TO_ANALYZE = {
            "columnsPerIter",
            "percentPerIter",
            "aantalIteraties",
            "aantalKolommen",
            "LPsolution:",
            "IPsolution",
            "elapsedFull",
            "elapsedInitial",
            "elapsedColumnGen",
            "elapsedIpSolution"
    };

    public static void main(String[] args) {
        Path folder = Paths.get("output_files/Master_problem");

        try {
            Files.list(folder)
                    .filter(path -> path.toString().endsWith(".jsonl"))
                    .sorted(Comparator.comparing(Path::toString, CalculateAverageTime.naturalComparator()))
                    .forEach(path -> {
                        Map<String, List<Double>> numericValues = new LinkedHashMap<>();
                        Map<String, List<Long>> timeValues = new LinkedHashMap<>();
                        for (String field : FIELDS_TO_ANALYZE) {
                            numericValues.put(field, new ArrayList<>());
                            timeValues.put(field, new ArrayList<>());
                        }

                        try (BufferedReader reader = Files.newBufferedReader(path)) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                for (String field : FIELDS_TO_ANALYZE) {
                                    if (line.contains("\"" + field + "\"")) {
                                        if (line.contains("h") && line.contains("ms") && line.contains("µs")) {
                                            long ns = extractElapsedNs(line, field);
                                            if (ns >= 0) timeValues.get(field).add(ns);
                                        } else {
                                            Double number = extractNumericValue(line, field);
                                            if (number != null) numericValues.get(field).add(number);
                                        }
                                    }
                                }
                            }
                        } catch (IOException e) {
                            System.err.println("Error reading file " + path + ": " + e.getMessage());
                            return;
                        }

                        System.out.println("======== " + path.getFileName() + " ========");
                        for (String field : FIELDS_TO_ANALYZE) {
                            if (!timeValues.get(field).isEmpty()) {
                                long avgNs = timeValues.get(field).stream().mapToLong(Long::longValue).sum() / timeValues.get(field).size();
                                System.out.printf("%-20s: %s (%d ns)%n", field, formatDuration(avgNs), avgNs);
                            } else if (!numericValues.get(field).isEmpty()) {
                                double avg = numericValues.get(field).stream().mapToDouble(Double::doubleValue).average().orElse(0);
                                System.out.printf("%-20s: %.3f%n", field, avg);
                            } else {
                                System.out.printf("%-20s: no data%n", field);
                            }
                        }
                        System.out.println();
                    });

        } catch (IOException e) {
            System.err.println("Failed to list files: " + e.getMessage());
        }
    }

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

    private static Double extractNumericValue(String line, String field) {
        String regex = "\"" + Pattern.quote(field) + "\"\\s*:\\s*([\\d\\.E+-]+)";
        Matcher matcher = Pattern.compile(regex).matcher(line);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static String formatDuration(long nanos) {
        long micros = (nanos / 1_000) % 1_000;
        long millis = (nanos / 1_000_000) % 1_000;
        long seconds = (nanos / 1_000_000_000) % 60;
        long minutes = (nanos / (60L * 1_000_000_000)) % 60;
        long hours = nanos / (60L * 60L * 1_000_000_000);

        return String.format("%02dh %02dm %02ds %03dms %03dµs",
                hours, minutes, seconds, millis, micros);
    }

    public static Comparator<String> naturalComparator() {
        return (s1, s2) -> {
            List<Object> k1 = extractNaturalKey(s1);
            List<Object> k2 = extractNaturalKey(s2);

            int len = Math.min(k1.size(), k2.size());
            for (int i = 0; i < len; i++) {
                Object o1 = k1.get(i);
                Object o2 = k2.get(i);

                int result;
                if (o1 instanceof Integer && o2 instanceof Integer) {
                    result = Integer.compare((Integer) o1, (Integer) o2);
                } else {
                    result = o1.toString().compareTo(o2.toString());
                }

                if (result != 0) return result;
            }

            return Integer.compare(k1.size(), k2.size());
        };
    }

    private static List<Object> extractNaturalKey(String s) {
        List<Object> key = new ArrayList<>();
        Matcher m = Pattern.compile("(\\d+)|(\\D+)").matcher(s);
        while (m.find()) {
            if (m.group(1) != null) {
                key.add(Integer.parseInt(m.group(1)));
            } else {
                key.add(m.group(2));
            }
        }
        return key;
    }
}
