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

        // 2. Convexiteitsrestrictie (10): één tour per team
        for (Map.Entry<Integer, List<GRBVar>> entry : lambdaVars.entrySet()) {
            GRBLinExpr expr = new GRBLinExpr();
            for (GRBVar var : entry.getValue()) {
                expr.addTerm(1.0, var);
            }
            model.addConstr(expr, GRB.EQUAL, 1.0, "oneTourPerTeam_" + entry.getKey());
        }

        // 3. Synchronisatieconstraint/coupling constraint (9): elk arc precies één keer
//        for (Map.Entry<String, List<int[]>> arcEntry : arcIndex.entrySet()) {
//            GRBLinExpr expr = new GRBLinExpr();
//            for (int[] tp : arcEntry.getValue()) {
//                GRBVar lambda = lambdaVars.get(tp[0]).get(tp[1]);
//                expr.addTerm(1.0, lambda);
//            }
//            model.addConstr(expr, GRB.EQUAL, 1.0, "matchOnce_" + arcEntry.getKey());
//        }

        // Helper variable voor constraint 9
        Map<Integer, Map<Integer, List<int[]>>> matchParticipation = new HashMap<>();

        for (int team = 0; team < numTeams; team++) {
            matchParticipation.put(team, new HashMap<>());
            for (int s = 0; s < numSlots; s++) {
                matchParticipation.get(team).put(s, new ArrayList<>());
            }
        }

        for (Map.Entry<Integer, List<Tour>> entry : allTours.entrySet()) {
            int team = entry.getKey();
            List<Tour> tours = entry.getValue();

            for (int p = 0; p < tours.size(); p++) {
                for (Arc arc : tours.get(p).arcs) {
                    int s = arc.time;
                    int i = arc.from;
                    int j = arc.to;

                    // team speelt thuis (van = team)
                    if (i == team) {
                        matchParticipation.get(team).get(s).add(new int[]{team, p});
                        System.out.println(" We geraken in de eerste IF");
                    }

                    // team speelt uit (naar = team)
                    if (j == team && i != j) { // sluit self-loops uit
                        matchParticipation.get(team).get(s).add(new int[]{team, p});
                        System.out.println(" We geraken in de tweede IF");

                    }
                }
            }
        }

        // 3. Synchronisatieconstraint/coupling constraint (9): Elk team speelt precies 1 wedstrijd per tijdstip, als thuis- of uitteam
        for (int t = 0; t < numTeams; t++) {
            for (int s = 0; s < numSlots; s++) {
                GRBLinExpr expr = new GRBLinExpr();

                for (int[] tp : matchParticipation.get(t).get(s)) {
                    GRBVar lambda = lambdaVars.get(tp[0]).get(tp[1]);
                    expr.addTerm(1.0, lambda);
                }

                model.addConstr(expr, GRB.EQUAL, 1.0, "teamTimeMatch_" + t + "_" + s);
            }
        }


        // 4. NRC constraint (12): geen heen- en terug direct achter elkaar
        for (int t1 = 0; t1 < numTeams; t1++) {
            for (int t2 = t1 + 1; t2 < numTeams; t2++) {
                for (int s = 0; s < numSlots - 1; s++) {
                    String key1 = s + "_" + t1 + "_" + t2;
                    String key2 = (s + 1) + "_" + t2 + "_" + t1;

                    List<int[]> list1 = arcIndex.getOrDefault(key1, new ArrayList<>());
                    List<int[]> list2 = arcIndex.getOrDefault(key2, new ArrayList<>());

                    for (int[] tp1 : list1) {
                        for (int[] tp2 : list2) {
                            GRBLinExpr expr = new GRBLinExpr();
                            expr.addTerm(1.0, lambdaVars.get(tp1[0]).get(tp1[1]));
                            expr.addTerm(1.0, lambdaVars.get(tp2[0]).get(tp2[1]));
                            model.addConstr(expr, GRB.LESS_EQUAL, 1.0,
                                    "nrc_" + tp1[0] + "_" + tp1[1] + "__" + tp2[0] + "_" + tp2[1]);
                        }
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
