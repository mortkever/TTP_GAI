package BranchPrice;

import java.util.List;
import java.util.Objects;

public class Tour implements Comparable<Tour> {
    private List<Arc> arcs;
    private double cost;

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

    public double getCost() {
        return cost;
    }

    public void setCost(Double realCost) {
        cost = realCost;
    }

    public List<Arc> getArcs() {
        return arcs;
    }

    public boolean containsArc(int from, int to, int time) {
        for (Arc arc : this.arcs) {
            if (arc.time == time && arc.from == from && arc.to == to) {
                return true;
            }
        }
        return false;
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
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Tour tour = (Tour) o;
        return this.getArcs().equals(tour.getArcs()) && this.getCost() == tour.getCost(); // assumes arcs are in the
                                                                                          // same order
    }

    @Override
    public int hashCode() {
        return Objects.hash(arcs);
    }

    @Override
    public int compareTo(Tour otherTour) {
        int res = Double.compare(-1 * cost, -1 * otherTour.cost);
        // System.out.println(res + " " + cost +" " + otherTour.cost);
        return res;
    }


}