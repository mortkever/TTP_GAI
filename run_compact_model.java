import Masterprobleem.CompactModel;
import Utils.InputHandler;
import Utils.OutputHandeler;
import Utils.PrintHandler;
import com.gurobi.gurobi.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class run_compact_model {
    // Convert nanoseconds to human-readable string
    public static String formatDuration(long nanos) {
        long micros = (nanos / 1_000) % 1_000;
        long millis = (nanos / 1_000_000) % 1_000;
        long seconds = (nanos / 1_000_000_000) % 60;
        long minutes = (nanos / (60L * 1_000_000_000)) % 60;
        long hours = nanos / (60L * 60L * 1_000_000_000);

        return String.format("%02dh %02dm %02ds %03dms %03dµs",
                hours, minutes, seconds, millis, micros);
    }

    // Converts a Map to a compact JSON string (basic implementation)
    public static String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sb.append("\"").append(entry.getKey()).append("\": ");
            if (entry.getValue() instanceof Number) {
                sb.append(entry.getValue());
            } else {
                sb.append("\"").append(entry.getValue()).append("\"");
            }
            sb.append(", ");
        }
        if (!map.isEmpty()) sb.setLength(sb.length() - 2); // Remove trailing comma
        sb.append("}");
        return sb.toString();
    }

    public static void writeResultsToJsonl(String filename, List<Map<String, Object>> results, boolean append) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename, append))) {
            for (Map<String, Object> result : results) {
                writer.println(mapToJson(result));
            }
        } catch (IOException e) {
            System.err.println("Error writing to JSONL file: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        // ====================== Distance matrix =========================
        String inputLabel = "NL6"; // e.g., NL4, NL6, NL8
        String fileName = "Data/Distances/" + inputLabel + "_distances.txt";
        boolean append = true; // false = overwrite, true = append

        InputHandler inputHandler = new InputHandler(fileName);
        int[][] distanceMatrix = inputHandler.getDistanceMatrix();
        int nTeams = distanceMatrix.length;
        int timeSlots = 2 * (nTeams - 1);

        PrintHandler printHandler = new PrintHandler();
        printHandler.printDistanceMatrixContents(distanceMatrix);

        ////////////////////////////////////////////////////////////////////////
        // ====================== Compacte Formulering =========================
        System.out.println(
                "///////////////////////////////////////////////////////////////////////////////////////////");
        System.out.println("Compacte formulering solution");

        int amount_of_runs = 1; // Number of runs for benchmarking
        List<Map<String, Object>> results = new ArrayList<>();

        for (int run = 0; run < amount_of_runs; run++) {
            System.out.println("\n\n======================== Run " + run + " ========================");
            long startTime = System.nanoTime();     // Start timer

            CompactModel compactModel = new Masterprobleem.CompactModel(nTeams, timeSlots, distanceMatrix);
            compactModel.getOptimalSolution();

            // Calculate elapsed time
            long endTime = System.nanoTime();       // End timer
            long durationNs = endTime - startTime;

            Map<String, Object> runData = new LinkedHashMap<>();
            runData.put("run", run);
            runData.put("duration_ns", durationNs);
            runData.put("duration_formatted", formatDuration(durationNs));
            runData.put("input", inputLabel);
            runData.put("amount_of_runs", amount_of_runs);
            results.add(runData);

            // Clean up
            compactModel.getModel().dispose();
        }

        String jsonlFileName = "output_files/compact_model/" + inputLabel + "-times.jsonl";
        writeResultsToJsonl(jsonlFileName, results, append);
        System.out.println("Benchmarking complete. Results written to " + jsonlFileName);
    }
}
