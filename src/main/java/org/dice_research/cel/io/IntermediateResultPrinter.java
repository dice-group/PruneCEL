package org.dice_research.cel.io;

import org.dice_research.cel.expression.ScoredClassExpression;
import org.dice_research.topicmodeling.commons.collections.TopDoubleObjectCollection;

public interface IntermediateResultPrinter {

    void printIntermediateResults(TopDoubleObjectCollection<ScoredClassExpression> topExpressions);
}
