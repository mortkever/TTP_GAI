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

    // Check of de tour al bestaat op het moment dat deze wordt aangemaakt! NIET bij
    // het toevoegen
    public int addTour(int team, Tour tour) {
        teamTours.get(team).add(tour);
        return 0;
    }

    public List<Tour> getTours(int team) {
        return teamTours.get(team);
    }

    public Map<Integer, List<Tour>> getAllTours() {
        return teamTours;
    }

    public void pruneTours(int numberToKeep) {
        for (List<Tour> tours : teamTours.values()) {
            tours.sort(null);
            System.out.println("-------------------");
            System.out.println(tours.getFirst().getModCost());

            while (tours.size() > numberToKeep) {
                tours.removeFirst();
            }
            System.out.println(tours.getLast().getModCost());

            System.out.println(tours.size());
        }
    }
}
