package org.dice_research.cel.tree.select;

import org.dice_research.cel.refine.instances.ScoredClassExpressionWithInstances;
import org.dice_research.cel.score.LengthBasedRefinementScorer;

import javolution.util.FastBitSet;

public class ClassExpressionLengthComparator
        implements FeatureValueComparator<ScoredClassExpressionWithInstances, Integer> {

    @Override
    public Integer extractValue(ScoredClassExpressionWithInstances feature, FastBitSet positives, FastBitSet negatives,
            int posIntersection, int negIntersection) {
        return LengthBasedRefinementScorer
                .getLength(((ScoredClassExpressionWithInstances) feature).getClassExpression());
    }

    @Override
    public Integer getWorstValue() {
        return Integer.valueOf(Integer.MAX_VALUE);
    }

}
