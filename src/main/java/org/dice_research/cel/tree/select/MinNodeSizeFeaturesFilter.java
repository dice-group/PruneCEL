package org.dice_research.cel.tree.select;

import org.dice_research.cel.data.LearningProblemSatistics;
import org.dice_research.cel.generic.CountBasedSubSetChecker;
import org.dice_research.cel.tree.Feature;

public class MinNodeSizeFeaturesFilter implements FeatureSelectionFilter<Feature> {

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
    public MinNodeSizeFeaturesFilter(int minAbsCardCleanNode, double minRelCardCleanNode) {
        super();
        this.checker = new CountBasedSubSetChecker(minAbsCardCleanNode, minRelCardCleanNode);
    }

    @Override
    public boolean check(Feature feature, int posCount, int negCount, int posIntersection, int negIntersection,
            LearningProblemSatistics lpStats) {
        // If an intersection is not empty, it should fulfill the minimum size
        // requirements
        return ((posIntersection == 0) || (checker.isSetGood(posIntersection, lpStats.numberOfPositives)))
                && ((negIntersection == 0) || (checker.isSetGood(negIntersection, lpStats.numberOfNegatives)));
    }

}
