package org.dice_research.cel.tree.select;

import org.dice_research.cel.tree.Feature;

import javolution.util.FastBitSet;

public class SelectedPositiveExamplesComparator implements FeatureValueComparator<Feature, Integer> {

    @Override
    public Integer extractValue(Feature feature, FastBitSet positives, FastBitSet negatives, int posIntersection,
            int negIntersection) {
        return -posIntersection;
    }

    @Override
    public Integer getWorstValue() {
        return 0;
    }

}
