package org.dice_research.cel.tree.select;

import org.dice_research.cel.tree.Feature;

import javolution.util.FastBitSet;

/**
 * Smaller values are better!
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public interface FeatureValueComparator<T extends Feature, V extends Comparable<V>> {

    V extractValue(T feature, FastBitSet positives, FastBitSet negatives, int posIntersection,
            int negIntersection);

    V getWorstValue();
}
