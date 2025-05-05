import com.gurobi.gurobi.*;

public class Main {
    public static void main(String[] args) throws GRBException {
        int upperbound = 3;
        PrintHandler printHandler = new PrintHandler();

        // String fileName = "Data/NL4.xml";
        String fileName = "Data/Distances/NL4_distances.txt";
        // String fileName = "Data/Distances/NL16_distances.txt";

        // ====================== Distance matrix =========================
        InputHandler inputHandler = new InputHandler(fileName);
        int[][] distanceMatrix = inputHandler.getDistanceMatrix();
        int nTeams = distanceMatrix.length;
        int timeSlots = 2 * (nTeams - 1) + 1;
        printHandler.printDistanceMatrixContents(distanceMatrix);

        // ====================== Gurobi ============================
        System.out.println("======================== Gurobi ============================");
        // Set up model
        GRBEnv env = new GRBEnv();
        CompactGurobiFormulation compact = new CompactGurobiFormulation(distanceMatrix, upperbound, env);

        // Solve
        GRBModel model = compact.getModel();
        model.optimize();

        // Output
        GRBVar[][][][] x = compact.getX();
        if (model.get(GRB.IntAttr.Status) == GRB.OPTIMAL) {
            System.out.println("oplossing gevonden");
            System.out.println("Objective value total Distance: " + model.get(GRB.DoubleAttr.ObjVal));
            for (int t = 0; t < nTeams; t++) {
                for (int s = 0; s < timeSlots; s++) {
                    for (int i = 0; i < nTeams; i++) {
                        for (int j = 0; j < nTeams; j++) {
                            if (x[t][s][i][j].get(GRB.DoubleAttr.X) > 0.5) {
                                System.out.println("Team " + t + " moved from " + i + " to " + j + " at time " + s);
                            }
                        }
                    }
                }
            }
        } else {
            System.out.println("geen oplossing gevonden.");
        }

        OutputHandeler oh = new OutputHandeler();
        try {
            oh.output(x, nTeams, timeSlots, model.get(GRB.DoubleAttr.ObjVal));
        } catch (Exception e) {
            e.printStackTrace();
        }

        Schedule schedule = Schedule.loadScheduleFromXML("output.xml");
        // Schedule schedule =
        // Schedule.loadScheduleFromXML("Data/Solutions/NL16_Best_Solution_Broken.xml");

        // Stap 4: Schema printen
        schedule.printSchedule();

        // Stap 5: Specifieke ronde ophalen
        System.out.println("Wedstrijden in Ronde 2:");
        for (int ronde = 1; ronde <= 4; ronde++) {
            System.out.println("Wedstrijden in Ronde " + ronde + ":");
            for (Match match : schedule.getMatches(ronde)) {
                System.out.println(" " + match);
            }
        }
        // Stap 6: Validate solution
        ScheduleValidator scheduleValidator = new ScheduleValidator(schedule, distanceMatrix);
        scheduleValidator.validate();
        // ================================================================

    }

    // ==================== Branch & Price ====================





}