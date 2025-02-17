import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // PrintHandler object to easily print
        PrintHandler printHandler = new PrintHandler();

        // ---------------------- Read input --------------------------
        // Path to input file
        String fileName = "Data/t4.txt";

        // Put the table in a 2D array
        InputHandler inputHandler = new InputHandler(fileName);
        int[][] distanceMatrix = inputHandler.getDistanceMatrixFromTXT();

        // Print the 2D array
        printHandler.printDistanceMatrixContents(distanceMatrix);


        // Voorbeeld code:
        List<Team> teams = Arrays.asList(
                new Team("Team 1"), new Team("Team 2"), new Team("Team 3"),
                new Team("Team 4"), new Team("Team 5"), new Team("Team 6")
        );

        Schedule schedule = new Schedule();

        schedule.addMatches(1, Arrays.asList(
                new Match(teams.get(0), teams.get(5)), // Team 1 vs Team 6
                new Match(teams.get(1), teams.get(4)), // Team 2 vs Team 5
                new Match(teams.get(2), teams.get(3))  // Team 3 vs Team 4
        ));

        schedule.addMatches(2, Arrays.asList(
                new Match(teams.get(0), teams.get(4)), // Team 1 vs Team 5
                new Match(teams.get(5), teams.get(3)), // Team 6 vs Team 4
                new Match(teams.get(1), teams.get(2))  // Team 2 vs Team 3
        ));

        schedule.addMatches(3, Arrays.asList(
                new Match(teams.get(0), teams.get(3)), // Team 1 vs Team 4
                new Match(teams.get(4), teams.get(2)), // Team 5 vs Team 3
                new Match(teams.get(5), teams.get(1))  // Team 6 vs Team 2
        ));

        // Stap 4: Schema printen
        schedule.printSchedule();

        // Stap 5: Specifieke ronde ophalen
        System.out.println("Wedstrijden in Ronde 2:");
        for (Match match : schedule.getMatches(2)) {
            System.out.println("  " + match);
        }
    }



}


