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
    private GRBModel relaxedModel;
    private Map<Integer, HashMap<Tour, GRBVar>> lambdaVars;
    private int[][] distanceMatrix;
    private HashMap<String, GRBVar> relaxedVarMap = new HashMap<>();

    public Masterproblem(TourRepository tourRepo, int[][] distanceMatrix) throws GRBException {
        this.tourRepo = tourRepo;
        this.distanceMatrix = distanceMatrix;

        // Create environment ONCE
        env = new GRBEnv(true);
        env.set("logFile", "master.log");
        env.set(GRB.IntParam.LogToConsole, 0);
        env.start();
    }

    public int addTour(int team, Tour tour) {
        return tourRepo.addTour(team, tour);
    }

    public void buildConstraints() throws GRBException {
        long start = System.nanoTime();
        //System.out.println("Constraint building:");

        Map<Integer, List<Tour>> allTours = tourRepo.getAllTours();
        int numTeams = allTours.size();
        int numSlots = 2 * (numTeams - 1);

        // Dispose previous model if it exists
        if (model != null) {
            model.dispose();
        }
        model = new GRBModel(env);
        lambdaVars = new HashMap<>();
        //System.out.println("Setup (ms): " + (System.nanoTime() - start) / 1000000);
        start = System.nanoTime();

        // 1. Maak lambda-variabelen
        for (Map.Entry<Integer, List<Tour>> entry : allTours.entrySet()) {
            int team = entry.getKey();
            HashMap<Tour, GRBVar> teamVars = new HashMap<>();
            List<Tour> teamTours = entry.getValue();

            for (int p = 0; p < teamTours.size(); p++) {
                Tour tour = teamTours.get(p);
                // System.out.println("\nTour cost: " + tour.cost);
                GRBVar var = model.addVar(0.0, 1.0, tour.getRealCost(), GRB.BINARY, "lambda_" + team + "_" + p);
                teamVars.put(tour, var);
            }

            lambdaVars.put(team, teamVars);
        }
        //System.out.println("Vars (ms): " + (System.nanoTime() - start) / 1000000);
        start = System.nanoTime();

        // 2. Convexiteitsrestrictie/convexity (10): één tour per team
        for (Map.Entry<Integer, HashMap<Tour, GRBVar>> entry : lambdaVars.entrySet()) {
            GRBLinExpr expr = new GRBLinExpr();
            for (GRBVar var : entry.getValue().values()) {
                expr.addTerm(1.0, var);
            }
            model.addConstr(expr, GRB.EQUAL, 1.0, "convexity_" + entry.getKey());
        }
        //System.out.println("C10 (ms): " + (System.nanoTime() - start) / 1000000);
        start = System.nanoTime();

        // 3. coupling (9): teams om zelfde moment op zelfde locatie (voor een match)
        // For all loops
        for (int t = 0; t < numTeams; t++) {
            for (int s = 0; s < numSlots; s++) {
                GRBLinExpr expr = new GRBLinExpr();
                String key = s + "_" + t;

                // Somatie loop 1
                // j = t'
                for (int j = 0; j < numTeams; j++) {
                    if (j != t) {
                        for (Tour tour : allTours.get(t)) {
                            if (couplingArcExist(tour, j, s)) { // met j=t'
                                expr.addTerm(1.0, lambdaVars.get(t).get(tour));
                            }
                        }
                    }
                }

                // Somatie loop 2
                for (int j = 0; j < numTeams; j++) {
                    if (j != t) {
                        for (Tour tour : allTours.get(j)) {
                            if (couplingArcExist(tour, t, s)) {
                                expr.addTerm(1.0, lambdaVars.get(j).get(tour));
                            }
                        }
                    }
                }

                // System.out.println("\n\n Constraint added, key: " + key);
                model.addConstr(expr, GRB.EQUAL, 1.0, "coupling_" + key);
            }
        }
        //System.out.println("C9 (ms): " + (System.nanoTime() - start) / 1000000);
        start = System.nanoTime();

        // Constraint (12): NRC – geen heen-en-terug onmiddellijk na elkaar
        // for all loop
        for (int t = 0; t < numTeams; t++) {
            for (int j = 0; j < numTeams; j++) {
                if (t < j) {
                    for (int s = 0; s < numSlots; s++) {
                        GRBLinExpr expr = new GRBLinExpr();
                        String key = s + "_" + t + "_" + j;

                        // Somatie loop 1
                        for (Tour tour : allTours.get(t)) {
                            if (NRCArcExist(tour, t, j, s)) {
                                expr.addTerm(1.0, lambdaVars.get(t).get(tour));
                            }
                        }

                        // Somatie loop 2
                        for (Tour tour : allTours.get(j)) {
                            if (NRCArcExist(tour, j, t, s)) {
                                expr.addTerm(1.0, lambdaVars.get(j).get(tour));
                            }
                        }
                        // System.out.println("\n\n Constraint added, key: " + key);
                        // System.out.println("Before");
                        model.addConstr(expr, GRB.LESS_EQUAL, 1.0, "nrc_" + key);
                        // System.out.println("After");
                    }
                }
            }
        }
        //System.out.println("C12 (ms): " + (System.nanoTime() - start) / 1000000);
        start = System.nanoTime();

        model.update();
        //System.out.println("Done (ms): " + (System.nanoTime() - start) / 1000000);

    }

    private boolean couplingArcExist(Tour tour, int j, int s) {
        // Helper function for coupling constraint
        /*
         * for (Arc arc : tour.getArcs()) {
         * if (arc.to == j && arc.time == s) {
         * return true;
         * }
         * }
         */
        Arc arc = tour.getArcs().get(s);
        try {
            if (arc.time != s) {
                System.err.println("index != s");
                throw new Exception("Tijd en index komen niet overeen");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        if (arc.to == j)
            return true;

        return false;
    }

    private boolean NRCArcExist(Tour tour, int t, int j, int s) {
        // Helper function for NRC constraint
        Arc arc = tour.getArcs().get(s);
        try {
            if (arc.time != s) {
                System.err.println("index != s");
                throw new Exception("Tijd en index komen niet overeen");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        if (arc.from == t && arc.to == j) {
            return true;
        } else if (arc.from == j && arc.to == t) {
            return true;
        }

        return false;
    }

    public void setRelaxedModel(GRBModel relaxedModel) {
        this.relaxedModel = relaxedModel;

        for (GRBVar var : relaxedModel.getVars()) {
            try {
                relaxedVarMap.put(var.get(GRB.StringAttr.VarName), var);
            } catch (GRBException e) {
                e.printStackTrace();
            }
        }
    }

    public void optimize() throws GRBException {
        model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);

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

        for (Map.Entry<Integer, HashMap<Tour, GRBVar>> entry : lambdaVars.entrySet()) {
            int team = entry.getKey();
            HashMap<Tour, GRBVar> vars = entry.getValue();
            List<Tour> teamTours = tourRepo.getAllTours().get(team);

            boolean found = false;
            for (GRBVar var : vars.values()) {
                double value = var.get(GRB.DoubleAttr.X);
                if (value > 0.5) { // geselecteerde tour
                    selectedTours.put(team, teamTours.get(var.index()));
                    found = true;
                    break;
                }
            }

            // for (int i = 0; i < vars.size(); i++) {
            // double value = vars.get(i).get(GRB.DoubleAttr.X);
            // if (value > 0.5) { // geselecteerde tour
            // selectedTours.put(team, teamTours.get(i));
            // found = true;
            // break;
            // }
            // }

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

    public void printLambda(Boolean printAll) throws GRBException {
        for (Map.Entry<Integer, HashMap<Tour, GRBVar>> teamEntry : lambdaVars.entrySet()) {
            int index = 0;
            for (Map.Entry<Tour, GRBVar> tourEntry : teamEntry.getValue().entrySet()) {
                if (printAll || relaxedVarMap.get(tourEntry.getValue().get(GRB.StringAttr.VarName))
                        .get(GRB.DoubleAttr.X) > 0)
                    System.err.println(index +
                            " Team: " + teamEntry.getKey() + ", GRBVAR: "
                            + relaxedVarMap.get(tourEntry.getValue().get(GRB.StringAttr.VarName))
                                    .get(GRB.DoubleAttr.X)
                            + "\n"
                            +
                            tourEntry.getKey());
                index++;
            }
        }
    }

    public Map<Integer, HashMap<Tour, GRBVar>> getLambdaVars() {
        return lambdaVars;
    }
}
