package Masterprobleem.columnGen;

import Masterprobleem.Arc;
import Masterprobleem.Tour;

import java.util.ArrayList;
import java.util.List;

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

    private ShortestPathGenerator(int nTeams, int upperbound, int ts, int[][] costs, ColumnGenerationHelper cgh) {
        this.nTeams = nTeams;
        this.upperbound = upperbound;
        this.timeSlots = ts;
        this.visits = new int[nTeams];
        this.costs = costs;
        times = new long[nTeams];
        this.cgenHelper = cgh;
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
        if ((visits[to] > 0 && to != team) || (visits[to] >= nTeams && to == team) && time != (2 * (nTeams - 1) + 1)) {
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
        DFSrec(team, 0, team, 0);
        times[team] = (System.nanoTime() - start) / 1000;
        System.err.println("Best cost: " + bestCost + ", Time (µs): " + times[team]);
        return new Tour(bestArcs, bestCost);
    }

    private boolean DFSrec(int team, int s, int from, double cost) {
        boolean tourFound = false;
        for (int i = 0; i < nTeams; i++) {
            if (/*cost + cgenHelper.computeModifiedCost(team, from, i, s, this.costs, nTeams) >= bestCost
                    ||*/ (s == timeSlots && i != team))
                continue;
            int b_prev = b;
            if (resourceExtentionFunction(team, s, from, i)) {
                if (s == timeSlots && i == team) {
                    if (cost + cgenHelper.computeModifiedCost(team, from, i, s, this.costs, nTeams) < bestCost) {
                        bestCost = cost + cgenHelper.computeModifiedCost(team, from, i, s, this.costs, nTeams);
                        bestArcs.clear();
                        bestArcs.add(new Arc(s, from, i)); // is dit gegarandeert een pad naar homebase? Ja...?
                        return true;
                    }
                } else {
                    visits[i]++;
                    if (DFSrec(team, s + 1, i,
                            cost + cgenHelper.computeModifiedCost(team, from, i, s, this.costs, nTeams))) {
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
        boolean two = (j == t && s == (2 * (nTeams - 1) + 1));
        boolean three = (i != j || (i == t && i == j)) && s != (2 * (nTeams - 1) + 1) && s != 0;
        return one || two || three;
    }

    private static boolean isArcB(int t, int s, int i, int j, int nTeams) {
        boolean one = (t == i && t == j && s != 0 && s != (2 * (nTeams - 1) + 1));
        boolean two = (j != t && t != i);
        return (one || two) && isArcA(t, s, i, j, nTeams);
    }
}
