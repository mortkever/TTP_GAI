package Masterprobleem;

import java.util.ArrayList;
import java.util.List;

public class Tour {
    public List<Arc> arcs;
    public double cost;

    public Tour(List<Arc> arcs, double cost) {
        this.arcs = arcs;
        this.cost = cost;
    }

    @Override
    public String toString() {
        return "Tour{" +
                "arcs=" + arcs +
                ", cost=" + cost +
                '}';
    }
}