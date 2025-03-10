import com.gurobi.gurobi.*;

public class Main {
    public static void main(String[] args) throws GRBException {

        String fileName = "Data/Distances/NL4_distances.txt";
        int upperbound = 3;

        // Put the table in a 2D array
        InputHandler inputHandler = new InputHandler(fileName);
        int[][] distanceMatrix = inputHandler.getDistanceMatrix();
        int nTeams = distanceMatrix.length;
        int timeSlots = 2 * nTeams;

        // Print the 2D array
        PrintHandler printHandler = new PrintHandler();
        printHandler.printDistanceMatrixContents(distanceMatrix);

        // ---------------------- Voorbeeld code --------------------------
        Schedule schedule = Schedule.loadScheduleFromXML("Data/Solutions/NL4_Optimal_Solution.xml");

        // Stap 4: Schema printen
        schedule.printSchedule();

        // Stap 5: Specifieke ronde ophalen
        System.out.println("Wedstrijden in Ronde 2:");
        for (Match match : schedule.getMatches(2)) {
            System.out.println("  " + match);
        }

        // Stap 6: Validate solution
        ScheduleValidator scheduleValidator = new ScheduleValidator(schedule, distanceMatrix);
        scheduleValidator.validate();

        GRBModel model = new GRBModel(new GRBEnv());
        model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);

        // -----------------------------------------------------------------------------------------------------------
        // variables
        // -----------------------------------------------------------------------------------------------------------
        GRBLinExpr doelstelling = new GRBLinExpr();

        GRBVar x[][][][] = new GRBVar[nTeams][timeSlots][nTeams][nTeams];
        for (int t = 0; t < nTeams; t++) {
            for (int s = 0; s < timeSlots; s++) {
                for (int i = 0; i < nTeams; i++) {
                    for (int j = 0; j < nTeams; j++) {
                        x[t][s][i][j] = model.addVar(0, 1, 0, GRB.BINARY,
                                "x( s " + s + ", i " + i + ", j " + j + ")");
                        doelstelling.addTerm((double) distanceMatrix[i][j], x[t][s][i][j]);
                    }
                }
            }
        }

        // Contrait 2: FlowConversion
        for (int t = 0; t < nTeams; t++) {
            for (int i = 0; i < nTeams; nTeams++) {
                for (int s = 1; s < timeSlots; timeSlots++) {

                    GRBLinExpr flow = new GRBLinExpr();

                    for (int j = 0; j < nTeams; j++) {
                        if (i != j) {
                            flow.addTerm(1, x[t][s - 1][j][i]); // Kwam van team j naar i op s-1
                        }
                    }

                    for (int j = 0; j < nTeams; j++) {
                        if (i != j) {
                            flow.addTerm(-1, x[t][s][i][j]); // Vertrekt van i naar team j op s
                        }
                    }
                    model.addConstr(flow, GRB.EQUAL, 0, "flow_conservation(t=" + t + ",i=" + i + ",s=" + s + ")");

                }
            }
        }

        // constraint 3
        for (int t = 0; t < nTeams; t++) {
            for (int i = 0; i < nTeams; i++) {
                if (i != t) {
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int s = 0; s < timeSlots; s++) {
                        for (int j = 0; j < nTeams; j++) {
                            expr.addTerm(1.0, x[t][s][i][j]);
                        }
                    }
                    model.addConstr(expr, '=', 1, "visitation_" + t + "_" + i);
                }
            }
        }

        // constraint 4 consecutive home games
        for (int t = 0; t < nTeams; t++) {
            for (int s = 0; s < timeSlots - upperbound; s++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int u = 0; u < upperbound; u++) {
                    expr.addTerm(1.0, x[t][s + u][t][t]);
                }
                model.addConstr(expr, '<', upperbound, "breaks_" + t);
            }
        }

        // Constraint 5 :
        for (int s = 0; s < nTeams; s++) { // Voor elk team
            for (int t = 0; t < timeSlots; t++) { // Voor elk tijdslot
                GRBLinExpr constraint = new GRBLinExpr();

                // Eerste som: Flow die vertrekt van i naar j op tijdstip t
                for (int i = 0; i < nTeams; i++) {
                    for (int j = 0; j < nTeams; j++) {
                        constraint.addTerm(1, x[t][s][i][j]);
                    }
                }

                // Tweede som: Flow vanuit andere tijdstippen t' naar hetzelfde team s
                for (int t2 = 0; t2 < timeSlots; t2++) {
                    if (t2 != t) { // Vermijd dubbele toewijzing
                        for (int j = 0; j < nTeams; j++) {
                            constraint.addTerm(1, x[t2][s][j][s]);
                        }
                    }
                }

                // Beperking toevoegen: Exact 1 flow per team s
                model.addConstr(constraint, GRB.EQUAL, 1, "flow_constraint_s" + s + "_t" + t);
            }
        }

        model.optimize();
    }
}