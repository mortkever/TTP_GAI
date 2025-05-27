package Masterprobleem;

import com.gurobi.gurobi.GRBException;

import Masterprobleem.Arc;
import Masterprobleem.Tour;
import Masterprobleem.TourRepository;
import Masterprobleem.columnGen.ShortestPathGenerator;
import Masterprobleem.columnGen.ColumnGenerationHelper;
import com.gurobi.gurobi.GRBException;

import java.util.*;

public class BranchAndPrice {


    public static class Node{
        public final Set<Arc> forcedArcs;
        public final Set<Arc> forbiddenArcs;

        public Node() {
            this.forcedArcs = new HashSet<>();
            this.forbiddenArcs = new HashSet<>();
        }

        public Node(Set<Arc> forced, Set<Arc> forbidden) {
            this.forcedArcs = new HashSet<>(forced);
            this.forbiddenArcs = new HashSet<>(forbidden);
        }
    }

    private TourRepository tourRepo;
    private int[][] distanceMatrix;
    private Solution BestSolution;

    public BranchAndPrice(TourRepository tourRepo, int[][] distanceMatrix) {
        this.tourRepo = tourRepo;
        this.distanceMatrix = distanceMatrix;
        this.BestSolution = null;
    }
    public void solve() throws GRBException {
        Node root = new Node();
        branchAndPrice(root);
        if (BestSolution != null) {
            System.out.println("Best integer solution cost = " + BestSolution.cost);
            // optionally: print tours
        } else {
            System.out.println("No integer solution found.");
        }
    }

    private void branchAndPrice(Node node) throws GRBException {
        // 1) Column generation on this node
        Solution sol = columnGeneration(node);

        if (sol.isIntegral) {
            // update bestSolution if better
            if (BestSolution == null || sol.cost < BestSolution.cost) 
            {BestSolution= sol;}
        } else {
            // 2) select an arc to branch on
            Arc branchArc = selectBranchingArc(sol);

            // 3a) Include branch
            Node include = new Node(node.forcedArcs, node.forbiddenArcs);
            include.forcedArcs.add(branchArc);
            branchAndPrice(include);

            // 3b) Exclude branch
            Node exclude = new Node(node.forcedArcs, node.forbiddenArcs);
            exclude.forbiddenArcs.add(branchArc);
            branchAndPrice(exclude);
        }
    }

    private Arc selectBranchingArc(Solution sol) {
        // TODO: implement heuristic to pick fractional arc
        throw new UnsupportedOperationException("selectBranchingArc not implemented");
    }

    private Solution columnGeneration(Node node) {
        // TODO: implement column generation
        throw new UnsupportedOperationException("columnGeneration not implemented");

    }

    private static class Solution {
        public double cost;
        public Map<Integer, List<Tour>> tours;
        public boolean isIntegral;
    }
}
