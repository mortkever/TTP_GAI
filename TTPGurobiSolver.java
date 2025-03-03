import com.gurobi.gurobi.*;


public class TTPGurobiSolver {


    public static void main(String args[]) throws GRBException {


        String fileName = "Data/NL4.xml";
        PrintHandler printHandler = new PrintHandler();

        // Put the table in a 2D array
        InputHandler inputHandler = new InputHandler(fileName);
        int[][] distanceMatrix = inputHandler.getDistanceMatrix();
        printHandler.printDistanceMatrixContents(distanceMatrix);


        // Model opzetten
        int nRounds = 10;
        int nTeams = 5;

        GRBEnv env =  new GRBEnv();
        GRBModel model = new GRBModel(env);

        GRBVar[][][] x = new GRBVar[nTeams][nTeams][nRounds];

        for (int i = 0; i < nTeams; i++) {
            for (int j = 0; j < nTeams; j++) {
                if (i != j) {
                    for (int s = 0; s < nRounds; s++) {
                        x[i][j][s] = model.addVar(0, 1, distances[i][j], GRB.BINARY, "x(" + i + "," + j + "," + s + ")");
                    }
                }
            }
        }

        // Constraint 1: Elk Team Speelt Exact één wegstrijd per speeldag

        for (int i = 0; i < nTeams; i++) {
            for (int s = 0; s < nRounds; s++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int j = 0; j < nTeams; j++) {
                    if (i != j) {
                        expr.addTerm(1, x[i][j][s]);
                        expr.addTerm(1, x[j][i][s]);
                    }
                }
                model.addConstr(expr, GRB.EQUAL, 1, "game_per_day(" + i + "," + s + ")");
            }
        }

        // Constraint 2:


        // Minimale afstand xij,s-1 & xji,s

        // Constraint 1







        // Doelfuntctie

        GRBLinExpr objective =  new GRBLinExpr();
        for (int i = 0; i < nTeams; i++) {
            for (int j = 0; j < nTeams; j++) {
                if (i != j) {
                    for (int s = 0; s < nRounds; s++) {
                        objective.addTerm(distances[i][j], x[i][j][s]);
                    }
                }
            }
        }
        model.setObjective(objective,GRB.MINIMIZE);
        model.optimize();

        // Resultaten tonen
        for (int i = 0; i < nTeams; i++) {
            for (int j = 0; j < nTeams; j++) {
                if (i != j) {
                    for (int s = 0; s < nRounds; s++) {
                        if (x[i][j][s].get(GRB.DoubleAttr.X) > 0.5) {
                            System.out.println("Speeldag " + (s + 1) + ": " + i + " speelt uit tegen " + j);
                        }
                    }
                }
            }
        }

        model.dispose();
        env.dispose();









    }
}
