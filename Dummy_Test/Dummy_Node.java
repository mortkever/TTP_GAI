package Dummy_Test;

import java.util.ArrayList;
import java.util.List;

public class Dummy_Node {
    public int venue;    // where the team is (0 = home, 1/2/3 = opponent)
    public int slot;     // time slot
    public Integer opponent; // opponent being played at this node (can be null if unknown)
    public List<Dummy_Arc> outgoingArcs;

    public Dummy_Node(int venue, int slot) {
        this.venue = venue;
        this.slot = slot;
        this.opponent = null; // initially unknown
        this.outgoingArcs = new ArrayList<>();
    }

    public void setOpponent(int opponent) {
        this.opponent = opponent;
    }

    public void addArc(Dummy_Arc arc) {
        outgoingArcs.add(arc);
    }

    @Override
    public String toString() {
        return "Node(Venue=" + venue + ", Slot=" + slot + ")";
    }
}
