package BranchPrice;
import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;

import java.util.*;

public class BranchAndPrice {

    private Masterproblem master;
    private List<BranchingDecision> fixations; // bijv. "lambda_tourX = 0"
    private ColumnGenerationHelper helper;
    private ShortestPathGenerator spg;
    private int nTeams, timeSlots;
    private int[][] distanceMatrix;
    private double lowerBound;
    private int lpStatus;
    private GRBModel bestSolution;
    private double incumbent = 32132131231231232132.312312321;
    private Set<String> previousBranchDecisions = new HashSet<>();
    private Queue<List<BranchingDecision>> queue = new LinkedList<>();


    //List<BranchingDecision> fixations
    public BranchAndPrice(int nTeams, int[][] distanceMatrix, List<BranchingDecision> fixations) throws GRBException {
        this.nTeams = nTeams;
        this.distanceMatrix = distanceMatrix;
        this.timeSlots = 2 * (nTeams - 1);
        this.fixations = fixations;

        this.helper = new ColumnGenerationHelper();
        this.master = new Masterproblem(new TourRepository(nTeams), distanceMatrix);
        this.spg = ShortestPathGenerator.initializeSPG(nTeams, 3, timeSlots, distanceMatrix, helper);

        // Start met superkolommen of compact oplossing
        ColumnGenerationHelper.addInitialSolution(4, master, spg, nTeams, timeSlots, distanceMatrix);
    }





    public int solveLP() throws GRBException {
        ///////////////////
        // Variable
        int counter = 0;
        int exisingTours = 0;
        int optimalTours = 0;
        boolean isfrac = false;
        helper.setRandCost(false);


        GRBModel relaxed;
        master.filterToursWithFixations(fixations);

        do {

            ////////////////////
            // Build LP
            master.buildConstraints();
            relaxed = master.getModel().relax();
            relaxed.optimize();
            master.setRelaxedModel(relaxed);

            for (GRBVar var : relaxed.getVars()) {
                double value = var.get(GRB.DoubleAttr.X);
                if (value < 1 - 1e-6 && value > 0 + 1e-6) {
                    helper.setRandCost(false);
                    isfrac = true;
                }
            }
            /////////////////////
            // Haal Duale waarde eruit
            helper.setModel(relaxed);
            helper.extractDuals();

            ////////////////////
            // Tours toevoegen
            exisingTours = 0;
            optimalTours = 0;
            int maxNumber = 500;
            for (int t = 0; t < nTeams; t++) {
                spg.generateTour(t);
                if (spg.tours.size() > 0) {
                    while (spg.tours.size() > maxNumber)
                        spg.tours.poll();
                    for (Tour tour : spg.tours) {
                        if (tourIsCompatibleWithFixations(tour, t)) {
                            exisingTours += master.addTour(t, tour);
                        }
                    }
                } else {
                    optimalTours++;
                }
            }

            counter++;
            System.out.println("iteratie: " + counter);
        } while (optimalTours < nTeams);

        ////////////////////////////////////////////////////:
        // MasterLP -> MasterIp
        double lpObj = relaxed.get(GRB.DoubleAttr.ObjVal);
        master.filterToursWithFixations(fixations);
        if (lpObj >= lowerBound) {
            // Hier gaan we eingelijk al prunen.
            System.out.println("❌ Prune: LP " + lpObj + " ≥ incumbent " + lowerBound);
            return 3;
        }


        Map<Integer, HashMap<Tour, GRBVar>> lambdaVars = master.getLambdaVars();

            for (Map.Entry<Integer, HashMap<Tour, GRBVar>> entry : lambdaVars.entrySet()) {
                Tour bestTour = null;
                double bestVal = -1;
                try {
                    // First check if the model has been optimized successfully
                    if (master.getModel().get(GRB.IntAttr.Status) == GRB.OPTIMAL) {
                        for (Map.Entry<Tour, GRBVar> e : entry.getValue().entrySet()) {
                            GRBVar var = e.getValue();
                            if (var != null) {
                                double val = var.get(GRB.DoubleAttr.X);
                                if (val > bestVal) {
                                    bestVal = val;
                                    bestTour = e.getKey();
                                }
                            }
                        }
                        if (bestTour != null) {
                            entry.getValue().get(bestTour).set(GRB.DoubleAttr.Start, 1.0);
                        }
                    }
                } catch (GRBException e) {
                    System.err.println("Error accessing variable value: " + e.getMessage());
                    continue;
                }
            }

            System.out.println("\n\n-------------------------------");
            System.out.println("Masterprobleem eind LP model:");
            System.out.println("Optimal tours: " + optimalTours);
            System.out.println("Existing tours: " + exisingTours);

            System.out.println("\n\n-------------------------------");
            master.getModel().set(GRB.DoubleParam.MIPGap, 0.01);
            master.getModel().set(GRB.IntParam.MIPFocus, 1);

            master.getModel().set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);
            master.getModel().optimize();
            int status = master.getModel().get(GRB.IntAttr.Status);

            if (status == GRB.Status.OPTIMAL) {
                System.out.println("Optimal solution found");
                System.out.println("Total cost: " + master.getModel().get(GRB.DoubleAttr.ObjVal));

                System.out.println("\n🟢 Geselecteerde tours in de optimale oplossing:");

                for (Map.Entry<Integer, HashMap<Tour, GRBVar>> entry : lambdaVars.entrySet()) {
                    int team = entry.getKey();
                    for (Map.Entry<Tour, GRBVar> tourEntry : entry.getValue().entrySet()) {
                        double val = tourEntry.getValue().get(GRB.DoubleAttr.X);
                        if (val > 0.5) {
                            System.out.println("Team " + team + ": " + tourEntry.getKey());
                            break; // er is maar 1 tour geselecteerd per team
                        }
                    }

                }

            } else {
                System.out.println("No optimal solution found");
            }
            ////////////////////////////////


            double lpValue = relaxed.get(GRB.DoubleAttr.ObjVal); // Lower Bound
            double ipValue = master.getModel().get(GRB.DoubleAttr.ObjVal); // Integer oplossing

            if (isIntegerSolution()) {
                if (ipValue < incumbent) {
                    incumbent = ipValue;
                    bestSolution = extractSolution(); // Sla concrete tours op
                    System.out.println("✅ Nieuwe incumbent: " + incumbent);
                    return 1;
                } else {
                    System.out.println("ℹ️ Integer maar niet beter dan incumbent");
                    return 2;
                }
                 // Geen branching nodig
            }

            if (lpValue >= incumbent) {
                System.out.println("❌ Prune: lower bound " + lpValue + " ≥ incumbent " + incumbent);
                return 3;
            }

            // ➕ LP < incumbent, maar nog geen integer → branch
            System.out.println("🔁 Branching mogelijk: lp " + lpValue + " < incumbent " + incumbent);
            BranchingDecision arc = selectMostFractionalArc();

            if (arc == null) {
                System.out.println("⚠️ Geen geschikte branching arc gevonden.");
                return counter;
            }


        return 4;
    }


    BranchingDecision selectMostFractionalArc() {
        double bestFractionality = Double.MAX_VALUE;
        BranchingDecision bestDecision = null;

        Map<Integer, HashMap<Tour, GRBVar>> lambdaVars = master.getLambdaVars();

        for (Map.Entry<Integer, HashMap<Tour, GRBVar>> entry : lambdaVars.entrySet()) {
            int team = entry.getKey();
            for (Map.Entry<Tour, GRBVar> e : entry.getValue().entrySet()) {
                Tour tour = e.getKey();
                GRBVar var = e.getValue();

                try {
                    double val = var.get(GRB.DoubleAttr.X);
                    double frac = Math.abs(val - 0.5);

                    if (frac > 1e-5 && frac < bestFractionality) {
                        // Zoek de eerste echte verplaatsing
                        for (Arc arc : tour.getArcs()) {
                            if (arc.time > 0) {
                                String key = team + "-" + arc.from + "-" + arc.to + "-" + arc.time;

                                if (previousBranchDecisions.contains(key)) {
                                    continue; // deze beslissing is al eens geprobeerd
                                }

                                // Nieuw voorstel: onthoud hem
                                bestFractionality = frac;
                                bestDecision = new BranchingDecision(team, arc.from, arc.to, arc.time, true);

                                previousBranchDecisions.add(key); // markeer als geprobeerd
                                break;
                            }
                        }
                    }
                } catch (GRBException ex) {
                    System.err.println("Fout bij ophalen van lambda-waarde: " + ex.getMessage());
                }
            }
        }

        return bestDecision;
    }

    private GRBModel extractSolution() {
        try {
            return new GRBModel(master.getModel());
        } catch (GRBException e) {
            System.err.println("❌ Kon mastermodel niet kopiëren: " + e.getMessage());
            return null;
        }
    }


    private boolean isIntegerSolution() throws GRBException {
        for (GRBVar var : master.getModel().getVars()) {
            double val = var.get(GRB.DoubleAttr.X);
            if (val > 1e-6 && val < 1 - 1e-6) {
                return false;
            }
        }
        return true;
    }

    private boolean tourIsCompatibleWithFixations(Tour tour, int team)
    {
        for (BranchingDecision fix : fixations) {
            if (fix.team != team) continue;

            boolean contains = tour.containsArc(fix.from, fix.to, fix.slot);

            if (fix.mustUseArc && !contains) return false;
            if (!fix.mustUseArc && contains) return false;
        }
        return true;

    }


    public double getLowerBound() {
        return lowerBound;
    }

    public boolean isLPSolvedToOptimality() {
        return lpStatus == GRB.Status.OPTIMAL;
    }

    public boolean isIntegral() throws GRBException {
        for (HashMap<Tour, GRBVar> teamVars : master.getLambdaVars().values()) {
            for (GRBVar var : teamVars.values()) {
                double val = var.get(GRB.DoubleAttr.X);
                if (val > 1e-5 && val < 1 - 1e-5) {
                    return false; // fractionele waarde
                }
            }
        }
        return true;
    }
    

    public double getIncumbent() {
        return incumbent;
    }

    public GRBModel getBestModel() {
        return bestSolution;
    }
}