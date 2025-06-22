package Masterprobleem;

import Masterprobleem.columnGen.ShortestPathGenerator;
import Things.Match;
import Things.Schedule;
import Things.Team;
import com.gurobi.gurobi.*;

import java.util.*;

public class ColumnGenerationHelper {

    private GRBModel masterModel;

    // Stores dual prices
    private Map<String, Double> dualPrices;

    public ColumnGenerationHelper() throws GRBException {
        this.dualPrices = new HashMap<>();
    }

    public void setModel(GRBModel model) throws GRBException {
        this.checkValidModel(model);
        this.masterModel = model;
    }

    private void checkValidModel(GRBModel model) throws GRBException {
        if (model == null) {
            throw new IllegalArgumentException("Input model is null.");
        }

        if (model.get(GRB.IntAttr.NumVars) == 0 || model.get(GRB.IntAttr.NumConstrs) == 0) {
            throw new IllegalArgumentException("Input model is empty (no variables or constraints).");
        }
    }

    public void optimize() throws GRBException {
        this.masterModel.optimize();
    }

    // Method to extract and print duals
    public void extractDuals() {
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
                // System.out.println("\n\nConstraint: " + constrName + ", Dual Price: " +
                // dual);
                dualPrices.put(constrName, dual);
            }
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    public void printDuals() {
        for (Map.Entry<String, Double> entry : dualPrices.entrySet()) {
            System.out.println("Constraint: " + entry.getKey() + ", Dual Price: " + entry.getValue());
        }
    }

    public Map<String, Double> getDualPrices() {
        if (this.dualPrices.isEmpty()) {
            extractDuals();
        }

        return dualPrices;
    }

    public double getMu(int team){
        return dualPrices.get("convexity_" + team);
    }

    public double computeModifiedCost(
            int t, // team
            int i, // from
            int j, // to
            int s, // time slot index
            int[][] distanceMatrix,
            int numTeams) {

        // It will be calculated as c = X - Y - Z for readability
        // System.out.println("\nModified costs:");

        // X: base travel distance
        double cost = distanceMatrix[i][j];
        // System.out.println("\n\nBefore: " + cost);
        // System.out.println("Default cost: " + cost);

        // Y: subtract π_(ts) + π_(is) if i ≠ t (i.e., this is an away game)
        if (i != t) {
            double pi_ts = 0.0;
            double pi_is = 0.0;

            // The key is: "coupling_" + s + "_" + t;
            String pi_ts_key = "coupling_" + s + "_" + t;
            String pi_is_key = "coupling_" + s + "_" + i;

            pi_ts += dualPrices.getOrDefault(pi_ts_key, 0.0);
            pi_is += dualPrices.getOrDefault(pi_is_key, 0.0);

            // System.out.println("Y:");
            // System.out.println("\tpi_ts sum: " + pi_ts);
            // System.out.println("\tpi_is sum: " + pi_is);

            cost -= (pi_ts + pi_is);
        }

        // Z: subtract β_{ijs} or β_{jis}, unless s == 2(n - 1)
        if (s != 2 * (numTeams - 1)) {
            String betaKey;
            // The key is: "nrc_" + s + "_" + t + "_" + j

            if (i < j) {
                betaKey = "nrc_" + s + "_" + i + "_" + j;
            } else if (i > j) {
                betaKey = "nrc_" + s + "_" + j + "_" + i;
            } else {
                betaKey = null;
            }

            if (betaKey != null && dualPrices.containsKey(betaKey)) {
                // System.out.println("Z:");
                // System.out.println("\tBetaKey: " + betaKey);
                // System.out.println("\tBeta value: " + dualPrices.get(betaKey));
                cost -= dualPrices.get(betaKey);
            }
        }

        // System.out.println("Modified cost: " + cost);
        // System.out.println("After: " + cost);
        return cost;

        // Some extra information:
        // 3 type of constraints in master problem
        // - Coupling constraints: "matchOnce_i_j_s"
        // - Convexity constraints: "oneTourPerTeam_t"
        // - NRC constraints: "nrc_i_j_s"
    }

    public static List<Tour> generateOneFeasibleSuperColumn(int nTeams, int[][] distanceMatrix) {
        List<Tour> tours = generateFeasibleSuperColumns(nTeams, distanceMatrix, false);
        return tours;
    }

    public static List<Tour> generateTwoFeasibleSuperColumns(int nTeams, int[][] distanceMatrix) {
        List<Tour> tours1 = generateFeasibleSuperColumns(nTeams, distanceMatrix, false);
        List<Tour> tours2 = generateFeasibleSuperColumns(nTeams, distanceMatrix, true);

        List<Tour> allTours = new ArrayList<>();
        allTours.addAll(tours1);
        allTours.addAll(tours2);

        return allTours;
    }


    public static List<Tour> generateFeasibleSuperColumns(int nTeams, int[][] distanceMatrix, boolean reverseHomeAway) {
        /**
         * Generates one feasible solution (super columns) based on a round-robin scheduling pattern.
         * Meant to create a feasible ttp solution fast (the total cost is not important here).
         *
         * This method constructs full tours for all teams by:
         * - Using the standard circle method for the first half of the schedule:
         *      The circle method fixes one team and rotates the others around it,
         *      producing a round-robin tournament where every team plays every other team once.
         * - Mirroring the rounds to create the second half, reversing home/away roles to complete the double round-robin.
         * - Adding a final arc that brings each team back to its home location after the last match.
         *
         * Parameters:
         * - reverseHomeAway: if true, flips the home/away pattern. used to generate 2 distinct solutions.
         */

        int rounds = 2 * (nTeams - 1);
        List<List<Arc>> teamArcs = new ArrayList<>();
        int[] currentLocation = new int[nTeams];

        for (int t = 0; t < nTeams; t++) {
            teamArcs.add(new ArrayList<>());
            currentLocation[t] = t;
        }

        // First half: circle method (round-robin)
        for (int round = 0; round < nTeams - 1; round++) {
            for (int i = 0; i < nTeams / 2; i++) {
                int team1 = (round + i) % (nTeams - 1);
                int team2 = (nTeams - 1 - i + round) % (nTeams - 1);
                if (i == 0) team2 = nTeams - 1;

                boolean team1Home = reverseHomeAway ? round % 2 != 0 : round % 2 == 0;
                int home = team1Home ? team1 : team2;
                int away = team1Home ? team2 : team1;

                teamArcs.get(home).add(new Arc(round, currentLocation[home], home));
                currentLocation[home] = home;

                teamArcs.get(away).add(new Arc(round, currentLocation[away], home));
                currentLocation[away] = home;
            }
        }

        // Second half: mirrored round-robin with flipped home/away
        for (int round = nTeams - 1; round < rounds; round++) {
            int origRound = round - (nTeams - 1);
            for (int i = 0; i < nTeams / 2; i++) {
                int team1 = (origRound + i) % (nTeams - 1);
                int team2 = (nTeams - 1 - i + origRound) % (nTeams - 1);
                if (i == 0) team2 = nTeams - 1;

                boolean team1Home = reverseHomeAway ? origRound % 2 == 0 : origRound % 2 == 1;
                int home = team1Home ? team1 : team2;
                int away = team1Home ? team2 : team1;

                teamArcs.get(home).add(new Arc(round, currentLocation[home], home));
                currentLocation[home] = home;

                teamArcs.get(away).add(new Arc(round, currentLocation[away], home));
                currentLocation[away] = home;
            }
        }

        // Add return-home arcs
        for (int t = 0; t < nTeams; t++) {
            List<Arc> arcs = teamArcs.get(t);
            int lastVenue = currentLocation[t];
            arcs.add(new Arc(rounds, lastVenue, t));
        }

        // Wrap as Tour objects
        List<Tour> tours = new ArrayList<>();
        for (int t = 0; t < nTeams; t++) {
            tours.add(new Tour(teamArcs.get(t), distanceMatrix));
        }

        return tours;
    }


    public static void printTours(List<Tour> tours) {
        for (int t = 0; t < tours.size(); t++) {
            Tour tour = tours.get(t);
            System.out.println("Team " + t + " Tour (cost: " + tour.getRealCost() + "):");
            for (Arc arc : tour.getArcs()) {
                System.out.println("  " + arc);
            }
            System.out.println();
        }
    }

    public static Schedule getMatchSchedule(List<Tour> tours) {
        // Create a schedule with matches for each time slot

        // constants (in this scope)
        int nTeams = tours.size();
        int timeSlots = 2 * (nTeams - 1);
        int matchesPerSlot = nTeams / 2;

        // make 2 lists: 1 for home games, 1 for away games. Each list is a list of teams
        List<List<Arc>> homeLists = new ArrayList<>();
        List<List<Arc>> awayLists = new ArrayList<>();

        // add the arcs
        for (int t = 0; t < nTeams; t++) {
            // The tour of team t
            Tour tour = tours.get(t);

            // initialize the lists for each team
            homeLists.add(new ArrayList<>());
            awayLists.add(new ArrayList<>());

            // the first arc: from="home location", to="location of first match"
            // => check the "to" location of the arc
            for (Arc arc : tour.getArcs()) {
                int timeslot = arc.time;
                if (timeslot < 0 || timeslot >= timeSlots) continue; // skip invalid and return-home timeslots

                if (arc.to == t) {
                    // home game
                    homeLists.get(t).add(arc);
                } else {
                    // away game
                    awayLists.get(t).add(arc);
                }
            }
            System.out.println("Sizes: home=" + homeLists.get(t).size() + ", away=" + awayLists.get(t).size());
        }

        // now we can create the schedule using the 2 lists
        // schedule contains the matches. Hashmap that maps timeslot to a list of matches (each match has 2 teams)
        Schedule schedule = new Schedule();

        // populate the hashmap for easy reference later on
        for (int t = 0; t < timeSlots; t++) {
            List<Match> matches = new ArrayList<>();
            for (int m = 0; m < matchesPerSlot; m++) {
                matches.add(new Match(new Team(-1), new Team(-1))); // placeholders
            }
            schedule.addMatches(t, matches);
        }

        // Now we can fill the schedule
        // Fill the schedule with home games/arcs first, so we can map away games to the correct time slots later on.
        for (int t = 0; t < nTeams; t++) {
            List<Arc> homeArcs = homeLists.get(t);

            // Fill the schedule with home arcs
            for (Arc arc : homeArcs) {
                int timeslot = arc.time;
                if (timeslot < 0 || timeslot >= timeSlots) continue; // skip invalid and return-home timeslots

                // Find an empty slot for the home arc
                for (int m = 0; m < matchesPerSlot; m++) {
                    Match match = schedule.getMatches(timeslot).get(m);
                    if (match.getTeamHome().getID() == -1) {
                        // empty slot, fill it with the home team
                        match.setTeamHome(new Team(t));
                        break;
                    }
                }
            }
        }

        // Fill the schedule with away arcs
        for (int t = 0; t < nTeams; t++) {
            List<Arc> awayArcs = awayLists.get(t);

            for (Arc arc : awayArcs) {
                int timeslot = arc.time;
                if (timeslot < 0 || timeslot >= timeSlots)
                    throw new IllegalStateException("Invalid timeslot: " + timeslot);

                // Find the corresponding homegame. Let's say the t' = arc.to is the away location for team t in the arc
                // find a slot where t' is the homegame of that match
                for (int m = 0; m < matchesPerSlot; m++) {
                    Match match = schedule.getMatches(timeslot).get(m);

                    if (match.getTeamHome().getID() != -1 && match.getTeamAway().getID() == -1 &&
                            match.getTeamHome().getID() == arc.to) {
                        // match found, fill it with the away team
                        match.setTeamAway(new Team(t));
                        break;
                    }
                }
            }
        }

        // temp placeholder
        schedule.setObjectiveValue(10);

        return schedule;
    }


    // ================== Strategies for initial solution ==================
    public static void addInitialSolution(int strategie, Masterproblem master, ShortestPathGenerator spg,
                                          int nTeams, int timeSlots, int[][] distanceMatrix) throws GRBException {
        if (strategie == 1) {
            // Add 1 compact formulation solution
            CompactModel compactModel = new CompactModel(nTeams, timeSlots, distanceMatrix);
            compactModel.getFirstSolution();
            GRBVar[][][][] x = compactModel.getFirstSolution();

            for (int t = 0; t < nTeams; t++) {
                List<Arc> arcs = new ArrayList<>();
                double totalCost = 0.0;

                for (int s = 0; s < timeSlots + 1; s++) {
                    for (int i = 0; i < nTeams; i++) {
                        for (int j = 0; j < nTeams; j++) {
                            if (x[t][s][i][j].get(GRB.DoubleAttr.X) > 0.5) {
                                arcs.add(new Arc(s, i, j));
                                totalCost += distanceMatrix[i][j];
                            }
                        }
                    }
                }

                Tour tour = new Tour(arcs, totalCost, -10000);
                master.addTour(t, tour);
                spg.addTour(t, tour);
            }
        }
        else if (strategie == 2) {
            // Add multiple compact formulation solutions
            CompactModel compactModel = new CompactModel(nTeams, timeSlots, distanceMatrix);
            List<double[][][][]> solutions = compactModel.getMultipleSolutions(2);

            System.out.println("\n------------------");
            System.out.println("Adding tours to master...");
            for (double[][][][] xSol : solutions) {
                // xSol: [t][s][i][j]
                for (int t = 0; t < nTeams; t++) {
                    List<Arc> arcs = new ArrayList<>();
                    double totalCost = 0.0;

                    for (int s = 0; s < timeSlots + 1; s++) {
                        for (int i = 0; i < nTeams; i++) {
                            for (int j = 0; j < nTeams; j++) {
                                if (xSol[t][s][i][j] > 0.5) {
                                    arcs.add(new Arc(s, i, j));
                                    totalCost += distanceMatrix[i][j];
                                }
                            }
                        }
                    }

                    Tour tour = new Tour(arcs, totalCost);
                    //System.out.println(tour);
                    //System.out.println("\nBefore adding master and spg tours");
                    master.addTour(t, tour); // This adds the new column
                    spg.addTour(t, tour);
                    //System.out.println("After adding master and spg tours\n");
                }
            }
        }
        else if (strategie == 3) {
            // Add super columns

            // Debug stuff ============================
//        List<Tour> superTours = ColumnGenerationHelper.generateFeasibleSuperColumns(nTeams, distanceMatrix);
//        System.out.println("\n\nSuper column test:");
//        System.out.println("Aantal super tours: " + superTours.size());
//        for (Tour tour : superTours) {
//            System.out.println(tour);
//        }
//        Schedule superSchedule = ColumnGenerationHelper.getMatchSchedule(superTours);
//        superSchedule.printSchedule();
//        ScheduleValidator scheduleValidator = new ScheduleValidator(superSchedule, distanceMatrix);
//        scheduleValidator.validate();
//        System.out.println("\n\n\n");
            // Debug stuff ============================

            List<Tour> superTours = ColumnGenerationHelper.generateOneFeasibleSuperColumn(nTeams, distanceMatrix);
            System.out.println("\n\nSuper column test:");
            System.out.println("Aantal super tours: " + superTours.size());

            for (int t = 0; t < nTeams; t++) {
                System.out.println("Team " + t + ":" + superTours.get(t));
                master.addTour(t, superTours.get(t)); // This adds the new column
                spg.addTour(t, superTours.get(t));
            }
        }
        else if (strategie == 4) {
            // Add multiple super columns

            List<Tour> superTours = ColumnGenerationHelper.generateTwoFeasibleSuperColumns(nTeams, distanceMatrix);
            System.out.println("\n\nSuper column test:");
            System.out.println("Aantal super tours: " + superTours.size());

            for (int t = 0; t < superTours.size(); t++) {
                // there are multiple solutions: t % nTeams to get the correct team index
                int teamIndex = t % nTeams;
                System.out.println("Team " + teamIndex + ":" + superTours.get(t));
                master.addTour(teamIndex, superTours.get(t)); // This adds the new column
                spg.addTour(teamIndex, superTours.get(t));
            }
        }
        else if (strategie == 5) {
            // Add 1 solution of compact formulation and 1 solution of the super columns

            // === Compact solution ===
            CompactModel compactModel = new CompactModel(nTeams, timeSlots, distanceMatrix);
            compactModel.getFirstSolution();
            GRBVar[][][][] x = compactModel.getFirstSolution();
            System.out.println("Compact formulation tous:");
            double totalCostPrint = 0.0;

            for (int t = 0; t < nTeams; t++) {
                List<Arc> arcs = new ArrayList<>();
                double totalCost = 0.0;

                for (int s = 0; s < timeSlots + 1; s++) {
                    for (int i = 0; i < nTeams; i++) {
                        for (int j = 0; j < nTeams; j++) {
                            if (x[t][s][i][j].get(GRB.DoubleAttr.X) > 0.5) {
                                arcs.add(new Arc(s, i, j));
                                totalCost += distanceMatrix[i][j];
                            }
                        }
                    }
                }

                Tour tour = new Tour(arcs, totalCost);
                System.out.println("Team " + t + ": " + tour);
                master.addTour(t, tour);
                spg.addTour(t, tour);
                totalCostPrint += totalCost;
            }
            System.out.println("Total cost of compact formulation tours: " + totalCostPrint);

            // === Super column solution ===
            List<Tour> superTours = ColumnGenerationHelper.generateOneFeasibleSuperColumn(nTeams, distanceMatrix);
            System.out.println("\n\nSuper column test (added to strategy 5):");
            System.out.println("Aantal super tours: " + superTours.size());

            for (int t = 0; t < nTeams; t++) {
                System.out.println("Team " + t + " (super): " + superTours.get(t));
                master.addTour(t, superTours.get(t));
                spg.addTour(t, superTours.get(t));
            }
            System.out.println("Total cost of super column tours: " + superTours.stream().mapToDouble(tour -> tour.getRealCost()).sum());
        }

    }

}
