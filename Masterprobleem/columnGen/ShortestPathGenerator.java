package Masterprobleem.columnGen;

import Masterprobleem.Arc;
import Masterprobleem.Tour;
import Masterprobleem.TourRepository;

import java.util.ArrayList;
import java.util.List;

import com.gurobi.gurobi.*;

public class ShortestPathGenerator {
    private int upperbound;
    private int nTeams;
    private int timeSlots;
    private int[] visits;
    private int b;
    private double bestCost;
    private int[][] costs;
    private List<Arc> bestArcs = new ArrayList<>();
    public long[] times;
    private static ShortestPathGenerator spg;
    private ColumnGenerationHelper cgenHelper;
    private TourRepository existingTours;
    private int[] visited;

    private ShortestPathGenerator(int nTeams, int upperbound, int ts, int[][] costs, ColumnGenerationHelper cgh) {
        this.nTeams = nTeams;
        this.upperbound = upperbound;
        this.timeSlots = ts;
        this.visits = new int[nTeams];
        this.visited = new int[2 * nTeams - 1];
        this.costs = costs;
        times = new long[nTeams];
        this.cgenHelper = cgh;
        existingTours = new TourRepository(nTeams);
    }

    public static ShortestPathGenerator initializeSPG(int nTeams, int upperbound, int ts, int[][] costs,
            ColumnGenerationHelper cgenHelper) {
        if (spg == null) {
            spg = new ShortestPathGenerator(nTeams, upperbound, ts, costs, cgenHelper);
        } else {
            spg.nTeams = nTeams;
            spg.upperbound = upperbound;
            spg.timeSlots = ts;
            spg.visits = new int[nTeams];
            spg.costs = costs;
            spg.times = new long[nTeams];
            spg.cgenHelper = cgenHelper;
            spg.existingTours = new TourRepository(nTeams);
        }
        return spg;
    }

    public static void updateCost(int[][] newCost) {
        spg.costs = newCost;
    }

    public static ShortestPathGenerator getSPG() {
        if (spg == null) {
            System.err.println("Error generator is not initialized");
            System.exit(-1);
        }
        return spg;
    }

    private boolean resourceExtentionFunction(int team, int time, int from, int to) {
        if ((visits[to] > 0 && to != team) || (visits[to] >= nTeams - 1 && to == team) && time != (2 * (nTeams - 1))) {
            return false;
        }
        if (isArcB(team, time, from, to, nTeams)) {
            if (b >= upperbound - 1) {
                return false;
            }
            b++;
        } else {
            b = 0;
        }
        return true;
    }

    public Tour generateTour(int team) {
        long start = System.nanoTime();
        for (int k = 0; k < nTeams; k++) {
            visits[k] = 0;
        }
        cgenHelper.resetCache(nTeams, timeSlots);
        bestCost = Integer.MAX_VALUE;
        b = 0;
        bestArcs = new ArrayList<>();
        DFSrec(team, 0, team, 0, 0);
        // times[team] = (System.nanoTime() - start) / 1000;
        // System.err.println("Best cost: " + bestCost + ", Time (µs): " + times[team]);
        if (bestCost - cgenHelper.getMu(team) < 0) {
            int realCost = 0;
            for (Arc arc : bestArcs) {
                realCost += costs[arc.from][arc.to];
            }
            Tour tour = new Tour(bestArcs, realCost);
            existingTours.addTour(team, tour);
            return tour;
        } else {
            return new Tour(new ArrayList<Arc>(), 0);
        }
    }

    public void addTour(int team, Tour tour) {
        existingTours.addTour(team, tour);
    }

    private boolean DFSrec(int team, int s, int from, double cost, int layer) {
        boolean tourFound = false;
        for (int i = 0; i < nTeams; i++) {
            if (/*
                 * cost + cgenHelper.computeModifiedCost(team, from, i, s, this.costs, nTeams)
                 * >= bestCost
                 * ||
                 */ (s == timeSlots && i != team))
                continue;
            int b_prev = b;
            if (resourceExtentionFunction(team, s, from, i)) {
                visited[layer] = i;
                if (s == timeSlots && i == team) {
                    if (cost + cgenHelper.computeModifiedCost(team, from, i, s, this.costs, nTeams) < bestCost
                            && isRedundantTour(team)) {
                        bestCost = cost + cgenHelper.computeModifiedCost(team, from, i, s, this.costs, nTeams);
                        bestArcs.clear();
                        bestArcs.add(new Arc(s, from, i)); // is dit gegarandeert een pad naar homebase? Ja...?
                        return true;
                    }
                } else {
                    visits[i]++;
                    if (DFSrec(team, s + 1, i,
                            cost + cgenHelper.computeModifiedCost(team, from, i, s, this.costs, nTeams), layer + 1)) {
                        bestArcs.addFirst(new Arc(s, from, i));
                        tourFound = true;
                    }
                    visits[i]--;
                }
            }
            b = b_prev;
        }
        return tourFound;
    }

    private static boolean isArcA(int t, int s, int i, int j, int nTeams) {
        boolean one = (t == i && s == 0);
        boolean two = (j == t && s == 2 * (nTeams - 1));
        boolean three = (i != j || (i == t && i == j)) && s != (2 * (nTeams - 1)) && s != 0;
        return one || two || three;
    }

    private static boolean isArcB(int t, int s, int i, int j, int nTeams) {
        boolean one = (t == i && t == j && s != 0 && s != (2 * (nTeams - 1)));
        boolean two = (j != t && t != i);
        return (one || two) && isArcA(t, s, i, j, nTeams);
    }

    private boolean isRedundantTour(int team) {
        // Alle tours overlopen
        List<Tour> tours = existingTours.getTours(team);
        for (Tour tour : tours) {
            // Als er een arc verschilt met de gegenereerde tour: abort check, check
            // volgende tour
            boolean isDuplicate = true;
            for (Arc arc : tour.arcs) {
                if (visited[arc.time] != arc.to) {
                    isDuplicate = false;
                    break;
                }
            }
            // Als alle arcs overeen komen: zelfde tour => return false;
            if (isDuplicate) {
                return false;
            }
        }
        return true; // Does not exist in the repo
    }

    public Tour generateGTour(int team) throws GRBException {
        GRBEnv env = new GRBEnv();
        env.set(GRB.IntParam.LogToConsole, 0);
        env.set(GRB.IntParam.OutputFlag, 0);
        GRBModel model = new GRBModel(env);
        GRBVar x[][][] = new GRBVar[timeSlots + 1][nTeams][nTeams];
        cgenHelper.resetCache(nTeams, timeSlots);

        for (int s = 0; s < timeSlots + 1; s++) {
            for (int i = 0; i < nTeams; i++) {
                for (int j = 0; j < nTeams; j++) {
                    if (isArcA(team, s, i, j, nTeams)) {
                        x[s][i][j] = model.addVar(0, 1, cgenHelper.computeModifiedCost(team, i, j, s, costs, nTeams),
                                GRB.BINARY,
                                (s + "_" + i + "_" + j));
                    }
                }
            }
        }

        for (int s = 1; s < timeSlots + 1; s++) {
            for (int j = 0; j < nTeams; j++) {

                GRBLinExpr flow = new GRBLinExpr();
                for (int i = 0; i < nTeams; i++) {
                    if (isArcA(team, s - 1, i, j, nTeams)) {
                        flow.addTerm(1, x[s - 1][i][j]);
                    }
                }

                for (int i = 0; i < nTeams; i++) {
                    if (isArcA(team, s, j, i, nTeams)) {
                        flow.addTerm(-1, x[s][j][i]);
                    }
                }
                model.addConstr(flow, GRB.EQUAL, 0, "flow_conservation(j=" + j + ",s=" + s + ")");

            }
        }

        for (int i = 0; i < nTeams; i++) {
            if (i != team) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int s = 1; s < timeSlots + 1; s++) {
                    for (int j = 0; j < nTeams; j++) {
                        if (isArcA(team, s, i, j, nTeams))
                            expr.addTerm(1.0, x[s][i][j]);
                    }
                }
                model.addConstr(expr, '=', 1, "visitation_" + team + "_" + i);
            }
        }

        for (int s = 1; s <= 2 * (nTeams - 1) - upperbound; s++) {
            GRBLinExpr expr = new GRBLinExpr();
            for (int i = 0; i < nTeams; i++) {
                for (int j = 0; j < nTeams; j++) {
                    if (isArcB(team, s, i, j, nTeams)) {
                        for (int u = 0; u < upperbound; u++) {
                            expr.addTerm(1.0, x[s + u][i][j]);
                        }
                    }
                }
            }
            model.addConstr(expr, GRB.LESS_EQUAL, upperbound - 1, "breaks_" + team);
        }

        model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);
        model.optimize();

        List<Arc> arcs = new ArrayList<>();
        int totalCost = 0;

        for (int s = 0; s < timeSlots + 1; s++) {
            for (int i = 0; i < nTeams; i++) {
                for (int j = 0; j < nTeams; j++) {
                    if (isArcA(team, s, i, j, nTeams) && x[s][i][j].get(GRB.DoubleAttr.X) > 0.5) {
                        arcs.add(new Arc(s, i, j));
                        totalCost += costs[i][j];
                    }
                }
            }
        }

        return new Tour(arcs, totalCost);

    }
}
