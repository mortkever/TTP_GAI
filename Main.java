public class Main {
    public static void main(String[] args) {
        // PrintHandler object to easily print
        PrintHandler printHandler = new PrintHandler();

        // ---------------------- Read input --------------------------
        // Path to input file
        String fileName = "Data/t4.txt";

        // Put the table in a 2D array
        InputHandler inputHandler = new InputHandler(fileName);
        int[][] distanceMatrix = inputHandler.getDistanceMatrixFromTXT();

        // Print the 2D array
        printHandler.printDistanceMatrixContents(distanceMatrix);
    }

}
