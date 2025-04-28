package Dummy_Test;

public class Dummy_Arc {
    public Dummy_Node from;
    public Dummy_Node to;
    public double cost; // adjusted travel cost (including dual bonuses)

    public Dummy_Arc(Dummy_Node from, Dummy_Node to, double cost) {
        this.from = from;
        this.to = to;
        this.cost = cost;
    }
}
