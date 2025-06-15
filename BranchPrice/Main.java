package BranchPrice;

import Utils.InputHandler;
import Utils.OutputHandeler;
import Utils.PrintHandler;
import com.gurobi.gurobi.*;

import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        long start = System.nanoTime();

        String fileName = "Data/Distances/NL6_distances.txt";
        InputHandler inputHandler = new InputHandler(fileName);
        int[][] distanceMatrix = inputHandler.getDistanceMatrix();
        int nTeams = distanceMatrix.length;

        PrintHandler printHandler = new PrintHandler();
        printHandler.printDistanceMatrixContents(distanceMatrix);

        // ======================== Branch-and-Price (BFS) ============================
        Queue<List<BranchingDecision>> queue = new LinkedList<>();
        queue.add(new ArrayList<>()); // Start met lege fixaties

        double bestCost = Double.MAX_VALUE;
        GRBModel bestModel = null;

        while (!queue.isEmpty()) {
            List<BranchingDecision> fixations = queue.poll();
            System.out.println("🌿 Verken nieuwe tak met " + fixations.size() + " fixaties");

            BranchAndPrice bnp = new BranchAndPrice(nTeams, distanceMatrix, fixations);
            int outcome = bnp.solveLP();
            // Outcome = 1 -> IP < Best Int
            // Outcome = 2 -> Lp  < Lowerbound maar IP > beste
            // Outcome = 3 -> LP > Lowerbound. Pruning
            // Outcome = 4 -> Lp  < Lowerbound maar IP > beste

            if (outcome == 1) {
                double cost = bnp.getIncumbent();
                if (cost < bestCost) {
                    bestCost = cost;
                    bestModel = bnp.getBestModel();
                    System.out.println("✅ Nieuw beste oplossing met kost: " + cost);
                }
            }

            if (outcome == 2 || outcome == 4) {
                BranchingDecision arc = bnp.selectMostFractionalArc();
                if (arc != null) {
                    // Maak linkse en rechtse branches
                    List<BranchingDecision> left = new ArrayList<>(fixations);
                    left.add(new BranchingDecision(arc.team, arc.from, arc.to, arc.slot, true));

                    List<BranchingDecision> right = new ArrayList<>(fixations);
                    right.add(new BranchingDecision(arc.team, arc.from, arc.to, arc.slot, false));

                    queue.add(left);
                    queue.add(right);
                } else {
                    System.out.println("⚠️ Geen bruikbare branching arc gevonden.");
                }
            }
        }

        // ======================== Resultaat tonen ============================
        if (bestModel != null) {
            System.out.println("🎯 Beste gevonden oplossing met kost: " + bestCost);
            // Optioneel: print tours
        } else {
            System.out.println("❌ Geen geldige oplossing gevonden.");
        }

        System.out.println("⏱️ Tijdsduur (s): " + (System.nanoTime() - start) / 1e9);
    }
}