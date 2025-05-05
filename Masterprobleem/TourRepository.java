package Masterprobleem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TourRepository {
    private Map<Integer, List<Tour>> teamTours; // integer is hier onze team

    public TourRepository(int numTeams) {
        teamTours = new HashMap<>();
        for (int t = 0; t < numTeams; t++) {
            teamTours.put(t, new ArrayList<>());
        }
    }

    public void addTour(int team, Tour tour) {
        teamTours.get(team).add(tour);
    }

    public List<Tour> getTours(int team) {
        return teamTours.get(team);
    }

    public Map<Integer, List<Tour>> getAllTours() {
        return teamTours;
    }
}
