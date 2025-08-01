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
public class BiggestPureSetComparator implements FeatureValueComparator<Feature, Double> {

    @Override
    public Double extractValue(Feature feature, FastBitSet positives, FastBitSet negatives, int posIntersection,
            int negIntersection) {
        int posDiff = positives.cardinality() - posIntersection;
        int negDiff = negatives.cardinality() - negIntersection;
        if ((posIntersection == 0) || (negIntersection == 0) || (posDiff == 0) || (negDiff == 0)) {
            // if the intersection is a pure set, get it's maximum value, else 0
            double interMax = (posIntersection == 0) ? negIntersection : ((negIntersection == 0) ? posIntersection : 0);
            // if the difference is a pure set, get it's maximum value, else 0
            double diffMax = (posDiff == 0) ? negDiff : ((negDiff == 0) ? posDiff : 0);
            return -Double.valueOf(interMax > diffMax ? interMax : diffMax);
        } else {
            return Double.valueOf(0.0);
        }
    }

    @Override
    public Double getWorstValue() {
        // TODO Auto-generated method stub
        return Double.valueOf(0.0);
    }

}
