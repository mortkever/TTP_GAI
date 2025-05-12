package Masterprobleem;

import com.gurobi.gurobi.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Masterproblem {
    private final TourRepository tourRepo;
    private GRBEnv env;
    private GRBModel model;
    private Map<Integer, List<GRBVar>> lambdaVars;
    private Map<String, List<int[]>> arcIndex;

    public Masterproblem(TourRepository tourRepo) {
        this.tourRepo = tourRepo;
    }

    public void addTour(int team, Tour tour) {
        tourRepo.addTour(team, tour);
    }

    public void buildConstraints() throws GRBException {
        Map<Integer, List<Tour>> allTours = tourRepo.getAllTours();
        int numTeams = allTours.size();
        int numSlots = 2 * (numTeams - 1);

        env = new GRBEnv(true);
        env.set("logFile", "master.log");
        env.start();
        model = new GRBModel(env);
        lambdaVars = new HashMap<>();
        arcIndex = new HashMap<>();


        // 1. Maak lambda-variabelen
        for (Map.Entry<Integer, List<Tour>> entry : allTours.entrySet()) {
            int team = entry.getKey();
            List<GRBVar> teamVars = new ArrayList<>();
            List<Tour> teamTours = entry.getValue();

            for (int p = 0; p < teamTours.size(); p++) {
                Tour tour = teamTours.get(p);
                GRBVar var = model.addVar(0.0, 1.0, tour.cost, GRB.BINARY, "lambda_" + team + "_" + p);
                teamVars.add(var);

                // Arc-index vullen voor latere constraints
                for (Arc arc : tour.arcs) {
                    String key = arc.time + "_" + arc.from + "_" + arc.to;
                    arcIndex.computeIfAbsent(key, k -> new ArrayList<>()).add(new int[]{team, p});
                }
            }

            lambdaVars.put(team, teamVars);
        }

        // 2. Convexiteitsrestrictie/convexity (10): één tour per team
        for (Map.Entry<Integer, List<GRBVar>> entry : lambdaVars.entrySet()) {
            GRBLinExpr expr = new GRBLinExpr();
            for (GRBVar var : entry.getValue()) {
                expr.addTerm(1.0, var);
            }
            model.addConstr(expr, GRB.EQUAL, 1.0, "oneTourPerTeam_" + entry.getKey());
        }

        // 3. Synchronisatieconstraint/coupling (9): elk arc precies één keer
        for (Map.Entry<String, List<int[]>> arcEntry : arcIndex.entrySet()) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int[] tp : arcEntry.getValue()) {
                GRBVar lambda = lambdaVars.get(tp[0]).get(tp[1]);
                expr.addTerm(1.0, lambda);
            }
            model.addConstr(expr, GRB.EQUAL, 1.0, "matchOnce_" + arcEntry.getKey());
        }

         //Constraint (12): NRC – geen heen-en-terug onmiddellijk na elkaar
        for (int t = 0; t < numTeams; t++) {
            for (int tPrime = t + 1; tPrime < numTeams; tPrime++) {
                for (int s = 0; s < numSlots - 1; s++) {

                    // Arc (t, t', s) en (t', t, s) in tours van team t
                    String arc1_tt_s = s + "_" + t + "_" + tPrime;
                    String arc2_tprime_t_s = s + "_" + tPrime + "_" + t;

                    // Arc (t, t', s+1) en (t', t, s+1) in tours van team t'
                    String arc3_tt_s1 = (s + 1) + "_" + t + "_" + tPrime;
                    String arc4_tprime_t_s1 = (s + 1) + "_" + tPrime + "_" + t;

                    List<int[]> pt1 = arcIndex.getOrDefault(arc1_tt_s, new ArrayList<>());
                    List<int[]> pt2 = arcIndex.getOrDefault(arc2_tprime_t_s, new ArrayList<>());
                    List<int[]> pt3 = arcIndex.getOrDefault(arc3_tt_s1, new ArrayList<>());
                    List<int[]> pt4 = arcIndex.getOrDefault(arc4_tprime_t_s1, new ArrayList<>());

                    if ((!pt1.isEmpty() || !pt2.isEmpty()) && (!pt3.isEmpty() || !pt4.isEmpty())) {
                        GRBLinExpr expr = new GRBLinExpr();

                        for (int[] tp : pt1) {
                            if (tp[0] == t) expr.addTerm(1.0, lambdaVars.get(tp[0]).get(tp[1]));
                        }
                        for (int[] tp : pt2) {
                            if (tp[0] == t) expr.addTerm(1.0, lambdaVars.get(tp[0]).get(tp[1]));
                        }
                        for (int[] tp : pt3) {
                            if (tp[0] == tPrime) expr.addTerm(1.0, lambdaVars.get(tp[0]).get(tp[1]));
                        }
                        for (int[] tp : pt4) {
                            if (tp[0] == tPrime) expr.addTerm(1.0, lambdaVars.get(tp[0]).get(tp[1]));
                        }

                        model.addConstr(expr, GRB.LESS_EQUAL, 1.0,
                                "nrc_" + t + "_" + tPrime + "_" + s);
                    }
                }
            }
        }

        model.update();
    }

    public void optimize() {
        try {
            System.out.println("Starting optimization...");
            model.optimize();

            int status = model.get(GRB.IntAttr.Status);
            if (status == GRB.OPTIMAL) {
                double objVal = model.get(GRB.DoubleAttr.ObjVal);
                System.out.println("Optimal solution found with total cost: " + objVal);
            } else if (status == GRB.INFEASIBLE) {
                System.out.println("Model is infeasible.");
            } else if (status == GRB.UNBOUNDED) {
                System.out.println("Model is unbounded.");
            } else {
                System.out.println("Optimization ended with status: " + status);
            }
        } catch (GRBException e) {
            System.err.println("Error during optimization: " + e.getMessage());
        }
    }

    public Map<Integer, Tour> getSolution() throws GRBException {
        Map<Integer, Tour> selectedTours = new HashMap<>();

        for (Map.Entry<Integer, List<GRBVar>> entry : lambdaVars.entrySet()) {
            int team = entry.getKey();
            List<GRBVar> vars = entry.getValue();
            List<Tour> teamTours = tourRepo.getAllTours().get(team);

            boolean found = false;
            for (int i = 0; i < vars.size(); i++) {
                double value = vars.get(i).get(GRB.DoubleAttr.X);
                if (value > 0.5) { // geselecteerde tour
                    selectedTours.put(team, teamTours.get(i));
                    found = true;
                    break;
                }
            }

            if (!found) {
                System.out.println("Waarschuwing: geen tour geselecteerd voor team " + team);
            }
        }

        return selectedTours;
    }

    public TourRepository getTourRepo() {
        return tourRepo;
    }

    public GRBModel getModel() {
        return model;
    }

}
