package Masterprobleem;

import com.gurobi.gurobi.*;

public class CompactModel {
    private GRBVar x[][][][];
    private GRBModel model;

    public CompactModel(int nTeams, int timeSlots, int[][] distanceMatrix) throws GRBException {
        this.model = new GRBModel(new GRBEnv());

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

    public GRBVar[][][][] getFirstSolution() throws GRBException {
        model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);
        model.set(GRB.IntParam.SolutionLimit, 1);

        model.optimize();

        return x;
    }

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
}
