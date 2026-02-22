package org.dice_research.cel.tree.select;

import org.dice_research.cel.data.LearningProblemSatistics;
import org.dice_research.cel.tree.Feature;

public interface FeatureSelectionFilter<T extends Feature> {

    /**
     * Checks the given feature together with the given positive and negative
     * element count of the selection and the positive and negative intersection
     * counts. It returns <code>true</code> if the feature can be used for the
     * selection process or <code>false</code> if it should not be used.
     * 
     * @param feature
     * @param posCount
     * @param negCount
     * @param posIntersection
     * @param negIntersection
     * @return <code>true</code> if the feature can be used for the selection
     *         process or <code>false</code> if it should not be used
     */
    boolean check(T feature, int posCount, int negCount, int posIntersection, int negIntersection,
            LearningProblemSatistics lpStats);
}
