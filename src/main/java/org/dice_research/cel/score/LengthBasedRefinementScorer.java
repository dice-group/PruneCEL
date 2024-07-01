package org.dice_research.cel.score;

import org.dice_research.cel.expression.ClassExpression;
import org.dice_research.cel.expression.ClassExpressionVisitor;
import org.dice_research.cel.expression.Junction;
import org.dice_research.cel.expression.NamedClass;
import org.dice_research.cel.expression.SimpleQuantifiedRole;

public class LengthBasedRefinementScorer extends AbstractScoreCalculatorDecorator {

    protected double propertyPenalty = 0.01;
    protected double junctionPenalty = 0.005;

    public LengthBasedRefinementScorer(ScoreCalculator decorated) {
        super(decorated);
    }

    public LengthBasedRefinementScorer(ScoreCalculator decorated, double propertyPenalty, double junctionPenalty) {
        super(decorated);
        this.propertyPenalty = propertyPenalty;
        this.junctionPenalty = junctionPenalty;
    }

    @Override
    public double calculateRefinementScore(int posCount, int negCount, double classificationScore, ClassExpression ce) {
        LengthDetectingVisitor visitor = new LengthDetectingVisitor();
        ce.accept(visitor);
        return classificationScore
                - ((visitor.propertyCount * propertyPenalty) + (visitor.junctionMemberCount * junctionPenalty));
    }

    public static class Factory implements ScoreCalculatorFactory {
        protected ScoreCalculatorFactory decoratedFactory;

        public Factory(ScoreCalculatorFactory decoratedFactory) {
            super();
            this.decoratedFactory = decoratedFactory;
        }

        @Override
        public ScoreCalculator create(int numOfPositives, int numOfNegatives) {
            return new LengthBasedRefinementScorer(decoratedFactory.create(numOfPositives, numOfNegatives));
        }
    }

    public static class LengthDetectingVisitor implements ClassExpressionVisitor {

        protected int junctionCount = 0;
        protected int junctionMemberCount = 0;
        protected int propertyCount = 0;

        public LengthDetectingVisitor() {
            super();
        }

        @Override
        public void visitNamedClass(NamedClass node) {
        }

        @Override
        public void visitJunction(Junction node) {
            this.junctionCount++;
            this.junctionMemberCount += node.getChildren().size();
            for (ClassExpression child : node.getChildren()) {
                child.accept(this);
            }
        }

        @Override
        public void visitSimpleQuantificationRole(SimpleQuantifiedRole node) {
            this.propertyCount++;
            node.getTailExpression().accept(this);
        }

        /**
         * @return the junctionCount
         */
        public int getJunctionCount() {
            return junctionCount;
        }

        /**
         * @param junctionCount the junctionCount to set
         */
        public void setJunctionCount(int junctionCount) {
            this.junctionCount = junctionCount;
        }

        /**
         * @return the junctionMemberCount
         */
        public int getJunctionMemberCount() {
            return junctionMemberCount;
        }

        /**
         * @param junctionMemberCount the junctionMemberCount to set
         */
        public void setJunctionMemberCount(int junctionMemberCount) {
            this.junctionMemberCount = junctionMemberCount;
        }

        /**
         * @return the propertyCount
         */
        public int getPropertyCount() {
            return propertyCount;
        }

        /**
         * @param propertyCount the propertyCount to set
         */
        public void setPropertyCount(int propertyCount) {
            this.propertyCount = propertyCount;
        }

    }

}
