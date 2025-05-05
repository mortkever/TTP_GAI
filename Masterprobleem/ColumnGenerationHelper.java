package Masterprobleem;

import com.gurobi.gurobi.*;
import java.util.HashMap;
import java.util.Map;

public class ColumnGenerationHelper {

    private GRBModel masterModel;

    // Stores dual prices
    private Map<String, Double> dualPrices;

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
                System.out.println("\n\nConstraint: " + constrName + ", Dual Price: " + dual);
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
        if(this.dualPrices.isEmpty()) {
            extractDuals();
        }

        return dualPrices;
    }

    public double computeModifiedCost(
            int t,
            int i,                 // from
            int j,                 // to
            int s,                 // time slot index
            Map<String, Double> duals,
            int[][] distanceMatrix,
            int numTeams
    ) {
        // It will be calculated as c = X - Y - Z for readability
        System.out.println("\nModified costs:");

        // X: base travel distance
        double cost = distanceMatrix[i][j];
        System.out.println("Default cost: " + cost);

        // Y: subtract π_(ts) + π_(is) if i ≠ t (i.e., this is an away game)
        if (i != t) {
            String pi_ts_key = "slot_" + s + "_team_" + t;
            String pi_is_key = "slot_" + s + "_team_" + i;

            double pi_ts = duals.getOrDefault(pi_ts_key, 0.0);
            double pi_is = duals.getOrDefault(pi_is_key, 0.0);
            System.out.println("Y:");
            System.out.println("\tpi_ts: " + pi_ts);
            System.out.println("\tpi_is: " + pi_is);

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
                betaKey = null; // self-loop, shouldn't happen
            }

            if (betaKey != null && duals.containsKey(betaKey)) {
                System.out.println("Y:");
                System.out.println("\tBetaKey: " + betaKey);
                cost -= duals.get(betaKey);
            }
        }

        System.out.println("Modified cost: " + cost);
        return cost;
    }
}
