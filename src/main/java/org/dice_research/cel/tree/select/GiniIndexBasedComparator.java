package org.dice_research.cel.tree.select;

import org.dice_research.cel.tree.Feature;

import javolution.util.FastBitSet;

public class GiniIndexBasedComparator implements FeatureValueComparator<Feature, Double> {

    protected static double calculateGiniIndex(int numberOfPositives, int numberOfNegatives, int selectedPositives,
            int selectedNegatives) {
        double sum = selectedPositives + selectedNegatives;
        if (sum <= 0) {
            return 1.0;
        } else {
            return 1 - (Math.pow(selectedPositives / sum, 2.0) + Math.pow(selectedNegatives / sum, 2.0));
        }
    }

    @Override
    public Double extractValue(Feature feature, FastBitSet positives, FastBitSet negatives, int posIntersection,
            int negIntersection) {
        return Double.valueOf(
                calculateGiniIndex(positives.cardinality(), negatives.cardinality(), posIntersection, negIntersection));
    }

    @Override
    public Double getWorstValue() {
        return Double.valueOf(1.0);
    }
}
