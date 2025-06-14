package Masterprobleem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Tour {
    private List<Arc> arcs;
    private double realCost;

    public Tour(List<Arc> arcs, double realCost) {
        this.arcs = arcs;
        this.realCost = realCost;
    }

    public Tour(List<Arc> arcs, int[][] distanceMatrix) {
        this.arcs = arcs;
        this.realCost = calculateCost(arcs, distanceMatrix);
    }

    private double calculateCost(List<Arc> arcs, int[][] distanceMatrix) {
        double totalCost = 0.0;
        for (Arc arc : arcs) {
            totalCost += distanceMatrix[arc.from][arc.to];
        }
        return totalCost;
    }

    public double getRealCost() {
        return realCost;
    }

    public void setRealCost(Double realCost) {
        this.realCost = realCost;
    }

    public List<Arc> getArcs() {
        return arcs;
    }

    @Override
    public String toString() {
        return "Tour{" +
                "arcs=" + arcs +
                ", cost=" + realCost +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Tour tour = (Tour) o;
        return this.getArcs().equals(tour.getArcs()); // && this.getCost() == tour.getCost(); // assumes arcs are in the
                                                      // same order
    }

    @Override
    public int hashCode() {
        return Objects.hash(arcs);
    }
}