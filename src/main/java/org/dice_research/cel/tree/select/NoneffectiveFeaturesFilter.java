package org.dice_research.cel.tree.select;

import org.dice_research.cel.data.LearningProblemSatistics;
import org.dice_research.cel.tree.Feature;

/**
 * This filter provides a fast way to exclude features that do not offer any
 * change compared to the selection, i.e., they either do not select anything or
 * they select all elements that the selection offers.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class NoneffectiveFeaturesFilter implements FeatureSelectionFilter<Feature> {

    @Override
    public boolean check(Feature feature, int posCount, int negCount, int posIntersection, int negIntersection,
            LearningProblemSatistics lpStats) {
        return ((posIntersection + negIntersection) > 0)
                && ((posIntersection < posCount) || (negIntersection < negCount));
    }
}
