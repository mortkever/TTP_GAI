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

    public int addTour(int team, Tour tour) {
        for (Tour existingtTour : teamTours.get(team)) {
            if (existingtTour.cost == tour.cost) {
                Boolean allSame = true;
                for (int i = 0; i < tour.arcs.size(); i++) {
                    if ((tour.arcs.get(i).from != existingtTour.arcs.get(i).from ||
                            tour.arcs.get(i).to != existingtTour.arcs.get(i).to) &&
                            tour.arcs.get(i).time == existingtTour.arcs.get(i).time) {
                        allSame = false;
                    }
                }
                if (allSame) {

                    System.err.println("Tour already exists");
                    System.err.println(tour);
                    System.out.println(existingtTour);

                    return 1;
                }
            }
        }
        teamTours.get(team).add(tour);
        return 0;
    }

    public List<Tour> getTours(int team) {
        return teamTours.get(team);
    }

    public Map<Integer, List<Tour>> getAllTours() {
        return teamTours;
    }
}
