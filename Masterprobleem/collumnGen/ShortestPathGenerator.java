package Masterprobleem.collumnGen;

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
    private static int bestCost = Integer.MAX_VALUE;
    private static int[][] costs;
    private static List<Arc> bestArcs;

    public ShortestPathGenerator(int nTeams, int upperbound, int ts, int[][] costs) {
        ShortestPathGenerator.nTeams = nTeams;
        ShortestPathGenerator.upperbound = upperbound;
        ShortestPathGenerator.timeSlots = ts;
        ShortestPathGenerator.visits = new int[nTeams];
        ShortestPathGenerator.costs = costs;
    }

    public static boolean resourceExtentionFunction(int team, int time, int from, int to, int b) {
        if (((visits[to] == 1 && to != team) || (visits[to] == 3 && to == team)) && time != 2 * (nTeams - 1))
            return false;
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

    private static Tour generateTour(int team) {
        for (int k = 0; k < nTeams; k++) {
            visits[k] = 0;
        }
        DFSrec(team, 0, team, 0);

        return new Tour(bestArcs, 0);
    }

    private static boolean DFSrec(int team, int s, int from, int cost) {
        boolean tourFound = false;
        for (int i = 0; i < nTeams; i++) {
            if (resourceExtentionFunction(team, s, from, i, b)) {
                if (s == timeSlots) {
                    if (cost + costs[from][i] < bestCost) {
                        bestCost = cost + costs[from][i];
                        bestArcs.clear();
                        bestArcs.add(new Arc(s, from, i)); // is dit gegarandeert een pad naar homebase? Ja...?
                        return true;
                    }
                } else {
                    visits[i]++;
                    if (cost + costs[from][i] >= bestCost)
                        continue;
                    if (DFSrec(team, s + 1, from, cost + costs[from][i])) {
                        bestArcs.add(new Arc(s, from, i));
                        tourFound = true;
                    }
                    visits[i]--;
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
