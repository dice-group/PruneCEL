package org.dice_research.cel.tree;

import org.dice_research.cel.expression.ClassExpression;
import org.dice_research.cel.expression.Junction;
import org.dice_research.cel.expression.NamedClass;
import org.dice_research.cel.expression.NegatingVisitor;
import org.dice_research.cel.expression.OntolearnCompatibleSerializer;
import org.dice_research.cel.expression.ScoredClassExpression;
import org.dice_research.cel.refine.instances.ScoredClassExpressionWithInstances;
import org.dice_research.cel.score.ScoreCalculator;

public class TreeTransformer {

    protected NegatingVisitor negator = new NegatingVisitor();

    public ScoredClassExpression transformTree(DecisionTreeNode root, ScoreCalculator calculator) {
        TransformationResult result = transformNode(root);
        return calculator.score(result.ce, result.tp, result.fp, false);
    }

    private TransformationResult transformNode(DecisionTreeNode node) {
        if (node.isLeaf()) {
            int posCount = node.positives.cardinality();
            int negCount = node.negatives.cardinality();
            if (node.isPositiveClass()) {
                return new TransformationResult(NamedClass.TOP, posCount, 0, negCount, 0);
            } else {
                return new TransformationResult(NamedClass.BOTTOM, 0, negCount, 0, posCount);
            }
        } else {
            // 2 nodes with 3 states --> 9 possible combinations
            // positive class (+) = 0
            // negative class (-) = 1
            // class expression (A, B) = 2
            // the left (true) child's code will be multiplied by 3
            TransformationResult trueChildResult = transformNode(node.getTrueChild());
            TransformationResult falseChildResult = transformNode(node.getFalseChild());
            int code = toCode(trueChildResult, falseChildResult);
            ClassExpression nodeExpression = ((ScoredClassExpressionWithInstances) node.getFeature())
                    .getClassExpression();
            switch (code) {
            case 0: { // + + -> +
                return new TransformationResult(NamedClass.TOP, trueChildResult, falseChildResult);
            }
            case 1: // + - -> A
                return new TransformationResult(nodeExpression, trueChildResult, falseChildResult);
            case 2: // + B -> A or B
                return new TransformationResult(new Junction(false, nodeExpression, falseChildResult.ce),
                        trueChildResult, falseChildResult);
            case 3: // - + -> not A (case 1 inverted)
                return new TransformationResult(negator.negateExpression(nodeExpression), trueChildResult,
                        falseChildResult);
            case 4: // - - -> -
                return new TransformationResult(NamedClass.BOTTOM, trueChildResult, falseChildResult);
            case 5: // - B -> not A and B (case 7 inverted)
                return new TransformationResult(
                        new Junction(true, negator.negateExpression(nodeExpression), falseChildResult.ce),
                        trueChildResult, falseChildResult);
            case 6: // B + -> not A or B (case 2 inverted)
                return new TransformationResult(
                        new Junction(false, negator.negateExpression(nodeExpression), trueChildResult.ce),
                        trueChildResult, falseChildResult);
            case 7: // B - -> A and B
                return new TransformationResult(new Junction(true, nodeExpression, trueChildResult.ce), trueChildResult,
                        falseChildResult);
            case 8: // B C -> (A and B) or C
                return new TransformationResult(new Junction(false,
                        new Junction(true, nodeExpression, trueChildResult.ce), falseChildResult.ce), trueChildResult,
                        falseChildResult);
            default:
                throw new IllegalArgumentException("Unexpected value: " + code);
            }
        }
    }

    protected int toCode(TransformationResult trueChildResult, TransformationResult falseChildResult) {
        return toCode(trueChildResult) * 3 + toCode(falseChildResult);
    }

    protected int toCode(TransformationResult tr) {
        if (tr.ce == NamedClass.TOP) {
            return 0;
        } else if (tr.ce == NamedClass.BOTTOM) {
            return 1;
        } else {
            return 2;
        }
    }

    protected static class TransformationResult {

        protected ClassExpression ce;
        protected int tp = 0;
        protected int tn = 0;
        protected int fp = 0;
        protected int fn = 0;

        public TransformationResult(ClassExpression ce, int tp, int tn, int fp, int fn) {
            super();
            this.ce = ce;
            this.tp = tp;
            this.tn = tn;
            this.fp = fp;
            this.fn = fn;
        }

        public TransformationResult(ClassExpression ce, TransformationResult other1, TransformationResult other2) {
            super();
            this.ce = ce;
            addCounts(other1);
            addCounts(other2);
        }

        public TransformationResult(ClassExpression ce, TransformationResult other1, boolean invert1,
                TransformationResult other2, boolean invert2) {
            super();
            this.ce = ce;
            addCounts(other1, invert1);
            addCounts(other2, invert2);
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
