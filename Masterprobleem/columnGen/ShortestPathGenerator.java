package Masterprobleem.columnGen;

import Masterprobleem.Arc;
import Masterprobleem.ColumnGenerationHelper;
import Masterprobleem.Tour;
import Masterprobleem.TourRepository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import com.gurobi.gurobi.*;

public class ShortestPathGenerator {
    private int upperbound;
    private int nTeams;
    private int timeSlots;
    private int[] visits;
    private int b;
    private double bestCost;
    private int[][] costs;
    public long[] times;
    private static ShortestPathGenerator spg;
    private ColumnGenerationHelper cgenHelper;
    private TourRepository existingTours;
    private int[] visited;
    public ArrayList<Tour> tours;

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

    public void generateAllTours(int team, double maxPercentage) {
        for (int k = 0; k < nTeams; k++) {
            visits[k] = 0;
        }
        bestCost = Double.MAX_VALUE;
        b = 0;
        tours = new ArrayList<>();
        DFSrecAll(team, 0, team, 0, 0, 0, new Stack<>());

        spg.tours.sort(null);
        int maxNumber = (int) (spg.tours.size() * maxPercentage);
        maxNumber = (maxNumber > 0 ? maxNumber : 1);    // At least use 1 tour

        while (spg.tours.size() > maxNumber) {
            spg.tours.removeFirst();
        }

        Iterator<Tour> iterator = tours.iterator();
        System.out.println(team + ": " + tours.size());
        while (iterator.hasNext()) {
            Tour tour = iterator.next();

            existingTours.addTour(team, tour);
        }
    }

    public void addTour(int team, Tour tour) {
        existingTours.addTour(team, tour);
    }

    private void DFSrecAll(int team, int s, int from, double modCost, double tourCost, int layer, Stack<Arc> arcs) {
        for (int i = 0; i < nTeams; i++) {
            if (s == timeSlots && i != team)
                continue;
            int b_prev = b;
            if (resourceExtentionFunction(team, s, from, i)) {
                visited[layer] = i;
                arcs.add(new Arc(s, from, i));
                tourCost += costs[from][i];

                if (s == timeSlots && i == team) {
                    double totalModCost = modCost + cgenHelper.computeModifiedCost(team, from, i, s, this.costs, nTeams)
                            - cgenHelper.getMu(team);
                    if (totalModCost < 0
                            && isNotRedundantAllTours(team, tourCost)) {
                        bestCost = totalModCost;
                        ArrayList<Arc> tourArcs = new ArrayList<>(arcs);
                        tours.add(new Tour(tourArcs, tourCost, bestCost));

                        arcs.pop();
                        tourCost -= costs[from][i];
                        return;
                    }
                } else {
                    visits[i]++;
                    DFSrecAll(team, s + 1, i,
                            modCost + cgenHelper.computeModifiedCost(team, from, i, s, this.costs, nTeams), tourCost,
                            layer + 1,
                            arcs);
                    visits[i]--;
                }

                arcs.pop();
                tourCost -= costs[from][i];
            }
            b = b_prev;
        }
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

    private boolean isNotRedundantAllTours(int team, double tourCost) {
        // Alle tours overlopen
        List<Tour> tours = existingTours.getTours(team);
        for (Tour tour : tours) {
            // Als er een arc verschilt met de gegenereerde tour: abort check, check
            // volgende tour
            boolean isDuplicate = true;
            if (tour.getRealCost() == tourCost) // Als de kost verschilt kunnen de arcs niet overeenkomen.
            {
                for (Arc arc : tour.getArcs()) {
                    if (visited[arc.time] != arc.to) {
                        isDuplicate = false;
                        break;
                    }
                }
            } else {
                isDuplicate = false;
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
        bestCost = model.get(GRB.DoubleAttr.ObjVal);

        List<Arc> arcs = new ArrayList<>();
        double totalCost = 0;

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

        if (bestCost - cgenHelper.getMu(team) < 0) {
            System.err.println(bestCost);

            Tour tour = new Tour(arcs, totalCost);
            existingTours.addTour(team, tour);
            return tour;
        } else {
            return new Tour(new ArrayList<Arc>(), 0);
        }
    }
}
