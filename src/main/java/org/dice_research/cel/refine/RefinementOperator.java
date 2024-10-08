package org.dice_research.cel.refine;

import java.util.Set;

import org.dice_research.cel.expression.ScoredClassExpression;

/**
 * Basic interface of a refinement operator.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public interface RefinementOperator {

    /**
     * Given a class expression, the refinement operator creates a set of new class
     * expressions that are refinements of the given expression.
     * 
     * @param nextBestExpression the class expression that should be refined further
     * @return a set of new class expressions that are refinements of the given
     *         expression.
     */
    Set<ScoredClassExpression> refine(ScoredClassExpression nextBestExpression);
}
