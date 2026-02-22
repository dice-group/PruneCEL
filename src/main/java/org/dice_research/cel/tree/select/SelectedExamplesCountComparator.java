package org.dice_research.cel.tree.select;

import org.dice_research.cel.tree.Feature;

import javolution.util.FastBitSet;

/**
 * Returns the negative sum of the selected positive and negative elements.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class SelectedExamplesCountComparator implements FeatureValueComparator<Feature, Integer> {

    private static final Integer ZERO = Integer.valueOf(0);

    @Override
    public Integer extractValue(Feature feature, FastBitSet positives, FastBitSet negatives, int posIntersection,
            int negIntersection) {
        return -(posIntersection + negIntersection);
    }

    @Override
    public Integer getWorstValue() {
        return ZERO;
    }

}
