package Utils;

import java.util.List;
import java.util.Map;

public class PrintHandler {
    public void printDistanceMatrixContents(int[][] table) {
        System.out.println("Distance matrix from file:");
        for (int[] row : table) {
            for (int value : row) {
                System.out.print(value + "\t");
            }
            System.out.println();
        }
        System.out.println();
    }

}
