package org.dice_research.cel;

import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

import org.dice_research.cel.expression.ScoredClassExpression;
import org.dice_research.cel.io.IntermediateResultPrinter;

public interface ConceptLearner {

    public default List<ScoredClassExpression> findClassExpression(Collection<String> positive,
            Collection<String> negative) {
        return findClassExpression(positive, negative, null, null);
    }

    public List<ScoredClassExpression> findClassExpression(Collection<String> positive, Collection<String> negative,
            OutputStream logStream, IntermediateResultPrinter iResultPrinter);
}
