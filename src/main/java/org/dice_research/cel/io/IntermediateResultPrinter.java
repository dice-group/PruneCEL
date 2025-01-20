package org.dice_research.cel.io;

import org.dice_research.cel.expression.ScoredClassExpression;
import org.dice_research.topicmodeling.commons.collections.TopDoubleObjectCollection;

public interface IntermediateResultPrinter {
    
    void setStartTime(long startTime);

    void printIntermediateResults(TopDoubleObjectCollection<ScoredClassExpression> topExpressions);

    void recursionStarts();

    void recursionEnds();
}
