package Masterprobleem.columnGen;

import com.gurobi.gurobi.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ColumnGenerationHelper {

    private GRBModel masterModel;

    // Stores dual prices
    private Map<String, Double> dualPrices;
    private double[][][][] modCostCache;
    private boolean randCost = false;

    public ColumnGenerationHelper() throws GRBException {
        this.dualPrices = new HashMap<>();
    }

    public void setModel(GRBModel model) throws GRBException {
        this.checkValidModel(model);
        this.masterModel = model;
    }

    private void checkValidModel(GRBModel model) throws GRBException {
        if (model == null) {
            throw new IllegalArgumentException("Input model is null.");
        }

        if (model.get(GRB.IntAttr.NumVars) == 0 || model.get(GRB.IntAttr.NumConstrs) == 0) {
            throw new IllegalArgumentException("Input model is empty (no variables or constraints).");
        }
    }

    public void optimize() throws GRBException {
        this.masterModel.optimize();
    }

    // Method to extract and print duals
    public void extractDuals() {
        try {
            // Make sure model is optimized
            if (masterModel.get(GRB.IntAttr.Status) != GRB.Status.OPTIMAL) {
                System.out.println("Warning: Master problem is not optimal yet.");
                return;
            }

            // Loop through all constraints
            for (GRBConstr constr : masterModel.getConstrs()) {
                String constrName = constr.get(GRB.StringAttr.ConstrName);
                double dual = constr.get(GRB.DoubleAttr.Pi); // Pi = dual value
                // System.out.println("\n\nConstraint: " + constrName + ", Dual Price: " +
                // dual);
                dualPrices.put(constrName, dual);
            }
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    public void printDuals() {
        for (Map.Entry<String, Double> entry : dualPrices.entrySet()) {
            System.out.println("Constraint: " + entry.getKey() + ", Dual Price: " + entry.getValue());
        }
    }

    public Map<String, Double> getDualPrices() {
        if (this.dualPrices.isEmpty()) {
            extractDuals();
        }

        return dualPrices;
    }

    public void resetCache(int nTeams, int timeSlots) {
        modCostCache = new double[nTeams][timeSlots + 1][nTeams][nTeams];
        for (int t = 0; t < nTeams; t++) {
            for (int s = 0; s < timeSlots + 1; s++) {
                for (int i = 0; i < nTeams; i++) {
                    for (int j = 0; j < nTeams; j++) {
                        modCostCache[t][s][i][j] = Double.MAX_VALUE;
                    }
                }
            }
        }
    }

    public double computeModifiedCost(
            int t, // team
            int i, // from
            int j, // to
            int s, // time slot index
            int[][] distanceMatrix,
            int numTeams) {
        if (modCostCache[t][s][i][j] != Double.MAX_VALUE) {
            return modCostCache[t][s][i][j];
        }

        // It will be calculated as c = X - Y - Z for readability
        // System.out.println("\nModified costs:");

        // X: base travel distance
        double cost = distanceMatrix[i][j];
        // System.out.println("\n\nBefore: " + cost);
        // System.out.println("Default cost: " + cost);

        // Y: subtract π_(ts) + π_(is) if i ≠ t (i.e., this is an away game)
        if (i != t) {
            double pi_ts = 0.0;
            double pi_is = 0.0;

            // The key is: "coupling_" + s + "_" + t;
            String pi_ts_key = "coupling_" + s + "_" + t;
            String pi_is_key = "coupling_" + s + "_" + i;

            pi_ts += dualPrices.getOrDefault(pi_ts_key, 0.0);
            pi_is += dualPrices.getOrDefault(pi_is_key, 0.0);

            // System.out.println("Y:");
            // System.out.println("\tpi_ts sum: " + pi_ts);
            // System.out.println("\tpi_is sum: " + pi_is);

            cost -= (pi_ts + pi_is);
        }

        // Z: subtract β_{ijs} or β_{jis}, unless s == 2(n - 1)
        if (s != 2 * (numTeams - 1)) {
            String betaKey;
            // The key is: "nrc_" + s + "_" + t + "_" + j

            if (i < j) {
                betaKey = "nrc_" + s + "_" + i + "_" + j;
            } else if (i > j) {
                betaKey = "nrc_" + s + "_" + j + "_" + i;
            } else {
                betaKey = null;
            }

            if (betaKey != null && dualPrices.containsKey(betaKey)) {
                // System.out.println("Z:");
                // System.out.println("\tBetaKey: " + betaKey);
                // System.out.println("\tBeta value: " + dualPrices.get(betaKey));
                cost -= dualPrices.get(betaKey);
            }
        }

        // System.out.println("Modified cost: " + cost);
        // System.out.println("After: " + cost);

        // Obtain a number between [0 - 49].
        if (randCost) {
            Random rand = new Random();
            cost = rand.nextInt(1500);
        }
        modCostCache[t][s][i][j] = cost;
        // Random

        return cost;

        // Some extra information:
        // 3 type of constraints in master problem
        // - Coupling constraints: "matchOnce_i_j_s"
        // - Convexity constraints: "oneTourPerTeam_t"
        // - NRC constraints: "nrc_i_j_s"
    }

    public void setRandCost(boolean useRandCost) {
        randCost = useRandCost;
    }
}
