package Dummy_Test;

import Masterprobleem.ColumnGenerationHelper;
import Utils.InputHandler;

import com.gurobi.gurobi.*;
import java.util.*;

public class DummyMasterProblem {

    public static void main(String[] args) {
        try {
            // Random
            String fileName = "Data/Distances/NL4_distances.txt";
            Utils.InputHandler inputHandler = new InputHandler(fileName);
            int[][] distanceMatrix = inputHandler.getDistanceMatrix();
            int nTeams = distanceMatrix.length;
            int timeSlots = 2 * (nTeams - 1) + 1;

            // Set up Gurobi environment
            GRBEnv env = new GRBEnv(true);
            env.set("logFile", "dummy_master.log");
            env.start();

            // Build initial dummy master problem
            GRBModel model = new GRBModel(env);

            int[] teams = {0, 1, 2, 3}; // NL4
            int[] slots = {0, 1, 2, 3, 4, 5}; // 6 slots

            // Dummy tours: one per team
            Map<Integer, List<String>> Pt = new HashMap<>();
            Map<String, Double> cost = new HashMap<>();
            Map<String, Integer[][]> playsHome = new HashMap<>();
            Map<String, Integer[][]> playsAway = new HashMap<>();

            for (int t : teams) {
                List<String> tours = new ArrayList<>();
                String tourName = "p0"; // just one tour
                tours.add(tourName);

                Pt.put(t, tours);

                cost.put(t + "_" + tourName, 100.0 + t * 10); // fake cost

                Integer[][] dummyHome = new Integer[slots.length][1];
                Integer[][] dummyAway = new Integer[slots.length][1];

                for (int s = 0; s < slots.length; s++) {
                    if (s % 2 == 0) { // even = home
                        dummyHome[s][0] = (t + 1) % 4;
                        dummyAway[s][0] = -1;
                    } else { // odd = away
                        dummyHome[s][0] = -1;
                        dummyAway[s][0] = (t + 2) % 4;
                    }
                }
                playsHome.put(t + "_" + tourName, dummyHome);
                playsAway.put(t + "_" + tourName, dummyAway);
            }

            // Variables
            Map<String, GRBVar> lambdaVars  = new HashMap<>();
            for (int t : teams) {
                for (String p : Pt.get(t)) {
                    String key = t + "_" + p;
                    GRBVar var = model.addVar(0.0, 1.0, cost.get(key), GRB.BINARY, "lambda_" + key);
                    lambdaVars.put(key, var);
                }
            }

            // Constraints: one tour per team
            for (int t : teams) {
                GRBLinExpr expr = new GRBLinExpr();
                for (String p : Pt.get(t)) {
                    expr.addTerm(1.0, lambdaVars.get(t + "_" + p));
                }
                model.addConstr(expr, GRB.EQUAL, 1.0, "one_tour_" + t);
            }

            // Constraints: one match per slot
            for (int t : teams) {
                for (int s : slots) {
                    GRBLinExpr expr = new GRBLinExpr();

                    for (int t2 : teams) {
                        if (t == t2) continue;

                        for (String p2 : Pt.get(t2)) {
                            Integer[][] awayInfo = playsAway.get(t2 + "_" + p2);
                            if (awayInfo != null && awayInfo[s][0] == t) {
                                expr.addTerm(1.0, lambdaVars.get(t2 + "_" + p2));
                            }
                        }

                        for (String p : Pt.get(t)) {
                            Integer[][] homeInfo = playsHome.get(t + "_" + p);
                            if (homeInfo != null && homeInfo[s][0] == t2) {
                                expr.addTerm(1.0, lambdaVars.get(t + "_" + p));
                            }
                        }
                    }

                    model.addConstr(expr, GRB.EQUAL, 1.0, "slot_" + s + "_team_" + t);
                }
            }

            // Solve the dummy master
            model.optimize();

            // Relax to LP for dual prices
            GRBModel relaxed = model.relax();
            relaxed.optimize();

            // Extract dual prices
            ColumnGenerationHelper relaxedModel = new ColumnGenerationHelper();
            relaxedModel.extractDuals();
            Map<String, Double> dualPrices = relaxedModel.getDualPrices();
            relaxedModel.printDuals();

            // test to get modified cost
            // arguments: t, i, j, s, duals, distanceMatrix, numTeams
            double test_cost = relaxedModel.computeModifiedCost(1, 1, 2, 2, distanceMatrix, 4);
            System.out.println("\nMain:\n\tModified cost: " + test_cost);

            model.dispose();
            relaxed.dispose();
            env.dispose();
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }
}
