package Utils;
import java.util.*;

import Things.Match;
import Things.Schedule;
import Things.Team;

public class ScheduleValidator {
    private final Schedule schedule;
    private final int[][] distanceMatrix;

    public ScheduleValidator(Schedule schedule, int[][] distanceMatrix) {
        this.schedule = schedule;
        this.distanceMatrix = distanceMatrix;
    }

    /**
     * Voert alle validaties uit en toont de resultaten.
     */
    public void validate() {
        List<Boolean> validationResults = List.of(
                validateTotalGames(),
                validateTotalRounds(),
                validateUniqueMatchesPerRound(),
                validateNoSelfMatches(),
                validateNoDuplicateMatches(),
                validateHomeAwayStreaks(),
                validateRoundRobin()
                // validateTotalDistance() // fix bug needed
        );

        if (validationResults.contains(false)) {
            System.out.println("❌ Er zijn één of meerdere validatiefouten gevonden.");
        } else {
            System.out.println("✅ Het schema is volledig geldig.");
        }
    }

    /**
     * 1. Valideert of het totale aantal gespeelde wedstrijden correct is.
     */
    private boolean validateTotalGames() {
        int totalGames = schedule.getSchedule().values().stream()
                .mapToInt(List::size).sum();

        int n = getTeamCount();
        int expectedGames = n * (n - 1);

        if (totalGames != expectedGames) {
            System.out.println("⚠️ Ongeldig aantal wedstrijden: Gevonden " + totalGames + ", Verwacht " + expectedGames);
            return false;
        }
        return true;
    }

    /**
     * 2. Valideert of het totale aantal rondes correct is.
     */
    private boolean validateTotalRounds() {
        int totalRounds = schedule.getSchedule().size();
        int n = getTeamCount();
        int expectedRounds = 2 * (n - 1);

        if (totalRounds != expectedRounds) {
            System.out.println("⚠️ Ongeldig aantal rondes: Gevonden " + totalRounds + ", Verwacht " + expectedRounds);
            return false;
        }
        return true;
    }

    /**
     * 3. Controleert of een team maar één keer per ronde speelt.
     */
    private boolean validateUniqueMatchesPerRound() {
        boolean isValid = true;
        for (Map.Entry<Integer, List<Match>> entry : schedule.getSchedule().entrySet()) {
            int round = entry.getKey();
            List<Match> matches = entry.getValue();
            Set<Team> teamsInRound = new HashSet<>();

            for (Match match : matches) {
                if (!teamsInRound.add(match.getTeamHome()) || !teamsInRound.add(match.getTeamAway())) {
                    System.out.println("⚠️ Conflict in ronde " + round +
                            ": Een team speelt meerdere wedstrijden in dezelfde ronde.");
                    isValid = false;
                }
            }
        }
        return isValid;
    }

    /**
     * 4. Controleert of er geen wedstrijden zijn waar een team tegen zichzelf speelt.
     */
    private boolean validateNoSelfMatches() {
        boolean isValid = true;
        for (Map.Entry<Integer, List<Match>> entry : schedule.getSchedule().entrySet()) {
            int round = entry.getKey();
            for (Match match : entry.getValue()) {
                if (match.getTeamHome().equals(match.getTeamAway())) {
                    System.out.println("⚠️ Ongeldige wedstrijd in ronde " + round +
                            ": Team speelt tegen zichzelf (" + match.getTeamHome().getName() + ").");
                    isValid = false;
                }
            }
        }
        return isValid;
    }

    /**
     * 5. Controleert of er geen dubbele wedstrijden zijn tussen dezelfde teams.
     */
    private boolean validateNoDuplicateMatches() {
        boolean isValid = true;
        Set<String> matchSet = new HashSet<>();

        for (Map.Entry<Integer, List<Match>> entry : schedule.getSchedule().entrySet()) {
            int round = entry.getKey();
            for (Match match : entry.getValue()) {
                String matchKey = match.getTeamHome().getName() + "-" + match.getTeamAway().getName();

                if (matchSet.contains(matchKey)) {
                    System.out.println("⚠️ Dubbele wedstrijd gevonden in ronde " + round +
                            ": " + match.getTeamHome().getName() + " vs " + match.getTeamAway().getName());
                    isValid = false;
                } else {
                    matchSet.add(matchKey);
                }
            }
        }
        return isValid;
    }

    /**
     * 6. Controleert of een team niet meer dan 3 keer achter elkaar thuis of uit speelt.
     */
    private boolean validateHomeAwayStreaks() {
        boolean isValid = true;
        Map<Team, List<String>> teamStreaks = new HashMap<>();

        for (Map.Entry<Integer, List<Match>> entry : schedule.getSchedule().entrySet()) {
            int round = entry.getKey();
            for (Match match : entry.getValue()) {
                teamStreaks.putIfAbsent(match.getTeamHome(), new ArrayList<>());
                teamStreaks.putIfAbsent(match.getTeamAway(), new ArrayList<>());

                teamStreaks.get(match.getTeamHome()).add("H");
                teamStreaks.get(match.getTeamAway()).add("A");
            }
        }

        // Controleer of er streaks van 4 of meer zijn
        for (Map.Entry<Team, List<String>> entry : teamStreaks.entrySet()) {
            Team team = entry.getKey();
            List<String> streaks = entry.getValue();
            int consecutive = 1;
            for (int i = 1; i < streaks.size(); i++) {
                if (streaks.get(i).equals(streaks.get(i - 1))) {
                    consecutive++;
                    if (consecutive > 3) {
                        System.out.println("⚠️ Te lange thuis/uit reeks voor team " +
                                team.getName() + ": meer dan 3 keer achter elkaar '" +
                                streaks.get(i) + "'");
                        isValid = false;
                        break;
                    }
                } else {
                    consecutive = 1;
                }
            }
        }
        return isValid;
    }

    /**
     * 7. Controleert of alle teams exact 2 keer tegen elkaar spelen (1 keer thuis, 1 keer uit).
     */
    private boolean validateRoundRobin() {
        boolean isValid = true;
        Map<String, Integer> matchCounts = new HashMap<>();

        for (List<Match> matches : schedule.getSchedule().values()) {
            for (Match match : matches) {
                String matchKey = match.getTeamHome().getName() + "-" + match.getTeamAway().getName();
                matchCounts.put(matchKey, matchCounts.getOrDefault(matchKey, 0) + 1);
            }
        }

        // Controleer dat er precies 2 wedstrijden zijn tussen elke combinatie (1x thuis, 1x uit)
        Map<String, Integer> pairedMatches = new HashMap<>();
        for (String matchKey : matchCounts.keySet()) {
            String[] teams = matchKey.split("-");
            String reverseKey = teams[1] + "-" + teams[0];

            int count = matchCounts.get(matchKey) + matchCounts.getOrDefault(reverseKey, 0);
            pairedMatches.put(teams[0] + "-" + teams[1], count);
        }

        for (Map.Entry<String, Integer> entry : pairedMatches.entrySet()) {
            if (entry.getValue() != 2) {
                System.out.println("⚠️ Onvolledige home/away verdeling tussen " +
                        entry.getKey() + ": " + entry.getValue() + " wedstrijden gespeeld, verwacht 2.");
                isValid = false;
            }
        }
        return isValid;
    }

    /**
     * 8. Controleert de totaal afgelegde afstand van alle teams in het schema.
     */
    private boolean validateTotalDistance() {
        int totalDistance = calculateTotalTravelDistance();
        boolean isValid = schedule.getObjectiveValue() == totalDistance;
        if(!isValid) {
            System.out.println("⚠️ Totale travel distances komen niet overeen. " + schedule.getObjectiveValue() + " ≠ " + totalDistance);
        }

        return isValid;
    }

    /**
     * 9. Controleert of match niet gevolgd wordt door de
     */


    /**
     * Distance: Calculate the total travel distance for all teams in the schedule.
     */
    public int calculateTotalTravelDistance() {
        Map<Team, Integer> travelDistances = new HashMap<>();
        Map<Team, Integer> lastLocations = new HashMap<>(); // Store last known location

        // Get all unique teams and set initial location
        Set<Team> allTeams = schedule.getTeams();
        for (Team team : allTeams) {
            travelDistances.put(team, 0);
            lastLocations.put(team, team.getID()); // Start at home location
            System.out.println("Last locations size: " + lastLocations.size() + " for team: " + team.getName());
        }

        // Iterate through rounds in order
        for(List<Match> round : schedule.getSchedule().values()) {
            //System.out.println("\n\n\n\n\n\n\n\n\nNumber of rounds: " + schedule.getSchedule().size());
            for (Match match : round) {
                System.out.println("Processing match: " + match);
                // Get team information
                Team homeTeam = match.getTeamHome();
                Team awayTeam = match.getTeamAway();
                int homeLocation = homeTeam.getID();
                int homeLastLocation = lastLocations.get(homeTeam);
                int awayLastLocation = lastLocations.get(awayTeam);

                // Calculate the traveled distances for the 2 teams
                int travelDistanceHomeTeam = distanceMatrix[homeLastLocation][homeLocation];
                int travelDistanceAwayTeam = distanceMatrix[awayLastLocation][homeLocation];

                // Update the travelDistances Map with the new distances
                travelDistances.merge(homeTeam, travelDistanceHomeTeam, Integer::sum); // Add the travelDistance to the previous distance
                travelDistances.merge(awayTeam, travelDistanceAwayTeam, Integer::sum);

                // Update the lastLocation Map with the new locations
                lastLocations.put(homeTeam, homeLocation);
                lastLocations.put(awayTeam, homeLocation);
            }
        }

        // Add return-to-home distance for all teams
        for (Team team : allTeams) {
            int lastLocation = lastLocations.get(team);
            int homeLocation = team.getID();
            if (lastLocation != homeLocation) {
                int returnDistance = distanceMatrix[lastLocation][homeLocation];
                travelDistances.merge(team, returnDistance, Integer::sum);
            }
        }

        // Calculate total travel distance
        int totalDistance = 0;
        for(Team team: allTeams) totalDistance += travelDistances.get(team);

        return totalDistance;
    }

    /**
     *  Hulpfuncties:
     */
    // Berekent het aantal teams op basis van de eerste ronde.
    private int getTeamCount() {
        if (schedule.getSchedule().isEmpty()) return 0;

        return schedule.getSchedule().values().iterator().next().size() * 2; // Each match has 2 teams
    }
}
