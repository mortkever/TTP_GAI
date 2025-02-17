import java.util.*;

public class ScheduleValidator {
    private final Schedule schedule;

    public ScheduleValidator(Schedule schedule) {
        this.schedule = schedule;
    }

    /**
     * Voert alle validaties uit en toont de resultaten.
     */
    public void validate() {
        boolean uniqueMatches = validateUniqueMatchesPerRound();
        boolean noSelfMatches = validateNoSelfMatches();
        boolean noDuplicateMatches = validateNoDuplicateMatches();
        boolean noLongHomeAwayStreaks = validateHomeAwayStreaks();
        boolean completeRoundRobin = validateRoundRobin();

        if (uniqueMatches && noSelfMatches && noDuplicateMatches && noLongHomeAwayStreaks && completeRoundRobin) {
            System.out.println("✅ Het schema is volledig geldig.");
        } else {
            System.out.println("❌ Er zijn één of meerdere validatiefouten gevonden.");
        }
    }

    /**
     * 1. Controleert of een team maar één keer per ronde speelt.
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
     * 2. Controleert of er geen wedstrijden zijn waar een team tegen zichzelf speelt.
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
     * 3. Controleert of er geen dubbele wedstrijden zijn tussen dezelfde teams.
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
     * 4. Controleert of een team niet meer dan 3 keer achter elkaar thuis of uit speelt.
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
     * 5. Controleert of alle teams exact 2 keer tegen elkaar spelen (1 keer thuis, 1 keer uit).
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
}
