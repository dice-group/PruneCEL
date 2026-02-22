package org.dice_research.cel.tree;

import java.util.List;

import javolution.util.FastBitSet;

public class CostAwareDecisionTreeNode extends DecisionTreeNode {

    public List<CostAwareDecisionTreeNode> leaveNodes = null;
    /**
     ** If this is a leave node, r(t) = 1 - p(k(t)|t) (probability of an element ending up in this leave node not ending up in the 
     */
    public double resubstitution = 0;

    public CostAwareDecisionTreeNode(boolean isPositiveClass, FastBitSet positives, FastBitSet negatives) {
        super(isPositiveClass, positives, negatives);
    }

}
