package BranchPrice;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Arc arc = (Arc) o;
        return time == arc.time && from == arc.from && to == arc.to;
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, from, to);
    }
}
