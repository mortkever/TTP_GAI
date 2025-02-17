public class PrintHandler {
    public int[][] printDistanceMatrixContents(int[][] table) {
        System.out.println("Table from file:");
        for (int[] row : table) {
            for (int value : row) {
                System.out.print(value + "\t");
            }
            System.out.println();
        }
        return table;
    }
}
