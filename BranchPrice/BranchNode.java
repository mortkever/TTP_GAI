package BranchPrice;


import java.util.List;

public class BranchNode {
    public List<BranchingDecision> fixations;
    public BranchNode parent;
    public int remainingChildren;

    public BranchNode(List<BranchingDecision> fixations, BranchNode parent) {
        this.fixations = fixations;
        this.parent = parent;
        this.remainingChildren = 2; // Bij splitsen ontstaan altijd 2 kinderen
    }

    public boolean isFullyExplored() {
        return remainingChildren == 0;
    }

    public void markChildExplored() {
        if (remainingChildren > 0) {
            remainingChildren--;
        }
    }
}

