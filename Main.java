import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;

import com.gurobi.gurobi.*;

import java.awt.*;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.w3c.dom.Node;

public class Main {
    public static void main(String[] args) throws GRBException {

        String fileName = "Data/NL4.xml";

        // Put the table in a 2D array
        InputHandler inputHandler = new InputHandler(fileName);
        int[][] distanceMatrix = inputHandler.getDistanceMatrix();
        int nTeams = distanceMatrix.length;
        int timeSlots = 2 * nTeams;
        int upperbound = 3;

        // Print the 2D array
        printHandler.printDistanceMatrixContents(distanceMatrix);

        // ---------------------- Voorbeeld code --------------------------
        Schedule schedule = new Schedule();
        schedule.addFeasibleSchedule();

        // Stap 4: Schema printen
        schedule.printSchedule();

        // Stap 5: Specifieke ronde ophalen
        System.out.println("Wedstrijden in Ronde 2:");
        for (Match match : schedule.getMatches(2)) {
            System.out.println("  " + match);
        }

        // Stap 6: Validate solution
        ScheduleValidator scheduleValidator = new ScheduleValidator(schedule);
        scheduleValidator.validate();

        GRBModel model = new GRBModel(new GRBEnv());
        model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);

        // -----------------------------------------------------------------------------------------------------------
        // variables
        // -----------------------------------------------------------------------------------------------------------
        GRBLinExpr doelstelling = new GRBLinExpr();

        GRBVar x[][][][] = new GRBVar[nTeams][timeSlots][nTeams][nTeams];
        for (int t = 0; t < nTeams; t++) {
            for (int s = 0; s < timeSlots; s++) {
                for (int i = 0; i < nTeams; i++) {
                    for (int j = 0; j < nTeams; j++) {
                        x[t][s][i][j] = model.addVar(0, 1, 0, GRB.BINARY,
                                "x( s " + s + ", i " + i + ", j " + j + ")");
                        doelstelling.addTerm((double) distanceMatrix[i][j], x[t][s][i][j]);
                    }
                }
            }
        }

        // constraint 3
        for (int t = 0; t < nTeams; t++) {
            for (int i = 0; i < nTeams; i++) {
                if (i != t) {
                    GRBLinExpr expr = new GRBLinExpr();
                    for (int s = 0; s < timeSlots; s++) {
                        for (int j = 0; j < nTeams; j++) {
                            expr.addTerm(1.0, x[t][s][i][j]);
                        }
                    }
                    model.addConstr(expr, '=', 1, "visitation_" + t + "_" + i);
                }
            }
        }

        // constraint 4 consecutive home games
        for (int t = 0; t < nTeams; t++) {
            for (int s = 0; s < timeSlots - upperbound; s++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int u = 0; u < upperbound; u++) {
                    expr.addTerm(1.0, x[t][s + u][t][t]);
                }
                model.addConstr(expr, '<', upperbound, "breaks_" + t);
            }
        }

        model.optimize();
    }
}