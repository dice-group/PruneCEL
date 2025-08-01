package org.dice_research.cel.refine.instances;

import org.dice_research.cel.expression.ScoredClassExpression;
import org.dice_research.cel.tree.Feature;

import javolution.util.FastBitSet;

public class ScoredClassExpressionWithInstances extends ScoredClassExpression implements Feature {

    protected FastBitSet selectedPos;
    protected FastBitSet selectedNeg;

    public ScoredClassExpressionWithInstances(ScoredClassExpression scel, FastBitSet selectedPos, FastBitSet selectedNeg) {
        super(scel.getClassExpression(), scel.getClassificationScore(), scel.getRefinementScore(),
                scel.getPosCount(), scel.getNegCount(), scel.isAddedEdge());
        this.selectedPos = selectedPos;
        this.selectedNeg = selectedNeg;
    }

    @Override
    public FastBitSet getSelectedPositives() {
        return selectedPos;
    }

    @Override
    public FastBitSet getSelectedNegatives() {
        return selectedNeg;
    }
}