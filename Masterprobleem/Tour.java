package Masterprobleem;

import java.util.ArrayList;
import java.util.List;

public class Tour {
    public int team;
    public double cost;
    public List<Match> matches;

    public Tour(int team, double cost, List<Match> matches) {
        this.team = team;
        this.cost = cost;
        this.matches = (matches != null) ? matches : new ArrayList<>();
    }
}