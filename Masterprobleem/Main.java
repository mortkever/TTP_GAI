package Masterprobleem;

import com.gurobi.gurobi.*;

import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {

        // ====================== Distance matrix =========================
        String fileName = "Data/Distances/NL4_distances.txt";
        // String fileName = "Data/Distances/NL16_distances.txt";

        InputHandler inputHandler = new InputHandler(fileName);
        int[][] distanceMatrix = inputHandler.getDistanceMatrix();
        int nTeams = distanceMatrix.length;
        int timeSlots = 2 * (nTeams - 1) + 1;

        PrintHandler printHandler = new PrintHandler();
        printHandler.printDistanceMatrixContents(distanceMatrix);

        ////////////////////////////////////////////////////////////////////////


        // ====================== Compacte Formulering =========================
        System.out.println("///////////////////////////////////////////////////////////////////////////////////////////");
        System.out.println("Compacte formulering solutie");

        int upperbound = 3;  // of een redelijke schatting
        GRBEnv env = new GRBEnv();
        CompactGurobiFormulation compact = new CompactGurobiFormulation(distanceMatrix, upperbound, env);
        GRBModel model = compact.getModel();
        model.optimize();

        GRBVar[][][][] x = compact.getX();

        if (model.get(GRB.IntAttr.Status) == GRB.OPTIMAL) {
            System.out.println("Oplossing gevonden");
            System.out.println("Totale afstand: " + model.get(GRB.DoubleAttr.ObjVal));
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

            OutputHandeler oh = new OutputHandeler();
            oh.output(x, nTeams, timeSlots, model.get(GRB.DoubleAttr.ObjVal));
        } else {
            System.out.println("Geen oplossing gevonden.");
        }

        // ====================== Initieel vullen van MasterProblem =========================
        Masterproblem master = new Masterproblem(new TourRepository(nTeams));

        for (int t = 0; t < nTeams; t++) {
            List<Arc> arcs = new ArrayList<>();
            double totalCost = 0.0;

            for (int s = 0; s < timeSlots; s++) {
                for (int i = 0; i < nTeams; i++) {
                    for (int j = 0; j < nTeams; j++) {
                        if (x[t][s][i][j].get(GRB.DoubleAttr.X) > 0.5) {
                            arcs.add(new Arc(s, i, j));
                            totalCost += distanceMatrix[i][j];
                        }
                    }
                }
            }

            Tour tour = new Tour(arcs, totalCost);
            master.addTour(t, tour);
        }

        System.out.println("------------ Tours in MasterProblem (initieel) -------------");

        Map<Integer, List<Tour>> allTours = master.getTourRepo().getAllTours();
        for (Map.Entry<Integer, List<Tour>> entry : allTours.entrySet()) {
            int team = entry.getKey();
            System.out.println("Team " + team + ":");

            for (int tIndex = 0; tIndex < entry.getValue().size(); tIndex++) {
                Tour tour = entry.getValue().get(tIndex);
                System.out.println("  Tour " + tIndex + " (Cost: " + tour.cost + "):");

                for (Arc arc : tour.arcs) {
                    System.out.println("    Time " + arc.time + ": " + arc.from + " → " + arc.to);
                }
            }
        }
        try {
            // ====================== MasterProblem oplossen =========================
            master.buildConstraints();
            master.optimize();

            Map<Integer, Tour> finalSolution = master.getSolution();

            System.out.println("------------ Geselecteerde tours in masteroplossing -------------");

            for (Map.Entry<Integer, Tour> entry : finalSolution.entrySet()) {
                int team = entry.getKey();
                Tour tour = entry.getValue();
                System.out.println("Team " + team + " (Totale kost: " + tour.cost + "):");
                for (Arc arc : tour.arcs) {
                    System.out.println("    Tijd " + arc.time + ": " + arc.from + " → " + arc.to);
                }
            }

        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

}
