import com.gurobi.gurobi.*;


public class TTPGurobiSolver {


    public static void main(String args[]) throws GRBException {
        GRBModel model = new GRBModel(new GRBEnv());
        model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);




    }
}
