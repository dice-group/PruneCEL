package org.dice_research.cel.tree.select;

import org.dice_research.cel.tree.Feature;

import javolution.util.FastBitSet;

/**
 * A simple comparator that returns the number of elements that are additionally
 * selected (the less additionally selected elements, the better).
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class AdditionallySelectedElementsComparator implements FeatureValueComparator<Feature, Integer> {

    private static final Integer MAX_VALUE = Integer.valueOf(Integer.MAX_VALUE);

    @Override
    public Integer extractValue(Feature feature, FastBitSet positives, FastBitSet negatives, int posIntersection,
            int negIntersection) {
        return (feature.getSelectedPositives().cardinality() + feature.getSelectedNegatives().cardinality())
                - (posIntersection + negIntersection);
    }

    @Override
    public Integer getWorstValue() {
        return MAX_VALUE;
    }

}
