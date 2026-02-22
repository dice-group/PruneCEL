package org.dice_research.cel.tree.select;

import org.dice_research.cel.tree.Feature;

import javolution.util.FastBitSet;

/**
 * Returns the size of the largest pure set multiplied with -1, or 0 if no such
 * set exists.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class BiggestPureSetComparator implements FeatureValueComparator<Feature, Integer> {

    private static final Integer ZERO = Integer.valueOf(0);

    @Override
    public Integer extractValue(Feature feature, FastBitSet positives, FastBitSet negatives, int posIntersection,
            int negIntersection) {
        int posDiff = positives.cardinality() - posIntersection;
        int negDiff = negatives.cardinality() - negIntersection;
        if ((posIntersection == 0) || (negIntersection == 0) || (posDiff == 0) || (negDiff == 0)) {
            // if the intersection is a pure set, get it's maximum value, else 0
            int interMax = (posIntersection == 0) ? negIntersection : ((negIntersection == 0) ? posIntersection : 0);
            // if the difference is a pure set, get it's maximum value, else 0
            int diffMax = (posDiff == 0) ? negDiff : ((negDiff == 0) ? posDiff : 0);
            return -Integer.valueOf(interMax > diffMax ? interMax : diffMax);
        } else {
            return ZERO;
        }
    }

    @Override
    public Integer getWorstValue() {
        return ZERO;
    }

}
