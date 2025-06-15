package Masterprobleem;

import Things.Schedule;
import Utils.ScheduleValidator;
import com.gurobi.gurobi.*;

import Masterprobleem.columnGen.ShortestPathGenerator;
import Utils.InputHandler;
import Utils.OutputHandeler;
import Utils.PrintHandler;

import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        // ====================== Distance matrix =========================
        long start = System.nanoTime();

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

        // ====================== Initieel vullen van MasterProblem
        // =========================
        // Different strategies:
        // 1. Add 1 compact formulation solution
        // 2. Add multiple compact formulation solutions
        // 3. Add super columns
        // 4. Add multiple super columns
        // 5. Add 1 solution of compact formulation and 1 solution of the super columns
        int strategieInitiele = 1;

        Masterproblem master = new Masterproblem(new TourRepository(nTeams), distanceMatrix);
        ColumnGenerationHelper relaxedModel_helper = new ColumnGenerationHelper();

        ShortestPathGenerator spg = ShortestPathGenerator.initializeSPG(
                nTeams, 3, timeSlots, distanceMatrix, relaxedModel_helper);

        try {
            ColumnGenerationHelper.addInitialSolution(strategieInitiele, master, spg, nTeams, timeSlots,
                    distanceMatrix);
        } catch (GRBException e) {
            e.printStackTrace();
            System.err.println("Failed to initialize master with initial strategy.");
        }

        try {
            // ====================== MasterProblem oplossen =========================
            System.out.println("\n\n---------------------------------");
            System.out.println("Masterprobleem start:");

            // Extract dual prices
            int counter = 0;
            int exisingTours = 0;
            int optimalTours = 0;
            relaxedModel_helper.setRandCost(false);

            do {
                long startCycle = System.nanoTime();
                master.buildConstraints();
                System.out.println("Tijdsduur constraints build (ms): " + (System.nanoTime() - startCycle) / 1000000);

                // Relax to LP for dual prices
                GRBModel relaxed = master.getModel().relax();
                relaxed.optimize();
                master.setRelaxedModel(relaxed);

                // master.printLambda(false);

                // Extract Duals
                relaxedModel_helper.setModel(relaxed);
                relaxedModel_helper.extractDuals();
                // relaxedModel_helper.printDuals();

                System.out.println("Obj: " + relaxed.get(GRB.DoubleAttr.ObjVal));

                System.out.println("Tijdsduur master (ms): " + (System.nanoTime() - startCycle) / 1000000);

                exisingTours = 0;
                optimalTours = 0;
                double maxNumber = 0.12;
                for (int t = 0; t < nTeams; t++) {
                    spg.generateAllTours(t, maxNumber);
                    if (spg.tours.size() > 0) {
                        for (Tour tour : spg.tours) {
                            exisingTours += master.addTour(t, tour);
                        }
                    } else {
                        optimalTours++;
                    }
                }

                counter++;
                System.out.println("Iteratie: " + counter);
                System.out.println("Tijdsduur (ms): " + (System.nanoTime() - startCycle) / 1000000);

            } while (optimalTours < nTeams);
            master.printLambda(false);

            Map<Integer, HashMap<Tour, GRBVar>> lambdaVars = master.getLambdaVars();

            for (Map.Entry<Integer, HashMap<Tour, GRBVar>> entry : lambdaVars.entrySet()) {
                Tour bestTour = null;
                double bestVal = -1;
                try {
                    // First check if the model has been optimized successfully
                    if (master.getModel().get(GRB.IntAttr.Status) == GRB.OPTIMAL) {
                        for (Map.Entry<Tour, GRBVar> e : entry.getValue().entrySet()) {
                            GRBVar var = e.getValue();
                            if (var != null) {
                                double val = var.get(GRB.DoubleAttr.X);
                                if (val > bestVal) {
                                    bestVal = val;
                                    bestTour = e.getKey();
                                }
                            }
                        }
                        if (bestTour != null) {
                            entry.getValue().get(bestTour).set(GRB.DoubleAttr.Start, 1.0);
                        }
                    }
                } catch (GRBException e) {
                    System.err.println("Error accessing variable value: " + e.getMessage());
                    continue;
                }
            }

            System.out.println("\n\n-------------------------------");
            System.out.println("Masterprobleem eind LP model:");
            System.out.println("Optimal tours: " + optimalTours);
            System.out.println("Existing tours: " + exisingTours);

            System.out.println("\n\n-------------------------------");
            master.getModel().set(GRB.DoubleParam.MIPGap, 0.01);
            master.getModel().set(GRB.IntParam.MIPFocus, 1);

            master.getModel().set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);
            master.getModel().optimize();

            int status = master.getModel().get(GRB.IntAttr.Status);

            if (status == GRB.Status.OPTIMAL) {
                System.out.println("Optimal solution found");
                System.out.println("Total cost: " + master.getModel().get(GRB.DoubleAttr.ObjVal));

                System.out.println("\n🟢 Geselecteerde tours in de optimale oplossing:");

                for (Map.Entry<Integer, HashMap<Tour, GRBVar>> entry : lambdaVars.entrySet()) {
                    int team = entry.getKey();
                    for (Map.Entry<Tour, GRBVar> tourEntry : entry.getValue().entrySet()) {
                        double val = tourEntry.getValue().get(GRB.DoubleAttr.X);
                        if (val > 0.5) {
                            System.out.println("Team " + team + ": " + tourEntry.getKey() + " " + val);
                            break; // er is maar 1 tour geselecteerd per team
                        }
                    }

                }

            } else {
                System.out.println("No optimal solution found");
            }

        } catch (GRBException e) {
            e.printStackTrace();
        }

        System.out.println("Tijdsduur (ms): " + (System.nanoTime() - start) / 1000000);

    }

    //
    // public static Tour generateShiftedHomeGameTour(Tour original, int team,
    // int[][] distanceMatrix) {
    // List<Arc> arcs = original.getArcs();
    // int n = arcs.size();
    //
    // // Zoek tweede home game (arc.to == team)
    // int homeCount = 0;
    // int splitIndex = -1;
    // for (int i = 0; i < n; i++) {
    // if (arcs.get(i).to == team) {
    // homeCount++;
    // if (homeCount == 2) {
    // splitIndex = i;
    // break;
    // }
    // }
    // }
    //
    // // Als we geen tweede home game vinden, geef originele tour terug
    // if (splitIndex == -1)
    // return original;
    //
    // List<Arc> firstPart = arcs.subList(splitIndex, n);
    // List<Arc> secondPart = arcs.subList(0, splitIndex);
    //
    // List<Arc> newTourArcs = new ArrayList<>();
    //
    // // Herbouw tour met geüpdatete tijdstippen
    // int newTime = 0;
    // for (Arc arc : firstPart) {
    // newTourArcs.add(new Arc(newTime++, arc.from, arc.to));
    // }
    // for (Arc arc : secondPart) {
    // newTourArcs.add(new Arc(newTime++, arc.from, arc.to));
    // }
    //
    // // Bereken nieuwe kost
    // double cost = 0;
    // for (Arc a : newTourArcs) {
    // cost += distanceMatrix[a.from][a.to];
    // }
    //
    // return new Tour(newTourArcs, cost);
    // }
    //
    // public static boolean deepEquals(double[][][][] a, double[][][][] b) {
    // if (a.length != b.length)
    // return false;
    // for (int i = 0; i < a.length; i++) {
    // if (a[i].length != b[i].length)
    // return false;
    // for (int j = 0; j < a[i].length; j++) {
    // if (a[i][j].length != b[i][j].length)
    // return false;
    // for (int k = 0; k < a[i][j].length; k++) {
    // if (a[i][j][k].length != b[i][j][k].length)
    // return false;
    // for (int l = 0; l < a[i][j][k].length; l++) {
    // if (Double.compare(a[i][j][k][l], b[i][j][k][l]) != 0)
    // return false;
    // }
    // }
    // }
    // }
    // return true;
    // }

}
