package Masterprobleem;

public class Arc {
    public int time;
    public int from;
    public int to;

    public Arc(int time, int from, int to) {
        this.time = time;
        this.from = from;
        this.to = to;
    }

    @Override
    public String toString() {
        return "(" + time + ", " + from + " -> " + to + ")";
    }
}
