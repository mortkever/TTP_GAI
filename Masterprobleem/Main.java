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

        // ====================== Initieel vullen van MasterProblem
        // =========================
        int strategieInitiele = 2;
        Masterproblem master = null;
        CompactModel compactModel;

        if (strategieInitiele == 1) {
            master = new Masterproblem(new TourRepository(nTeams), distanceMatrix);

            compactModel = new CompactModel(nTeams, timeSlots, distanceMatrix);
            compactModel.getFirstSolution();
            GRBVar[][][][] x = compactModel.getFirstSolution();

            for (int t = 0; t < nTeams; t++) {
                List<Arc> arcs = new ArrayList<>();
                double totalCost = 0.0;

                for (int s = 0; s < timeSlots + 1; s++) {
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
        } else if (strategieInitiele == 2) {
            master = new Masterproblem(new TourRepository(nTeams), distanceMatrix);

            compactModel = new CompactModel(nTeams, timeSlots, distanceMatrix);
            List<double[][][][]> solutions = compactModel.getMultipleSolutions(5);

            for (double[][][][] xSol : solutions) {
                for (int t = 0; t < nTeams; t++) {
                    List<Arc> arcs = new ArrayList<>();
                    double totalCost = 0.0;

                    for (int s = 0; s < timeSlots + 1; s++) {
                        for (int i = 0; i < nTeams; i++) {
                            for (int j = 0; j < nTeams; j++) {
                                if (xSol[t][s][i][j] > 0.5) {
                                    arcs.add(new Arc(s, i, j));
                                    totalCost += distanceMatrix[i][j];
                                }
                            }
                        }
                    }

                    Tour tour = new Tour(arcs, totalCost);
                    System.out.println(tour);
                    master.addTour(t, tour); // This adds the new column
                }
            }
        } else if (strategieInitiele == 3) {
            for (int team = 0; team < nTeams; team++) {
                for (int variant = 0; variant < 2; variant++) {
                    List<Arc> arcs = new ArrayList<>();
                    double cost = 10000 + variant; // Make sure it's high but unique

                    // Fake tour logic: for example, loop around fixed cities
                    for (int s = 0; s < timeSlots; s++) {
                        int from = team;
                        int to = (team + s + variant + 1) % nTeams;
                        if (from == to)
                            to = (to + 1) % nTeams; // Avoid self-play

                        arcs.add(new Arc(s, from, to));
                        cost += distanceMatrix[from][to];
                    }

                    Tour fakeTour = new Tour(arcs, cost);
                    master.addTour(team, fakeTour);
                }
            }

        }

        try {
            // ====================== MasterProblem oplossen =========================

            // Extract dual prices
            ColumnGenerationHelper relaxedModel_helper = new ColumnGenerationHelper();

            ShortestPathGenerator spg = ShortestPathGenerator.initializeSPG(nTeams, 3, timeSlots, distanceMatrix,
                    relaxedModel_helper);

            double prevVal = Double.MAX_VALUE;
            int counter = 0;
            int exisingTours = 0;

            do {
                System.out.println("\nOplossen van het masterprobleem...");
                master.buildConstraints();

                // Relax to LP for dual prices
                System.out.println("\n\nRelaxing the model...");
                GRBModel relaxed = master.getModel().relax();
                relaxed.optimize();
                int status = relaxed.get(GRB.IntAttr.Status);
                System.out.println("Status: " + status);
                master.setRelaxedModel(relaxed);

                for (GRBVar var : relaxed.getVars()) {
                    System.out.println(var.get(GRB.StringAttr.VarName) +
                            " type=" + var.get(GRB.CharAttr.VType) +
                            " value=" + var.get(GRB.DoubleAttr.X));
                }

                master.printLambda(false);

                // Extract Duals
                relaxedModel_helper.setModel(relaxed);
                relaxedModel_helper.extractDuals();
                // relaxedModel_helper.printDuals();

                // Check wether to stop
                if (relaxed.get(GRB.DoubleAttr.ObjVal) == prevVal) {
                    counter++;
                } else {
                    counter = 0;
                }
                prevVal = relaxed.get(GRB.DoubleAttr.ObjVal);
                System.out.println("Obj: " + relaxed.get(GRB.DoubleAttr.ObjVal) + "\n");

                exisingTours = 0;
                for (int t = 0; t < nTeams; t++) {
                    Tour tour = spg.generateTour(t);
                    exisingTours += master.addTour(t, tour);
                }
                System.err.println(counter);

            } while (exisingTours < nTeams);

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
