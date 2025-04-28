package Dummy_Test;

import com.gurobi.gurobi.*;
import java.util.HashMap;
import java.util.Map;

public class ColumnGenerationHelper {

    private GRBModel masterModel;

    // Stores dual prices
    private Map<String, Double> dualPrices;

    public ColumnGenerationHelper(GRBModel model) {
        this.masterModel = model;
        this.dualPrices = new HashMap<>();
    }

    // Method to extract and print duals
    public void extractAndPrintDuals() {
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

                dualPrices.put(constrName, dual);

                System.out.println("Constraint: " + constrName + ", Dual Price: " + dual);
            }
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Double> getDualPrices() {
        return dualPrices;
    }
}
