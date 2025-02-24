import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // PrintHandler object to easily print
        PrintHandler printHandler = new PrintHandler();

        // ---------------------- Read input --------------------------
        // Path to input file
        String fileName = "Data/NL4.xml";

        // Put the table in a 2D array
        InputHandler inputHandler = new InputHandler(fileName);
        int[][] distanceMatrix = inputHandler.getDistanceMatrix();

        // Print the 2D array
        printHandler.printDistanceMatrixContents(distanceMatrix);


        // ---------------------- Voorbeeld code --------------------------
        Schedule schedule = new Schedule();
        schedule.addFeasibleSchedule();

        // Stap 4: Schema printen
        schedule.printSchedule();

        // Stap 5: Specifieke ronde ophalen
        System.out.println("Wedstrijden in Ronde 2:");
        for (Match match : schedule.getMatches(2)) {
            System.out.println("  " + match);
        }

        // Stap 6: Validate solution
        ScheduleValidator scheduleValidator = new ScheduleValidator(schedule);
        scheduleValidator.validate();
    }



}


