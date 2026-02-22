package org.dice_research.cel.strategy;

import org.dice_research.cel.data.LearningProblemSatistics;
import org.dice_research.cel.generic.CountBasedSubSetChecker;
import org.dice_research.cel.tree.DecisionTreeNode;

/**
 * A classifier which identifies leaf nodes of a decision tree that should be
 * improved. A leaf node is dirty if one of two conditions is fulfilled:
 * <ol>
 * <li>It has positive and negative examples, or</li>
 * <li>It contains less than the minimum amount of elements.</li>
 * </ol>
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class SimpleDirtyLeafNodeClassifier implements DirtyLeafNodeClassifier {

    /**
     * Checker implementation used to ensure the set sizes of clean nodes.
     */
    protected CountBasedSubSetChecker checker;

    /**
     * Constructor.
     * 
     * @param minAbsCardCleanNode Minimum absolute cardinality a leaf node should
     *                            have to be seen as clean. This can be used to
     *                            avoid nodes with a very small number of examples.
     * @param minRelCardCleanNode Minimum cardinality relative to the size of the
     *                            learning problem. See
     *                            {@link CountBasedSubSetChecker}.
     */
    public SimpleDirtyLeafNodeClassifier(int minAbsCardCleanNode, double minRelCardCleanNode) {
        super();
        this.checker = new CountBasedSubSetChecker(minAbsCardCleanNode, minRelCardCleanNode);
    }

    @Override
    public boolean isDirty(DecisionTreeNode node, LearningProblemSatistics lpStats) {
        int posCount = node.getPositives().cardinality();
        int negCount = node.getNegatives().cardinality();
        int min;
        int max;
        int completeSetSize;
        if (posCount > negCount) {
            min = negCount;
            max = posCount;
            completeSetSize = lpStats.numberOfPositives;
        } else {
            min = posCount;
            max = negCount;
            completeSetSize = lpStats.numberOfNegatives;
        }
        // If there are positives and negatives (i.e., the min count is larger than 0)
        // OR the max count is below the min cardinality threshold, this node is not
        // clean!
        return ((min != 0) || (!checker.isSetGood(completeSetSize, max)));
    }

}
