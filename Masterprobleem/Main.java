package Masterprobleem;

import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;

import java.util.*;

public class Main {
    public static void main(String[] args) throws GRBException {

        int upperbound = 3;
        PrintHandler printHandler = new PrintHandler();

        // String fileName = "Data/NL4.xml";
        String fileName = "Data/Distances/NL4_distances.txt";
        // String fileName = "Data/Distances/NL16_distances.txt";

        // ====================== Distance matrix =========================
        InputHandler inputHandler = new InputHandler(fileName);
        int[][] distanceMatrix = inputHandler.getDistanceMatrix();
        int nTeams = distanceMatrix.length;
        int timeSlots = 2 * (nTeams - 1) + 1;
        printHandler.printDistanceMatrixContents(distanceMatrix);



        List<Tour> initialTours = generateInitialTours(distanceMatrix, 2 * (nTeams - 1), upperbound);


        Masterproblem master = new Masterproblem(new GRBEnv());

        for (Tour tour : initialTours) {
            if (validateTour(tour, distanceMatrix, upperbound)) {
                master.addTour(tour);
            } else {
                System.out.println(">> Ongeldige tour voor team " + tour.team + " werd niet toegevoegd.");
            }
        }

        Set<Integer> teams = new HashSet<>();
        for (int i = 0; i < nTeams; i++) teams.add(i);
        Set<Integer> slots = new HashSet<>();
        for (int s = 0; s < 2 * (nTeams - 1); s++) slots.add(s);

        master.buildConstraints(teams, slots);
        master.optimize();



    }


    public static List<Tour> generateInitialTours(int[][] distanceMatrix, int timeSlots, int upperbound) {
        int nTeams = distanceMatrix.length;
        List<Tour> tours = new ArrayList<>();
        Random random = new Random();

        // 1. Bouw Circle Method Planning
        List<Match>[][] matchSchedule = new ArrayList[timeSlots][];
        for (int s = 0; s < timeSlots; s++) {
            matchSchedule[s] = new ArrayList[nTeams];
            for (int i = 0; i < nTeams; i++) {
                matchSchedule[s][i] = new ArrayList<>();
            }
        }

        int round = 0;
        for (int phase = 0; phase < 2; phase++) {
            for (int i = 0; i < nTeams - 1; i++) {
                for (int j = 0; j < nTeams / 2; j++) {
                    int home = (i + j) % (nTeams - 1);
                    int away = (nTeams - 1 - j + i) % (nTeams - 1);

                    if (j == 0) away = nTeams - 1;

                    int slot = round;
                    if (phase == 1) {
                        int tmp = home;
                        home = away;
                        away = tmp;
                        slot = round + (nTeams - 1);
                    }

                    matchSchedule[slot][home].add(new Match(home, away, slot));
                    matchSchedule[slot][away].add(new Match(home, away, slot));
                }
                round++;
            }
            round = 0;
        }

        // 2. Voor elk team: bouw 5 geldige tours
        for (int t = 0; t < nTeams; t++) {
            int toursGenerated = 0;
            int attempts = 0;

            while (toursGenerated < 5 && attempts < 1000) { // Stop als 1000 pogingen mislukt
                attempts++;

                List<Match> matches = new ArrayList<>();
                for (int s = 0; s < timeSlots; s++) {
                    matches.addAll(matchSchedule[s][t]);
                }
                matches.sort(Comparator.comparingInt(m -> m.timeSlot));

                // Kleine random swap tussen wedstrijden om variatie te creëren
                if (matches.size() >= 2) {
                    int idx1 = random.nextInt(matches.size());
                    int idx2 = random.nextInt(matches.size());
                    if (idx1 != idx2) {
                        Collections.swap(matches, idx1, idx2);
                    }
                }

                // Bereken de kost van de tour
                double cost = 0.0;
                int current = t;
                for (Match match : matches) {
                    int next = (match.homeTeam == t) ? match.awayTeam : match.homeTeam;
                    cost += distanceMatrix[current][next];
                    current = next;
                }
                cost += distanceMatrix[current][t];

                Tour tour = new Tour(t, cost, matches);

                // Controleer of de tour geldig is
                if (validateTour(tour, distanceMatrix, upperbound)) {
                    tours.add(tour);
                    toursGenerated++;

                    System.out.println(">> Geldige tour " + toursGenerated + " voor team " + t);

                    // Print tour details
                    for (Match match : matches) {
                        if (match.homeTeam == t) {
                            System.out.println("  THUIS tegen team " + match.awayTeam + " in tijdslot " + match.timeSlot);
                        } else {
                            System.out.println("  UIT tegen team " + match.homeTeam + " in tijdslot " + match.timeSlot);
                        }
                    }
                }
            }

            if (toursGenerated == 0) {
                System.out.println(">> Waarschuwing: Geen enkele geldige tour voor team " + t);
            }
        }

        return tours;
    }

    public static boolean validateTour(Tour tour, int[][] distanceMatrix, int upperbound) {
        Set<Integer> opponentsHome = new HashSet<>();
        Set<Integer> opponentsAway = new HashSet<>();
        Map<Integer, Integer> slotCount = new HashMap<>();

        List<Match> matches = tour.matches.stream()
                .sorted(Comparator.comparingInt(m -> m.timeSlot))
                .toList();

        int previousOpponent = -1;
        int previousTimeSlot = -2; // dummy init
        int repeatCount = 0;
        int consecutiveHome = 0;
        int consecutiveAway = 0;

        for (int i = 0; i < matches.size(); i++) {
            Match m = matches.get(i);
            int slot = m.timeSlot;

            // Regel 2: Niet meer dan 1 match per tijdslot
            if (slotCount.containsKey(slot)) {
                System.out.println("Fout: Meerdere wedstrijden in slot " + slot + " voor team " + tour.team);
                return false;
            }
            slotCount.put(slot, 1);

            int opponent = (m.homeTeam == tour.team) ? m.awayTeam : m.homeTeam;

            // Regel 1: dubbele wedstrijden (heen/terug)
            if (m.homeTeam == tour.team) {
                if (!opponentsHome.add(opponent)) {
                    System.out.println("Fout: Meerdere keren thuis tegen team " + opponent);
                    return false;
                }
            } else {
                if (!opponentsAway.add(opponent)) {
                    System.out.println("Fout: Meerdere keren uit tegen team " + opponent);
                    return false;
                }
            }

            // Regel 3: NRC (zelfde tegenstander in s en s+1)
            if (i > 0 && slot == previousTimeSlot + 1 && opponent == previousOpponent) {
                System.out.println("Fout: NRC tegen team " + opponent + " in slots " + previousTimeSlot + " en " + slot);
                return false;
            }

            previousOpponent = opponent;
            previousTimeSlot = slot;

            // Regel 4: breaks (consecutive home/away)
            boolean isHome = (m.homeTeam == tour.team);
            if (isHome) {
                consecutiveHome++;
                consecutiveAway = 0;
            } else {
                consecutiveAway++;
                consecutiveHome = 0;
            }

            if (consecutiveHome > upperbound) {
                System.out.println("Fout: Te veel opeenvolgende thuiswedstrijden voor team " + tour.team);
                return false;
            }
            if (consecutiveAway > upperbound) {
                System.out.println("Fout: Te veel opeenvolgende uitwedstrijden voor team " + tour.team);
                return false;
            }
        }

        // Final check: heeft elke tegenstander 1x thuis en 1x uit gespeeld?
        int n = distanceMatrix.length;
        if (opponentsHome.size() != n - 1 || opponentsAway.size() != n - 1) {
            System.out.println("Fout: Niet tegen alle tegenstanders gespeeld door team " + tour.team);
            return false;
        }

        return true;
    }

}
