package org.dice_research.cel.io;

import org.dice_research.cel.expression.ScoredClassExpression;
import org.dice_research.topicmodeling.commons.collections.TopDoubleObjectCollection;

public class NullIntermediateResultPrinter implements IntermediateResultPrinter {

    @Override
    public void printIntermediateResults(TopDoubleObjectCollection<ScoredClassExpression> topExpressions) {
    }

    @Override
    public void setStartTime(long startTime) {
    }

    @Override
    public void recursionStarts() {
    }

    @Override
    public void recursionEnds() {
    }

}
