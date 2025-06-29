package Masterprobleem;

import com.gurobi.gurobi.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompactModel {
    private GRBVar x[][][][];
    private GRBModel model;
    private int nTeams;
    private int timeSlots;

    public CompactModel(int nTeams, int timeSlots, int[][] distanceMatrix) throws GRBException {
        this.model = new GRBModel(new GRBEnv());
        this.nTeams = nTeams;
        this.timeSlots = timeSlots;

        int upperbound = 3;

        x = new GRBVar[nTeams][timeSlots + 1][nTeams][nTeams];
        for (int t = 0; t < nTeams; t++) {
            for (int s = 0; s < timeSlots + 1; s++) {
                for (int i = 0; i < nTeams; i++) {
                    for (int j = 0; j < nTeams; j++) {
                        x[t][s][i][j] = model.addVar(0, 1, distanceMatrix[i][j], GRB.BINARY,
                                "x( s " + s + ", i " + i + ", j " + j + ")");
                    }
                }
            }
        }

        // Contraint 2: Flow Conversion
        for (int t = 0; t < nTeams; t++) {
            for (int i = 0; i < nTeams; i++) {
                for (int s = 1; s < timeSlots + 1; s++) {

                    GRBLinExpr flow = new GRBLinExpr();
                    for (int j = 0; j < nTeams; j++) {
                        if (isArcA(t, s - 1, j, i, nTeams))
                            flow.addTerm(1, x[t][s - 1][j][i]); // Kwam van team j naar i op s-1
                    }

                    for (int j = 0; j < nTeams; j++) {
                        if (isArcA(t, s, i, j, nTeams))
                            flow.addTerm(-1, x[t][s][i][j]); // Vertrekt van i naar team j op s
                    }
                    model.addConstr(flow, GRB.EQUAL, 0, "flow_conservation(t=" + t + ",i=" + i + ",s=" + s + ")");

                }
            }
        }

        // Constraint 5 : force every team to play every time slot

        for (int t = 0; t < nTeams; t++) { // For each team
            for (int s = 1; s < timeSlots + 1; s++) { // For each timeslot
                GRBLinExpr constraint = new GRBLinExpr();

                // First summation: Flow from i to j at time t
                for (int i = 0; i < nTeams; i++) {
                    for (int j = 0; j < nTeams; j++) {
                        if (t != i) {
                            if (i != j || (t == i && i == j))
                                constraint.addTerm(1, x[t][s][i][j]);
                        }
                    }
                }

                // Second summation: Flow from other teams t' to same team s
                for (int t2 = 0; t2 < nTeams; t2++) {
                    if (t2 != t) { // Avoid duplicate assignment
                        for (int j = 0; j < nTeams; j++) {
                            constraint.addTerm(1, x[t2][s][t][j]); // Ensure valid indexing
                        }
                    }
                }

                // Add constraint: Exactly one match per team s
                model.addConstr(constraint, GRB.EQUAL, 1, "flow_constraint_s" + s + "_t" + t);
            }
        }

        // constraint 3 Alle andere teams éénmaal bezoeken
        for (int t = 0; t < nTeams; t++) {
            for (int i = 0; i < nTeams; i++) {
                if (i != t) {
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int s = 1; s < timeSlots + 1; s++) {
                        for (int j = 0; j < nTeams; j++) {
                            if (isArcA(t, s, i, j, nTeams))
                                expr.addTerm(1.0, x[t][s][i][j]);
                        }
                    }
                    model.addConstr(expr, '=', 1, "visitation_" + t + "_" + i);
                }
            }
        }

        // constraint 4 consecutive home games
        for (int t = 0; t < nTeams; t++) {
            for (int s = 1; s <= 2 * (nTeams - 1) - upperbound; s++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int i = 0; i < nTeams; i++) {
                    for (int j = 0; j < nTeams; j++) {
                        if (isArcB(t, s, i, j, nTeams)) {
                            for (int u = 0; u < upperbound; u++) {
                                expr.addTerm(1.0, x[t][s + u][i][j]);
                            }
                        }
                    }
                }
                model.addConstr(expr, GRB.LESS_EQUAL, upperbound - 1, "breaks_" + t);
            }
        }

        // constraint 7 No repeater constraint
        for (int s = 1; s < 2 * (nTeams - 1); s++) {
            for (int t = 0; t < nTeams - 1; t++) {
                for (int t_2 = t + 1; t_2 < nTeams; t_2++) {
                    GRBLinExpr expr = new GRBLinExpr();
                    expr.addTerm(1.0, x[t][s][t][t_2]);
                    expr.addTerm(1.0, x[t_2][s][t][t_2]);
                    expr.addTerm(1.0, x[t_2][s][t_2][t]);
                    expr.addTerm(1.0, x[t][s][t_2][t]);
                    model.addConstr(expr, GRB.LESS_EQUAL, 1, "NRC_" + s + t + t_2);
                }
            }
        }
    }

    public void optimize() throws GRBException {
        model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);

        model.optimize();
    }

    public GRBVar[][][][] getOptimalSolution() {
        try {
            model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);
            model.optimize();

            if (model.get(GRB.IntAttr.Status) == GRB.OPTIMAL) {
                return x;
            } else {
                System.out.println("No optimal solution found.");
                return null;
            }
        } catch (GRBException e) {
            e.printStackTrace();
            return null;
        }
    }

    public GRBVar[][][][] getFirstSolution() throws GRBException {
        model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);
        model.set(GRB.IntParam.SolutionLimit, 1);

        model.optimize();

        return x;
    }

    public List<double[][][][]> getMultipleSolutions(int maxSolutions) throws GRBException {
        model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);
        // model.set(GRB.IntParam.PoolSearchMode, 2); // Get diverse solutions
        model.set(GRB.IntParam.PoolSolutions, maxSolutions);
        model.set(GRB.IntParam.SolutionLimit, maxSolutions);
        model.optimize();

        int solCount = model.get(GRB.IntAttr.SolCount);
        List<double[][][][]> solutions = new ArrayList<>();

        for (int sol = 0; sol < Math.min(solCount, maxSolutions); sol++) {
            model.set(GRB.IntParam.SolutionNumber, sol);
            double[][][][] solVal = new double[x.length][x[0].length][x[0][0].length][x[0][0][0].length];

            for (int t = 0; t < x.length; t++) {
                for (int s = 0; s < x[t].length; s++) {
                    for (int i = 0; i < x[t][s].length; i++) {
                        for (int j = 0; j < x[t][s][i].length; j++) {
                            solVal[t][s][i][j] = x[t][s][i][j].get(GRB.DoubleAttr.Xn);
                        }
                    }
                }
            }

            solutions.add(solVal);
        }

        return solutions;
    }

    // public GRBVar[][][][] getWorstSolution() throws GRBException {
    // model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE); // Keep this as MINIMIZE
    // model.set(GRB.IntParam.PoolSearchMode, 2); // Get diverse feasible solutions
    // model.set(GRB.IntParam.PoolSolutions, 10); // Up to 10 solutions
    //
    // model.optimize();
    //
    // int solCount = model.get(GRB.IntAttr.SolCount);
    // if (solCount == 0) {
    // throw new GRBException("No feasible solutions found.");
    // }
    //
    // int worstIndex = 0;
    // double worstObj = model.get(GRB.DoubleAttr.PoolObjVal); // ObjVal of first
    // solution
    //
    // // Loop to find the worst (highest) objective
    // for (int i = 1; i < solCount; i++) {
    // double val = model.get(GRB.DoubleAttr.PoolObjVal, i);
    // if (val > worstObj) {
    // worstObj = val;
    // worstIndex = i;
    // }
    // }
    //
    // // Set model to return values from worst solution
    // model.set(GRB.IntParam.SolutionNumber, worstIndex);
    //
    // return x; // x contains the same variables; just read their Xn values
    // externally
    // }

    public GRBModel getModel() {
        return model;
    }

    public static boolean isArcA(int t, int s, int i, int j, int nTeams) {
        boolean one = (t == i && s == 0);
        boolean two = (j == t && s == (2 * (nTeams - 1)));
        boolean three = (i != j || (i == t && i == j)) && s != (2 * (nTeams - 1)) && s != 0;
        return one || two || three;
    }

    public static boolean isArcB(int t, int s, int i, int j, int nTeams) {
        boolean one = (t == i && t == j && s != 0 && s != (2 * (nTeams - 1)));
        boolean two = (j != t && t != i);
        return (one || two) && isArcA(t, s, i, j, nTeams);
    }

    public void printCompact() {

        try {
            System.out.println("Initiele oplossing");
            System.out.println("Totale afstand: " + model.get(GRB.DoubleAttr.ObjVal));
            for (int t = 0; t < nTeams; t++) {
                for (int s = 0; s < timeSlots + 1; s++) {
                    for (int i = 0; i < nTeams; i++) {
                        for (int j = 0; j < nTeams; j++) {
                            if (x[t][s][i][j].get(GRB.DoubleAttr.X) > 0.5) {
                                System.out.println("Team " + t + " moved from " + i + " to " + j + " at time " + s);
                            }
                        }
                    }
                }
            }
        } catch (GRBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
