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

        GRBModel model = new GRBModel(new GRBEnv());
        model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);

        String fileName = "Data/NL4.xml";

        // Put the table in a 2D array
        InputHandler inputHandler = new InputHandler(fileName);
        int[][] distanceMatrix = inputHandler.getDistanceMatrix();
        int nTeams = distanceMatrix.length;
        int timeSlots = 2 * nTeams;

    }
}