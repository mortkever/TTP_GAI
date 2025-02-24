import com.gurobi.gurobi.*;


public class KnapsackGurobiExample {
    static int nItems = 10;
    static int prices[] = new int[]{ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
    static int weights[] = new int[]{ 29, 23, 34, 43, 65, 43, 62, 80, 90, 80 };
    static int capacity = 100;

    public static void main(String args[]) throws GRBException {
        GRBModel model = new GRBModel(new GRBEnv());
        model.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);

        // add variables
        GRBVar x[] = new GRBVar[nItems];
        for (int i = 0; i < nItems; i++)
            x[i] = model.addVar(0, 1, prices[i], GRB.BINARY, "x(" + i + ")");
        model.update();

        // add constraint
        GRBLinExpr expr = new GRBLinExpr();
        for (int i = 0; i < nItems; i++)
            expr.addTerm(weights[i], x[i]);
        model.addConstr(expr, '<', capacity, "cap");

        model.optimize();

        System.out.println();
        for (int i = 0; i < nItems; i++)
            if (x[i].get(GRB.DoubleAttr.X) > 0.5)
                System.out.println("Item " + i + " was chosen");
        System.out.println("Total value is " + model.get(GRB.DoubleAttr.ObjVal));
    }
}
