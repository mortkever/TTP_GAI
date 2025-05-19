package Masterprobleem.columnGen;

import com.gurobi.gurobi.*;
import java.util.HashMap;
import java.util.Map;

public class ColumnGenerationHelper {

    private GRBModel masterModel;

    // Stores dual prices
    private Map<String, Double> dualPrices;
    private double[][][][] modCostCache;

    public ColumnGenerationHelper(GRBModel model) throws GRBException {
        // Check if the model is valid
        this.checkValidModel(model);

        this.masterModel = model;
        this.dualPrices = new HashMap<>();
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
            int t,                  // team
            int i,                  // from
            int j,                  // to
            int s,                  // time slot index
            int[][] distanceMatrix,
            int numTeams
    ) {
        if(modCostCache[t][s][i][j] != Double.MAX_VALUE){
            return modCostCache[t][s][i][j];
        }
        // It will be calculated as c = X - Y - Z for readability
        //System.out.println("\nModified costs:");

        // X: base travel distance
        double cost = distanceMatrix[i][j];
        //System.out.println("Default cost: " + cost);

        // Y: subtract π_(ts) + π_(is) if i ≠ t (i.e., this is an away game)
        if (i != t) {
            double pi_ts = 0.0;
            double pi_is = 0.0;

            for (int opp = 0; opp < numTeams; opp++) {
                String pi_ts_key = "matchOnce_" + t + "_" + opp + "_" + s;
                String pi_is_key = "matchOnce_" + i + "_" + opp + "_" + s;

                pi_ts += dualPrices.getOrDefault(pi_ts_key, 0.0);
                pi_is += dualPrices.getOrDefault(pi_is_key, 0.0);
            }

            //System.out.println("Y:");
            //System.out.println("\tpi_ts sum: " + pi_ts);
            //System.out.println("\tpi_is sum: " + pi_is);

            cost -= (pi_ts + pi_is);
        }

        // Z: subtract β_{ijs} or β_{jis}, unless s == 2(n - 1)
        if (s != 2 * (numTeams - 1)) {
            String betaKey;

            if (i < j) {
                betaKey = "nrc_" + i + "_" + j + "_" + s;
            } else if (i > j) {
                betaKey = "nrc_" + j + "_" + i + "_" + s;
            } else {
                betaKey = null;
            }

            if (betaKey != null && dualPrices.containsKey(betaKey)) {
                //System.out.println("Z:");
                //System.out.println("\tBetaKey: " + betaKey);
                //System.out.println("\tBeta value: " + dualPrices.get(betaKey));
                cost -= dualPrices.get(betaKey);
            }
        }

        //System.out.println("Modified cost: " + cost);
        modCostCache[t][s][i][j] = cost;
        return cost;

        // Some extra information:
//         3 type of constraints in master problem
//         - Coupling   constraints: "matchOnce_i_j_s"
//         - Convexity  constraints: "oneTourPerTeam_t"
//         - NRC        constraints: "nrc_i_j_s"
    }
}
