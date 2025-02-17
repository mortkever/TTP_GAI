import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


}
