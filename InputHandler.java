import java.io.*;
import java.util.*;

public class InputHandler {
    private int[][] table;

    // Constructor to read from file and initialize the table
    public InputHandler(String fileName) {
        readDistanceMatrixFromTextFile(fileName);
    }

    private void readDistanceMatrixFromTextFile(String fileName) {
        try {
            List<int[]> rows = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String line;

            while ((line = reader.readLine()) != null) {
                // Split line by whitespace (tabs or spaces)
                String[] values = line.trim().split("\\s+");
                int[] row = new int[values.length];

                for (int i = 0; i < values.length; i++) {
                    row[i] = Integer.parseInt(values[i]);
                }
                rows.add(row);
            }
            reader.close();

            // Convert List to 2D Array
            table = new int[rows.size()][];
            for (int i = 0; i < rows.size(); i++) {
                table[i] = rows.get(i);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.err.println("Error parsing number from file.");
        }
    }

    // Getter method to access the 2D array
    public int[][] getDistanceMatrixFromTXT() {
        return table;
    }
}
