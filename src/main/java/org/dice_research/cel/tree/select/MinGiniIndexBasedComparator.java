package org.dice_research.cel.tree.select;

import org.dice_research.cel.tree.Feature;

import javolution.util.FastBitSet;

/**
 * It works like the {@link GiniIndexBasedComparator}, but calculates two Gini
 * indexes. The first is the usual Gini index for applying the given feature and
 * looking at the values it selects. The second looks at the opposite of the
 * feature (i.e., using the feature in a negated way) and calculates the Gini
 * feature for that, since for a decision tree, it doesn't matter whether a
 * feature is used in its "normal" or in the negated way. It returns the minimum
 * of the calculated indexes.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class MinGiniIndexBasedComparator extends GiniIndexBasedComparator {

    @Override
    public Double extractValue(Feature feature, FastBitSet positives, FastBitSet negatives, int posIntersection,
            int negIntersection) {
        int posCount = positives.cardinality();
        int negCount = negatives.cardinality();
        return Double.valueOf(Math.min(calculateGiniIndex(posCount, negCount, posIntersection, negIntersection),
                calculateGiniIndex(posCount, negCount, posCount - posIntersection, negCount - negIntersection)));
    }

}
