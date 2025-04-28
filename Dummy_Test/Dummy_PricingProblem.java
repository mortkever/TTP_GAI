package Dummy_Test;

import java.util.List;
import java.util.Map;

public class Dummy_PricingProblem {
    private int team; // which team we are pricing for
    private double lambda; // dual price for one_tour constraint
    private Map<String, Double> dualPrices; // all slot dual prices

    private int[][] distanceMatrix; // travel distances

    private static final int U = 3; // max consecutive home/away games

    public Dummy_PricingProblem(int team, double lambda, Map<String, Double> dualPrices, int[][] distanceMatrix) {
        this.team = team;
        this.lambda = lambda;
        this.dualPrices = dualPrices;
        this.distanceMatrix = distanceMatrix;
    }

    public void buildAndSolvePricingGraph() {
        // Build network according to Irnich 2010
        // Source -> Slot layers -> Sink
        // Build nodes, arcs, costs, etc.

        // TODO: create Node, Arc classes (next step)
        // TODO: implement label-setting or simple shortest path for small dummy

        System.out.println("Building pricing graph for Team " + team);
        // Debug printing
    }

    public void printNewColumn(List<Dummy_Node> path) {
        System.out.println("New Column (Tour) for Team " + team + ":");

        for (int i = 1; i < path.size() - 1; i++) { // Skip Source and Sink
            Dummy_Node current = path.get(i);

            String homeOrAway = (current.venue == team) ? "Home" : "Away";
            int opponent = current.opponent;

            System.out.println("Slot " + current.slot + ": " + homeOrAway + " vs Team " + opponent);
        }
    }

}
