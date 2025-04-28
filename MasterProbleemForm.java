import Dummy_Test.ColumnGenerationHelper;
import com.gurobi.gurobi.*;
import java.util.*;

public class MasterProbleemForm {

    public static void main(String[] args) {
        try {
            GRBEnv env = new GRBEnv(true);
            env.set("logFile", "ttp_master.log");
            env.start();

            GRBModel model = new GRBModel(env);

            // Sets
            int[] teams = {0, 1, 2, 3};
            int[] slots = {0, 1, 2, 3, 4, 5};

            // Tours per team
            Map<Integer, List<String>> Pt = new HashMap<>(); // team -> list of tour names
            Map<String, Double> cost = new HashMap<>();      // tourKey -> cost
            Map<String, Integer[][]> playsHome = new HashMap<>(); // tourKey -> [slot][opponent]
            Map<String, Integer[][]> playsAway = new HashMap<>(); // tourKey -> [slot][opponent]

            // TODO: populate Pt, cost, playsHome, playsAway

            // Variables
            Map<String, GRBVar> lambda = new HashMap<>();
            for (int t : teams) {
                for (String p : Pt.get(t)) {
                    String key = "lambda_" + t + "_" + p;
                    GRBVar var = model.addVar(0.0, 1.0, cost.get(t + "_" + p), GRB.BINARY, key);
                    lambda.put(t + "_" + p, var);
                }
            }

            // Constraint (10): one tour per team
            for (int t : teams) {
                GRBLinExpr expr = new GRBLinExpr();
                for (String p : Pt.get(t)) {
                    expr.addTerm(1.0, lambda.get(t + "_" + p));
                }
                model.addConstr(expr, GRB.EQUAL, 1.0, "one_tour_" + t);
            }

            // Constraint (9): one match per slot
            for (int t : teams) {
                for (int s : slots) {
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int t2 : teams) {
                        if (t == t2) continue;

                        for (String p2 : Pt.get(t2)) {
                            Integer[][] awayInfo = playsAway.get(t2 + "_" + p2);
                            if (awayInfo != null && awayInfo[s][0] == t) {
                                expr.addTerm(1.0, lambda.get(t2 + "_" + p2));
                            }
                        }

                        for (String p : Pt.get(t)) {
                            Integer[][] homeInfo = playsHome.get(t + "_" + p);
                            if (homeInfo != null && homeInfo[s][0] == t2) {
                                expr.addTerm(1.0, lambda.get(t + "_" + p));
                            }
                        }
                    }
                    model.addConstr(expr, GRB.EQUAL, 1.0, "slot_" + s + "_team_" + t);
                }
            }

            // Constraint (12): NRCs
            for (int t1 = 0; t1 < teams.length; t1++) {
                for (int t2 = t1 + 1; t2 < teams.length; t2++) {
                    for (int s = 0; s < slots.length - 1; s++) {
                        GRBLinExpr expr = new GRBLinExpr();

                        for (String p1 : Pt.get(t1)) {
                            Integer[][] home = playsHome.get(t1 + "_" + p1);
                            Integer[][] away = playsAway.get(t1 + "_" + p1);
                            if (home[s][0] == t2 && away[s + 1][0] == t2) {
                                expr.addTerm(1.0, lambda.get(t1 + "_" + p1));
                            }
                        }

                        for (String p2 : Pt.get(t2)) {
                            Integer[][] home = playsHome.get(t2 + "_" + p2);
                            Integer[][] away = playsAway.get(t2 + "_" + p2);
                            if (home[s][0] == t1 && away[s + 1][0] == t1) {
                                expr.addTerm(1.0, lambda.get(t2 + "_" + p2));
                            }
                        }

                        model.addConstr(expr, GRB.LESS_EQUAL, 1.0, "NRC_" + t1 + "_" + t2 + "_" + s);
                    }
                }
            }

            // Optimize
            model.optimize();
            model.write("ttp_master.lp");

            // Output
            for (Map.Entry<String, GRBVar> entry : lambda.entrySet()) {
                if (entry.getValue().get(GRB.DoubleAttr.X) > 0.5)
                    System.out.println(entry.getKey() + " = 1");
            }

            // Print the dual prices
            ColumnGenerationHelper cgHelper = new ColumnGenerationHelper(model);
            cgHelper.extractAndPrintDuals();

            model.dispose();
            env.dispose();

        } catch (GRBException e) {
            System.out.println("Error code: " + e.getErrorCode() + ". " + e.getMessage());
        }
    }
}

