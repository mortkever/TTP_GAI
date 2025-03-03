import java.util.*;



public class Schedule {
    private Map<Integer, List<Match>> schedule;

    public Schedule(Map<Integer, List<Match>> shedule) {
        this.schedule = shedule;
    }

    public Schedule() {
        this.schedule = new HashMap<>();
    }

    public void addMatches(int round, List<Match> matches) {
        schedule.put(round, matches);
    }

    public List<Match> getMatches(int round) {
        return schedule.getOrDefault(round, new ArrayList<>());
    }

    public void printSchedule() {
        for (Map.Entry<Integer, List<Match>> entry : schedule.entrySet()) {
            System.out.println("Ronde " + entry.getKey() + ":");
            for (Match match : entry.getValue()) {
                System.out.println("  " + match);
            }
            System.out.println();
        }
    }

    public void addFeasibleSchedule() {
        // Create a feasible schedule
        List<Team> teams = Arrays.asList(
                new Team("Team 1"), new Team("Team 2"), new Team("Team 3"),
                new Team("Team 4")
        );

        this.addMatches(1, Arrays.asList(
                new Match(teams.get(0), teams.get(2)),
                new Match(teams.get(1), teams.get(3))
        ));

        this.addMatches(2, Arrays.asList(
                new Match(teams.get(0), teams.get(1)),
                new Match(teams.get(2), teams.get(3))
        ));

        this.addMatches(3, Arrays.asList(
                new Match(teams.get(0), teams.get(3)),
                new Match(teams.get(2), teams.get(1))
        ));

        this.addMatches(4, Arrays.asList(
                new Match(teams.get(2), teams.get(0)),
                new Match(teams.get(3), teams.get(1))
        ));

        this.addMatches(5, Arrays.asList(
                new Match(teams.get(1), teams.get(0)),
                new Match(teams.get(3), teams.get(2))
        ));

        this.addMatches(6, Arrays.asList(
                new Match(teams.get(1), teams.get(2)),
                new Match(teams.get(3), teams.get(0))
        ));
    }

    public Map<Integer, List<Match>> getSchedule() {
        return schedule;
    }
}
