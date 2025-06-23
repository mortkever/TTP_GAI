package BranchPrice;

import Utils.InputHandler;
import Utils.OutputHandeler;
import Utils.PrintHandler;
import com.gurobi.gurobi.*;

import java.security.KeyStore;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        long start = System.nanoTime();

        String fileName = "Data/Distances/NL4_distances.txt";
        InputHandler inputHandler = new InputHandler(fileName);
        int[][] distanceMatrix = inputHandler.getDistanceMatrix();
        int nTeams = distanceMatrix.length;
        Set<String> globalBranchMemory = new HashSet<>();
        boolean useDFS  = false;
        PrintHandler printHandler = new PrintHandler();
        printHandler.printDistanceMatrixContents(distanceMatrix);

        // ======================== Branch-and-Price (BFS) ============================
        Deque<BranchNode> nodeQueue = new ArrayDeque<>();
        nodeQueue.addFirst(new BranchNode(new ArrayList<>(), null));

        Set<String> prunedPaths = new HashSet<>();

        double bestCost = Double.MAX_VALUE;
        GRBModel bestModel = null;

        while (!nodeQueue.isEmpty()) {
            BranchNode current = useDFS ? nodeQueue.pollFirst() : nodeQueue.pollLast();
            if (prunedPaths.contains(current.fixations.toString())) {
                System.out.println("⏭️ Fixaties overgeslagen (reeds gepruned): " + current.fixations);
                continue;
            }


            List<BranchingDecision> fixations = current.fixations;

            System.out.println("🌿 Verken nieuwe tak met " + fixations.size() + " fixaties");

            BranchAndPrice bnp = new BranchAndPrice(nTeams, distanceMatrix, fixations, bestCost, bestModel, globalBranchMemory);
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

                // Ook verder branchen indien mogelijk
                BranchingDecision arc = bnp.selectMostFractionalArc();
                if (arc != null) {
                    List<BranchingDecision> left = new ArrayList<>(fixations);
                    left.add(new BranchingDecision(arc.team, arc.from, arc.to, arc.slot, true));

                    List<BranchingDecision> right = new ArrayList<>(fixations);
                    right.add(new BranchingDecision(arc.team, arc.from, arc.to, arc.slot, false));

                    BranchNode leftNode = new BranchNode(left, current);
                    BranchNode rightNode = new BranchNode(right, current);
                    if (useDFS) {
                        nodeQueue.addFirst(rightNode);
                        nodeQueue.addFirst(leftNode);
                    } else {
                        nodeQueue.add(leftNode);
                        nodeQueue.add(rightNode);
                    }
                } else {
                    System.out.println("⚠️ Geen bruikbare branching arc gevonden.");
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

                    BranchNode leftNode = new BranchNode(left, current);
                    BranchNode rightNode = new BranchNode(right, current);
                    if (useDFS) {
                        nodeQueue.addFirst(rightNode);
                        nodeQueue.addFirst(leftNode);
                    } else {
                        nodeQueue.add(leftNode);
                        nodeQueue.add(rightNode);
                    }
                } else {
                    System.out.println("⚠️ Geen bruikbare branching arc gevonden.");
                }
            }

            if (current.parent != null && current.parent.isFullyExplored()) {
                String parentKey = current.parent.fixations.toString();
                prunedPaths.add(parentKey);
                System.out.println("🗑️ Parent node volledig onderzocht en gemarkeerd als gepruned: " + parentKey);
            }

            System.out.println("=========================");
            System.out.println("Data waarde: \n");
            System.out.println("Current best: " + bestCost + "\n");
            System.out.println("Current Incumbent:"+ bnp.getIncumbent() + "\n");
            System.out.println("Outcome / Soort: " + outcome);
            System.out.println("branches: " + nodeQueue.size() + "\n");
            System.out.println("branchingdecisions: "+ fixations.toString());
            System.out.println("=========================");
            if(bestCost < 8277){
                System.out.println("lets fuckign goo baby");
                return;
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