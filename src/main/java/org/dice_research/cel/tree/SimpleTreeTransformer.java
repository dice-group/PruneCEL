package org.dice_research.cel.tree;

import org.dice_research.cel.expression.ClassExpression;
import org.dice_research.cel.expression.Junction;
import org.dice_research.cel.expression.NamedClass;
import org.dice_research.cel.expression.NegatingVisitor;
import org.dice_research.cel.expression.ScoredClassExpression;
import org.dice_research.cel.refine.instances.ScoredClassExpressionWithInstances;
import org.dice_research.cel.score.ScoreCalculator;

public class SimpleTreeTransformer {

    protected NegatingVisitor negator = new NegatingVisitor();

    public ScoredClassExpression transformTree(DecisionTreeNode root, ScoreCalculator calculator) {
        TransformationResult result = transformNode(root);
        return calculator.score(result.ce, result.tp, result.tn, false);
    }

    private TransformationResult transformNode(DecisionTreeNode node) {
        TransformationResult result = null;
        if (node.isLeaf()) {
            int posCount = node.positives.cardinality();
            int negCount = node.negatives.cardinality();
            result = new TransformationResult(null, posCount, 0, negCount, 0);
            if (!node.isPositiveClass()) {
                result.invert(negator);
            }
        } else {
            TransformationResult trueChildResult = transformNode(node.getTrueChild());
            TransformationResult falseChildResult = transformNode(node.getFalseChild());
            ClassExpression nodeExpression = ((ScoredClassExpressionWithInstances) node.getFeature())
                    .getClassExpression();
            result = new TransformationResult(null, 0, 0, 0, 0);
            if ((trueChildResult.ce != null)) {
                result.ce = new Junction(true, nodeExpression, trueChildResult.ce);
                result.addCounts(trueChildResult);
            } else if (node.getTrueChild().isPositiveClass()) {
                result.ce = nodeExpression;
                result.addCounts(trueChildResult);
            } else {
                // No need to add the expression...
                result.addCounts(trueChildResult, true);
            }
            if (falseChildResult.ce != null) {
                result.ce = result.ce != null ? new Junction(false, result.ce, falseChildResult.ce)
                        : new Junction(true, negator.negateExpression(nodeExpression), falseChildResult.ce);
                result.addCounts(falseChildResult);
            } else if (node.getFalseChild().isPositiveClass()) {
                result.ce = new Junction(false, result.ce, negator.negateExpression(nodeExpression));
                result.addCounts(falseChildResult, true);
            } else {
                // No need to add the expression...
                result.addCounts(falseChildResult);
            }
            // special case: both children are false...
            if (result.ce == null) {
                result.ce = NamedClass.BOTTOM;
            }
        }
        return result;
    }

    protected static class TransformationResult {

        ClassExpression ce;
        int tp = 0;
        int tn = 0;
        int fp = 0;
        int fn = 0;

        public TransformationResult(ClassExpression ce, int tp, int tn, int fp, int fn) {
            super();
            this.ce = ce;
            this.tp = tp;
            this.tn = tn;
            this.fp = fp;
            this.fn = fn;
        }

        public void invert(NegatingVisitor negator) {
            // We invert the meaning of this intermediate result
            if (ce != null) {
                ce = negator.negateExpression(ce);
            }
            int temp;
            temp = tp;
            tp = fn;
            fn = temp;
            temp = tn;
            tn = fp;
            fp = temp;
        }

        public int getGroundTruthPosCount() {
            return tp + fn;
        }

        public int getGroundTruthNegCount() {
            return tn + fp;
        }

        public void addCounts(TransformationResult other) {
            addCounts(other, false);
        }

        public void addCounts(TransformationResult other, boolean inverted) {
            if (inverted) {
                this.tp += other.fn;
                this.tn += other.fp;
                this.fp += other.tn;
                this.fn += other.tp;
            } else {
                this.tp += other.tp;
                this.tn += other.tn;
                this.fp += other.fp;
                this.fn += other.fn;
            }
        }
    }
}
