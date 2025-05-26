package Masterprobleem;

import com.gurobi.gurobi.*;

import Masterprobleem.columnGen.ColumnGenerationHelper;
import Masterprobleem.columnGen.ShortestPathGenerator;
import Utils.InputHandler;
import Utils.OutputHandeler;
import Utils.PrintHandler;

import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        // ====================== Distance matrix =========================
        String fileName = "Data/Distances/NL6_distances.txt";
        // String fileName = "Data/Distances/NL16_distances.txt";

        InputHandler inputHandler = new InputHandler(fileName);
        int[][] distanceMatrix = inputHandler.getDistanceMatrix();
        int nTeams = distanceMatrix.length;
        int timeSlots = 2 * (nTeams - 1);

        PrintHandler printHandler = new PrintHandler();
        printHandler.printDistanceMatrixContents(distanceMatrix);

        ////////////////////////////////////////////////////////////////////////

        // ====================== Compacte Formulering =========================
        boolean DO_COMPACTE_FORMULERING = false;

        if (DO_COMPACTE_FORMULERING) {
            System.out.println(
                    "///////////////////////////////////////////////////////////////////////////////////////////");
            System.out.println("Compacte formulering solution");

            int upperbound = 3; // of een redelijke schatting
            GRBEnv env = new GRBEnv();

            CompactModel firstSolution_compact = new Masterprobleem.CompactModel(nTeams, timeSlots, distanceMatrix);
            firstSolution_compact.getFirstSolution();
            GRBVar[][][][] x = firstSolution_compact.getFirstSolution();

            // GRBVar[][][][] x = compact.getX();
            GRBModel model = firstSolution_compact.getModel();
            model.optimize();

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
        }

        // ====================== Initieel vullen van MasterProblem (first version)
        // =========================
        // Masterproblem master = new Masterproblem(new TourRepository(nTeams),
        // distanceMatrix);
        //
        // CompactModel compactModel = new
        // CompactModel(nTeams,timeSlots,distanceMatrix);
        // compactModel.getFirstSolution();
        // GRBVar[][][][] x = compactModel.getFirstSolution();
        //
        // for (int t = 0; t < nTeams; t++) {
        // List<Arc> arcs = new ArrayList<>();
        // double totalCost = 0.0;
        //
        // for (int s = 0; s < timeSlots + 1; s++) {
        // for (int i = 0; i < nTeams; i++) {
        // for (int j = 0; j < nTeams; j++) {
        // if (x[t][s][i][j].get(GRB.DoubleAttr.X) > 0.5) {
        // arcs.add(new Arc(s, i, j));
        // totalCost += distanceMatrix[i][j];
        // }
        // }
        // }
        // }
        //
        // Tour tour = new Tour(arcs, totalCost);
        // master.addTour(t, tour);
        // }
        // ====================== Initieel vullen van MasterProblem (first version)
        // =========================

        // ====================== Initieel vullen van MasterProblem (New version)
        // =========================
        Masterproblem master = new Masterproblem(new TourRepository(nTeams), distanceMatrix);

        CompactModel compactModel = new CompactModel(nTeams, timeSlots, distanceMatrix);
        List<GRBVar[][][][]> solutions = compactModel.getMultipleSolutions(1);

        for (GRBVar[][][][] xSol : solutions) {
            for (int t = 0; t < nTeams; t++) {
                List<Arc> arcs = new ArrayList<>();
                double totalCost = 0.0;

                for (int s = 0; s < timeSlots + 1; s++) {
                    for (int i = 0; i < nTeams; i++) {
                        for (int j = 0; j < nTeams; j++) {
                            if (xSol[t][s][i][j].get(GRB.DoubleAttr.Xn) > 0.5) {
                                arcs.add(new Arc(s, i, j));
                                totalCost += distanceMatrix[i][j];
                            }
                        }
                    }
                }

                Tour tour = new Tour(arcs, totalCost);
                master.addTour(t, tour); // This adds the new column
            }
        }

        // for (int team = 0; team < nTeams; team++) {
        // Tour original = master.getTourRepo().getAllTours().get(team).get(0);
        // Tour shifted = generateShiftedHomeGameTour(original, team, distanceMatrix);
        // master.addTour(team, shifted);
        // }

        // System.out.println("------------ Tours in MasterProblem (initieel)
        // -------------");
        //
        // Map<Integer, List<Tour>> allTours = master.getTourRepo().getAllTours();
        // for (Map.Entry<Integer, List<Tour>> entry : allTours.entrySet()) {
        // int team = entry.getKey();
        // System.out.println("Team " + team + ":");
        //
        // for (int tIndex = 0; tIndex < entry.getValue().size(); tIndex++) {
        // Tour tour = entry.getValue().get(tIndex);
        // System.out.println(" Tour " + tIndex + " (Cost: " + tour.cost + "):");
        //
        // for (Arc arc : tour.arcs) {
        // System.out.println(" Time " + arc.time + ": " + arc.from + " → " + arc.to);
        // }
        // }
        // }

        try {
            // ====================== MasterProblem oplossen =========================

            // Extract dual prices
            ColumnGenerationHelper relaxedModel_helper = new ColumnGenerationHelper();

            ShortestPathGenerator spg = ShortestPathGenerator.initializeSPG(nTeams, 3, timeSlots, distanceMatrix,
                    relaxedModel_helper);

            boolean positiveDuals = false;

            do {
                System.out.println("\n\n\nOplossen van het masterprobleem...");
                master.buildConstraints();

                // Relax to LP for dual prices
                System.out.println("\n\nRelaxing the model...");
                GRBModel relaxed = master.getModel().relax();
                master.setRelaxedModel(relaxed);
                relaxed.optimize();

                master.printLambda(false);

                //Extract Duals
                relaxedModel_helper.setModel(relaxed);
                relaxedModel_helper.extractDuals();
                //relaxedModel_helper.printDuals();

                // Check wehter to stop
                for (Double dual : relaxedModel_helper.getDualPrices().values()) {
                    if (dual > 0) {
                        positiveDuals = true;
                    }
                }

                for (int t = 0; t < nTeams; t++) {
                    Tour tour = spg.generateTour(t);
                    master.addTour(t, tour);
                }
            } while (positiveDuals);
            /*
             * long total = 0;
             * for (int i = 0; i < nTeams; i++) {
             * total = spg.times[i] + total;
             * }
             * System.err.println("avg: " + total / nTeams);
             * 
             * // test to get modified cost
             * // arguments: t, i, j, s, duals, distanceMatrix, numTeams
             * double test_cost = relaxedModel_helper.computeModifiedCost(1, 1, 2, 2,
             * distanceMatrix,
             * distanceMatrix.length);
             * System.out.println("\nMain:\n\tModified cost: " + test_cost);
             */

            // ====================== FINAL SOLUTION (IP) =========================
            // Get the integer model's solution
            // master.optimize();
            // Map<Integer, Tour> finalSolution = master.getSolution();
            //
            // System.out.println("------------ Geselecteerde tours in masteroplossing
            // -------------");
            //
            // for (Map.Entry<Integer, Tour> entry : finalSolution.entrySet()) {
            // int team = entry.getKey();
            // Tour tour = entry.getValue();
            // System.out.println("Team " + team + " (Totale kost: " + tour.cost + "):");
            // for (Arc arc : tour.arcs) {
            // System.out.println(" Tijd " + arc.time + ": " + arc.from + " → " + arc.to);
            // }
            // }

        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    public static Tour generateShiftedHomeGameTour(Tour original, int team, int[][] distanceMatrix) {
        List<Arc> arcs = original.arcs;
        int n = arcs.size();

        // Zoek tweede home game (arc.to == team)
        int homeCount = 0;
        int splitIndex = -1;
        for (int i = 0; i < n; i++) {
            if (arcs.get(i).to == team) {
                homeCount++;
                if (homeCount == 2) {
                    splitIndex = i;
                    break;
                }
            }
        }

        // Als we geen tweede home game vinden, geef originele tour terug
        if (splitIndex == -1)
            return original;

        List<Arc> firstPart = arcs.subList(splitIndex, n);
        List<Arc> secondPart = arcs.subList(0, splitIndex);

        List<Arc> newTourArcs = new ArrayList<>();

        // Herbouw tour met geüpdatete tijdstippen
        int newTime = 0;
        for (Arc arc : firstPart) {
            newTourArcs.add(new Arc(newTime++, arc.from, arc.to));
        }
        for (Arc arc : secondPart) {
            newTourArcs.add(new Arc(newTime++, arc.from, arc.to));
        }

        // Bereken nieuwe kost
        double cost = 0;
        for (Arc a : newTourArcs) {
            cost += distanceMatrix[a.from][a.to];
        }

        return new Tour(newTourArcs, cost);
    }

}
