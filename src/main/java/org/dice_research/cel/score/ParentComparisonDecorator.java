package org.dice_research.cel.score;

import org.dice_research.cel.expression.ClassExpression;

public class ParentComparisonDecorator extends AbstractScoreCalculatorDecorator {

    public ParentComparisonDecorator(ScoreCalculator decorated) {
        super(decorated);
    }

    @Override
    public double calculateRefinementScore(int posCount, int negCount, double classificationScore, ClassExpression ce) {
        
        return super.calculateRefinementScore(posCount, negCount, classificationScore, ce);
    }
}
