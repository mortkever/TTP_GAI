package Utils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InputHandler {
    private int[][] distances;

    // Constructor to read from file and initialize the table
    public InputHandler(String fileName) {
        String dataType = fileName.substring(fileName.length() - 3);
        if(dataType.equals("txt")) {
            readDistanceMatrixFromTextFile(fileName);
        } else if (dataType.equals("xml")) {
            readDistanceMatrixFromXMLFile(fileName);
        }
    }

    private void readDistanceMatrixFromTextFile(String fileName) {
        try {
            List<int[]> rows = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String line;

            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    String[] values = line.trim().split("\\s+");
                    int[] row = new int[values.length];

                    for (int i = 0; i < values.length; i++) {
                        row[i] = Integer.parseInt(values[i]);
                    }
                    rows.add(row);
                } catch (NumberFormatException e) {
                    System.err.println("Error parsing number on line " + lineNumber + ": " + line);
                    throw e;  // rethrow to stop execution if needed
                }
            }

            reader.close();

            // Convert List to 2D Array
            distances = new int[rows.size()][];
            for (int i = 0; i < rows.size(); i++) {
                distances[i] = rows.get(i);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.err.println("Error parsing number from file.");
        }
    }

    private void readDistanceMatrixFromXMLFile(String fileName) {
        try {
            // Create StAX parser
            XMLInputFactory factory = XMLInputFactory.newInstance();
            FileInputStream fis = new FileInputStream(fileName);
            XMLStreamReader reader = factory.createXMLStreamReader(fis);

            // Store distances temporarily before determining array size
            List<int[]> distanceList = new ArrayList<>();
            int maxTeam = 0;

            // Parse the XML file
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamReader.START_ELEMENT && reader.getLocalName().equals("distance")) {
                    int dist = Integer.parseInt(reader.getAttributeValue(null, "dist"));
                    int team1 = Integer.parseInt(reader.getAttributeValue(null, "team1"));
                    int team2 = Integer.parseInt(reader.getAttributeValue(null, "team2"));

                    // Track maximum team number to size the 2D array
                    maxTeam = Math.max(maxTeam, Math.max(team1, team2));

                    // Store distance in a list
                    distanceList.add(new int[]{team1, team2, dist});
                }
            }
            reader.close();
            fis.close();

            // Initialize the 2D array
            distances = new int[maxTeam + 1][maxTeam + 1];

            // Populate the 2D array
            for (int[] entry : distanceList) {
                distances[entry[0]][entry[1]] = entry[2];
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Getter method to access the 2D array
    public int[][] getDistanceMatrix() {
        return distances;
    }
}

