package BranchPrice;

public class BranchingDecision {
    public final int team;
    public final int from;
    public final int to;
    public final int slot;
    public final boolean mustUseArc;

    public BranchingDecision(int team, int from, int to, int slot, boolean mustUseArc) {
        this.team = team;
        this.from = from;
        this.to = to;
        this.slot = slot;
        this.mustUseArc = mustUseArc;
    }

    public int getValue() {
        return mustUseArc ? 1 : 0;
    }


    public Arc getArc() {
        return new Arc(slot, from, to);
    }

    @Override
    public String toString() {
        return "BranchingDecision{" +
                "team=" + team +
                ", from=" + from +
                ", to=" + to +
                ", slot=" + slot +
                ", mustUseArc=" + mustUseArc +
                '}';
    }
}

