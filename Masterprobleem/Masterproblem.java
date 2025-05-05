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

        // 2. Convexiteitsrestrictie: één tour per team
        for (Map.Entry<Integer, List<GRBVar>> entry : lambdaVars.entrySet()) {
            GRBLinExpr expr = new GRBLinExpr();
            for (GRBVar var : entry.getValue()) {
                expr.addTerm(1.0, var);
            }
            model.addConstr(expr, GRB.EQUAL, 1.0, "oneTourPerTeam_" + entry.getKey());
        }

        // 3. Synchronisatieconstraint (9): elk arc precies één keer
        for (Map.Entry<String, List<int[]>> arcEntry : arcIndex.entrySet()) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int[] tp : arcEntry.getValue()) {
                GRBVar lambda = lambdaVars.get(tp[0]).get(tp[1]);
                expr.addTerm(1.0, lambda);
            }
            model.addConstr(expr, GRB.EQUAL, 1.0, "matchOnce_" + arcEntry.getKey());
        }

         //Constraint (12): NRC – geen heen-en-terug onmiddellijk na elkaar
        for (int t1 = 0; t1 < numTeams; t1++) {
            for (int t2 = t1 + 1; t2 < numTeams; t2++) {
                for (int s = 0; s < numSlots - 1; s++) {

                    String arc1Key = s + "_" + t1 + "_" + t2;       // heenwedstrijd
                    String arc2Key = (s + 1) + "_" + t2 + "_" + t1; // terugwedstrijd

                    List<int[]> arc1Tours = arcIndex.getOrDefault(arc1Key, new ArrayList<>());
                    List<int[]> arc2Tours = arcIndex.getOrDefault(arc2Key, new ArrayList<>());

                    // Voeg enkel een constraint toe als beide zijden iets bevatten
                    if (!arc1Tours.isEmpty() && !arc2Tours.isEmpty()) {
                        GRBLinExpr expr = new GRBLinExpr();

                        for (int[] tp : arc1Tours) {
                            int team = tp[0];
                            int p = tp[1];
                            expr.addTerm(1.0, lambdaVars.get(team).get(p));
                        }

                        for (int[] tp : arc2Tours) {
                            int team = tp[0];
                            int p = tp[1];
                            expr.addTerm(1.0, lambdaVars.get(team).get(p));
                        }

                        model.addConstr(expr, GRB.LESS_EQUAL, 1.0,
                                "nrc_" + t1 + "_" + t2 + "_s" + s);
                    }
                }
            }
        }
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


}
