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

        int upperbound = 3;
        // Put the table in a 2D array

        PrintHandler printHandler = new PrintHandler();
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


        String fileName = "Data/NL4.xml";

        // Put the table in a 2D array
        InputHandler inputHandler = new InputHandler(fileName);
        int[][] distanceMatrix = inputHandler.getDistanceMatrix();
        int nTeams = distanceMatrix.length;
        int timeSlots = 2 * nTeams;
        printHandler.printDistanceMatrixContents(distanceMatrix);

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

        // Contrait 1: FlowConversion
        for (int t = 0; t < nTeams; t++){
            for(int i =0; i< nTeams; i++){
                for(int s =1; s<timeSlots;s++){

                    GRBLinExpr flow = new GRBLinExpr();

                    for (int j = 0; j < nTeams; j++) {
                        flow.addTerm(1, x[t][s - 1][j][i]); // Kwam van team j naar i op s-1
                    }

                    for (int j = 0; j < nTeams; j++) {
                        flow.addTerm(-1, x[t][s][i][j]); // Vertrekt van i naar team j op s
                    }
                    model.addConstr(flow, GRB.EQUAL, 0, "flow_conservation(t=" + t + ",i=" + i + ",s=" + s + ")");

                }
            }
        }

        // Constraint 5 :

        for (int t = 0; t < nTeams; t++) {  // For each team
            for (int s = 0; s < timeSlots; s++) {  // For each timeslot
                GRBLinExpr constraint = new GRBLinExpr();

                // First summation: Flow from i to j at time t
                for (int i = 0; i < nTeams; i++) {
                    for (int j = 0; j < nTeams; j++) {
                        constraint.addTerm(1, x[t][s][i][j]);
                    }
                }

                // Second summation: Flow from other teams t' to same team s
                for (int t2 = 0; t2 < nTeams; t2++) {  // FIX: Loop over teams, not time slots
                    if (t2 != t) {  // Avoid duplicate assignment
                        for (int j = 0; j < nTeams; j++) {
                            constraint.addTerm(1, x[t2][s][j][t]);  // Ensure valid indexing
                        }
                    }
                }

                // Add constraint: Exactly one match per team s
                model.addConstr(constraint, GRB.EQUAL, 1, "flow_constraint_s" + s + "_t" + t);
            }
        }

        // Constraint 6





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

        if(model.get(GRB.IntAttr.Status) == GRB.OPTIMAL){
            System.out.println("oplossing gevonden");
            System.out.println("Objective value total Distance: " + model.get(GRB.DoubleAttr.ObjVal));

            System.out.println("\nMatch Schedule:");
            for (int t = 0; t < nTeams; t++) {
                for (int s = 0; s < timeSlots; s++) {
                    for (int i = 0; i < nTeams; i++) {
                        for (int j = 0; j < nTeams; j++) {
                            if (x[t][s][i][j].get(GRB.DoubleAttr.X) > 0.5) { // Alleen actieve variabelen tonen
                                System.out.println("Team " + t + " plays from " + i + " to " + j + " at time " + s);
                            }
                        }
                    }
                }
            }


        }
        else{
            System.out.println("geen oplossing gevonden. ");
        }

    }
}