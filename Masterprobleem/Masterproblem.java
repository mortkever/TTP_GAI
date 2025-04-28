package Masterprobleem;

import com.gurobi.gurobi.*;

import java.util.*;


public class Masterproblem {
    GRBModel model;
    Map<Integer, List<Tour>> teamTours; // team -> lijst van tours
    Map<Tour, GRBVar> tourVars;         // elke tour krijgt z'n GRBVar
    private Map<Pair<Integer, Integer>, List<Tour>> teamSlotMap;
    private Map<Triple<Integer, Integer, Integer>, List<Tour>> nrcMap = new HashMap<>();



    public MasterProblem(GRBEnv env) throws GRBException {
        model = new GRBModel(env);
        teamTours = new HashMap<>();
        tourVars = new HashMap<>();
        this.teamSlotMap = new HashMap<>();
        this.nrcMap = new HashMap<>();


    }

    public void addTour(Tour tour) throws GRBException {
        teamTours.computeIfAbsent(tour.team, k -> new ArrayList<>()).add(tour);
        // 1ste variable // Doelfuntctie
        GRBVar var = model.addVar(0.0, 1.0, tour.cost, GRB.BINARY,
                "kt_" + tour.team + "_" + teamTours.get(tour.team).size());
        tourVars.put(tour, var);

        for (Match match : tour.matches) {
            int s = match.timeSlot;

            Pair<Integer, Integer> keyHome = new Pair<>(match.homeTeam, s);
            Pair<Integer, Integer> keyAway = new Pair<>(match.awayTeam, s);

            teamSlotMap.computeIfAbsent(keyHome, k -> new ArrayList<>()).add(tour);
            teamSlotMap.computeIfAbsent(keyAway, k -> new ArrayList<>()).add(tour);

            int t1 = Math.min(match.homeTeam, match.awayTeam);
            int t2 = Math.max(match.homeTeam, match.awayTeam);
            Triple<Integer, Integer, Integer> key = new Triple<>(t1, t2, s);
            nrcMap.computeIfAbsent(key, k -> new ArrayList<>()).add(tour);
        }
    }

    public void buildConstraints(Set<Integer> teams, Set<Integer> timeSlots) throws GRBException {
        // 1. Convexiteit: precies 1 tour per team // Geen fracties geen meerdere tours
        for (int t : teamTours.keySet()) {
            GRBLinExpr expr = new GRBLinExpr();
            for (Tour tour : teamTours.get(t)) {
                expr.addTerm(1.0, tourVars.get(tour));
            }
            model.addConstr(expr, GRB.EQUAL, 1.0, "convexity_team_" + t);
        }

        // 2. Coupling constraints: elke team speelt in elk tijdslot
//        for (int t : teams) {
//            for (int s : timeSlots) {
//                Pair<Integer, Integer> key = new Pair<>(t, s);
//                List<Tour> involvedTours = teamSlotMap.getOrDefault(key, new ArrayList<>());
//
//                if (!involvedTours.isEmpty()) {
//                    GRBLinExpr expr = new GRBLinExpr();
//                    for (Tour tour : involvedTours) {
//                        expr.addTerm(1.0, tourVars.get(tour));
//                    }
//                    model.addConstr(expr, GRB.EQUAL, 1.0, "coupling_t" + t + "_s" + s);
//                }
//            }
//        }

        for (int i : teams) {
            for (int j : teams) {
                if (i != j) {
                    // (i speelt thuis tegen j)
                    GRBLinExpr exprHome = new GRBLinExpr();
                    for (Tour tour : teamTours.get(i)) {
                        for (Match match : tour.matches) {
                            if (match.homeTeam == i && match.awayTeam == j) {
                                exprHome.addTerm(1.0, tourVars.get(tour));
                            }
                        }
                    }
                    model.addConstr(exprHome, GRB.EQUAL, 1.0, "match_home_" + i + "_" + j);

                    // (j speelt thuis tegen i)
                    GRBLinExpr exprAway = new GRBLinExpr();
                    for (Tour tour : teamTours.get(j)) {
                        for (Match match : tour.matches) {
                            if (match.homeTeam == j && match.awayTeam == i) {
                                exprAway.addTerm(1.0, tourVars.get(tour));
                            }
                        }
                    }
                    model.addConstr(exprAway, GRB.EQUAL, 1.0, "match_home_" + j + "_" + i);
                }
            }
        }

        // 3. Constraint
        for (int t1 : teams) {
            for (int t2 : teams) {
                if (t1 < t2) {
                    for (int s : timeSlots) {
                        if (s < 2 * (teams.size() - 1)) {
                            Triple<Integer, Integer, Integer> key1 = new Triple<>(t1, t2, s);
                            Triple<Integer, Integer, Integer> key2 = new Triple<>(t1, t2, s + 1);

                            List<Tour> toursS = nrcMap.getOrDefault(key1, new ArrayList<>());
                            List<Tour> toursNext = nrcMap.getOrDefault(key2, new ArrayList<>());

                            if (!toursS.isEmpty() || !toursNext.isEmpty()) {
                                GRBLinExpr expr = new GRBLinExpr();
                                for (Tour tour : toursS) {
                                    expr.addTerm(1.0, tourVars.get(tour));
                                }
                                for (Tour tour : toursNext) {
                                    expr.addTerm(1.0, tourVars.get(tour));
                                }

                                String cname = "NRC_t" + t1 + "_t" + t2 + "_s" + s;
                                model.addConstr(expr, GRB.LESS_EQUAL, 1.0, cname);
                            }
                        }
                    }
                }
            }
        }




    }

    public void optimize() throws GRBException {
        model.optimize();
    }

    public Map<Tour, Double> getSolution() throws GRBException {
        Map<Tour, Double> sol = new HashMap<>();
        for (Tour tour : tourVars.keySet()) {
            double val = tourVars.get(tour).get(GRB.DoubleAttr.X);
            sol.put(tour, val);
        }
        return sol;
    }

    public double getObjective() throws GRBException {
        return model.get(GRB.DoubleAttr.ObjVal);
    }
}

