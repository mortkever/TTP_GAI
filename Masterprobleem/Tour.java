package Masterprobleem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Tour {
    public List<Arc> arcs;
    public double cost;

    public Tour(List<Arc> arcs, double cost) {
        this.arcs = arcs;
        this.cost = cost;
    }

    public Tour(List<Arc> arcs, int[][] distanceMatrix) {
        this.arcs = arcs;
        this.cost = calculateCost(arcs, distanceMatrix);
    }

    private double calculateCost(List<Arc> arcs, int[][] distanceMatrix) {
        double totalCost = 0.0;
        for (Arc arc : arcs) {
            totalCost += distanceMatrix[arc.from][arc.to];
        }
        return totalCost;
    }

    @Override
    public String toString() {
        return "Tour{" +
                "arcs=" + arcs +
                ", cost=" + cost +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tour tour = (Tour) o;
        return arcs.equals(tour.arcs) && cost == tour.cost; // assumes arcs are in the same order
    }

    @Override
    public int hashCode() {
        return Objects.hash(arcs);
    }
}