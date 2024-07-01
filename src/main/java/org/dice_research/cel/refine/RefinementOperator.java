package org.dice_research.cel.refine;

import java.util.Set;

import org.dice_research.cel.expression.ScoredClassExpression;

public interface RefinementOperator {

    Set<ScoredClassExpression> refine(ScoredClassExpression nextBestExpression);
}
