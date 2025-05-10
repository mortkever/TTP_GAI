package Masterprobleem.columnGen;

import Masterprobleem.Arc;
import Masterprobleem.Tour;

import java.util.ArrayList;
import java.util.List;

public class ShortestPathGenerator {
    private static int upperbound;
    private static int nTeams;
    private static int timeSlots;
    private static int[] visits;
    private static int b;
    private static int bestCost;
    private static int[][] costs;
    private static List<Arc> bestArcs = new ArrayList<>();
    public static long[] times;

    public ShortestPathGenerator(int nTeams, int upperbound, int ts, int[][] costs) {
        ShortestPathGenerator.nTeams = nTeams;
        ShortestPathGenerator.upperbound = upperbound;
        ShortestPathGenerator.timeSlots = ts;
        ShortestPathGenerator.visits = new int[nTeams];
        ShortestPathGenerator.costs = costs;
        times = new long[nTeams];
    }

    private static boolean resourceExtentionFunction(int team, int time, int from, int to) {
        if ((visits[to] > 0 && to != team) || (visits[to] > nTeams && to == team) && time != (2 * (nTeams - 1))) {
            return false;
        }
        if (isArcB(team, time, from, to, nTeams)) {
            if (b + 1 >= upperbound) {
                return false;
            }
            b++;
        } else {
            b = 0;
        }
        return true;
    }

    public static Tour generateTour(int team) {
        long start = System.nanoTime();
        for (int k = 0; k < nTeams; k++) {
            visits[k] = 0;
        }
        bestCost = Integer.MAX_VALUE;
        b = 0;
        bestArcs = new ArrayList<>();
        DFSrec(team, 0, team, 0);
        times[team] = (System.nanoTime() - start) / 1000;
        System.err.println("Best cost: " + bestCost + ", Time (µs): " + times[team]);
        return new Tour(bestArcs, bestCost);
    }

    private static boolean DFSrec(int team, int s, int from, int cost) {
        boolean tourFound = false;
        for (int i = 0; i < nTeams; i++) {
            if (resourceExtentionFunction(team, s, from, i)) {
                if (s == timeSlots && i == team) {
                    if (cost + costs[from][i] < bestCost) {
                        bestCost = cost + costs[from][i];
                        bestArcs.clear();
                        bestArcs.add(new Arc(s, from, i)); // is dit gegarandeert een pad naar homebase? Ja...?
                        return true;
                    }
                } else {
                    int b_prev = b;
                    visits[i]++;
                    if (cost + costs[from][i] >= bestCost)
                        continue;
                    if (DFSrec(team, s + 1, i, cost + costs[from][i])) {
                        bestArcs.addFirst(new Arc(s, from, i));
                        tourFound = true;
                    }
                    visits[i]--;
                    b = b_prev;
                }
            }
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
        boolean one = (t == i && t == j && s != 0 && s != 2 * (nTeams - 1));
        boolean two = (j != t && t != i);
        return (one || two) && isArcA(t, s, i, j, nTeams);
    }
}
