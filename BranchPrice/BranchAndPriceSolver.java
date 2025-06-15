package BranchPrice;

import java.util.ArrayList;
import java.util.Stack;

public class BranchAndPriceSolver {

    private final int nTeams;
    private final int[][] distanceMatrix;

    private double bestUpperBound = Double.POSITIVE_INFINITY;
    private BranchAndPrice bestNode = null;

    public BranchAndPriceSolver(int nTeams, int[][] distanceMatrix) {
        this.nTeams = nTeams;
        this.distanceMatrix = distanceMatrix;
    }

    public void solve() throws Exception {
        Stack<BranchAndPrice> openNodes = new Stack<>();
        openNodes.push(new BranchAndPrice(nTeams, distanceMatrix, new ArrayList<>()));

        while (!openNodes.isEmpty()) {
            BranchAndPrice node = openNodes.pop();

            node.solveLPWithColumnGeneration();

            if (!node.isLPSolvedToOptimality()) {
                System.out.println("⛔ LP niet optimaal — overslaan.");
                continue;
            }

            if (node.getLowerBound() >= bestUpperBound - 1e-5) {
                System.out.println("⛔ Bound >= incumbent — tak gesnoeid.");
                continue;
            }

            if (node.isIntegral()) {
                System.out.println("✅ Integer oplossing gevonden! Cost: " + node.getLowerBound());

                if (node.getLowerBound() < bestUpperBound - 1e-5) {
                    bestUpperBound = node.getLowerBound();
                    bestNode = node;
                }
            } else {
                System.out.println("📐 Fractionele oplossing — branchen...");
                openNodes.addAll(node.branch());
            }
        }

        if (bestNode != null) {
            System.out.println("\n🟢 Beste oplossing:");
            // print hier de geselecteerde tours of maak een Schedule
        } else {
            System.out.println("❌ Geen integer oplossing gevonden.");
        }
    }
}
