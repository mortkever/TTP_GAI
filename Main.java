import com.gurobi.gurobi.*;

public class Main {
    public static void main(String[] args) throws GRBException {
        int upperbound = 3;
        PrintHandler printHandler = new PrintHandler();

        // String fileName = "Data/NL4.xml";
        String fileName = "Data/Distances/NL6_distances.txt";
        // String fileName = "Data/Distances/NL16_distances.txt";

        // ====================== Distance matrix =========================
        InputHandler inputHandler = new InputHandler(fileName);
        int[][] distanceMatrix = inputHandler.getDistanceMatrix();
        int nTeams = distanceMatrix.length;
        int timeSlots = 2 * (nTeams - 1) + 1;
        printHandler.printDistanceMatrixContents(distanceMatrix);

        // ====================== Gurobi ============================
        System.out.println("======================== Gurobi ============================");
        GRBModel model = new GRBModel(new GRBEnv());
        model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);

        // -----------------------------------------------------------------------------------------------------------
        // variables
        // -----------------------------------------------------------------------------------------------------------
        GRBLinExpr doelstelling = new GRBLinExpr();

        GRBVar x[][][][] = new GRBVar[nTeams][timeSlots + 1][nTeams][nTeams];
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
            for (int s = 1; s < timeSlots; s++) { // For each timeslot
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
                    for (int s = 1; s < timeSlots; s++) {
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

        model.optimize();

        if (model.get(GRB.IntAttr.Status) == GRB.OPTIMAL) {
            System.out.println("oplossing gevonden");
            System.out.println("Objective value total Distance: " + model.get(GRB.DoubleAttr.ObjVal));

            System.out.println("\nMatch Schedule:");
            for (int t = 0; t < nTeams; t++) {
                for (int s = 0; s < timeSlots; s++) {
                    for (int i = 0; i < nTeams; i++) {
                        for (int j = 0; j < nTeams; j++) {
                            if (x[t][s][i][j].get(GRB.DoubleAttr.X) > 0.5) { // Alleen actieve variabelen tonen
                                System.out.println("Team " + t + " moved from " + i + " to " + j + " at time " + s);
                            }
                        }
                    }
                }
            }

        } else {
            System.out.println("geen oplossing gevonden. ");
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
        // System.out.println("Wedstrijden in Ronde 2:");
        // for (Match match : schedule.getMatches(2)) {
        // System.out.println(" " + match);
        // }

        // Stap 6: Validate solution
        ScheduleValidator scheduleValidator = new ScheduleValidator(schedule, distanceMatrix);
        scheduleValidator.validate();
        // ================================================================

    }

    public static boolean isArcA(int t, int s, int i, int j, int nTeams) {
        boolean one = (t == i && s == 0);
        boolean two = (j == t && s == (2 * (nTeams - 1) + 1));
        boolean three = (i != j || (i == t && i == j)) && s != (2 * (nTeams - 1) + 1) && s != 0;
        return one || two || three;
    }

    public static boolean isArcB(int t, int s, int i, int j, int nTeams) {
        boolean one = (t == i && t == j && s != 0 && s != 2 * (nTeams - 1));
        boolean two = (j != t && t != i);
        return (one || two) && isArcA(t, s, i, j, nTeams);
    }
}