package org.dice_research.cel.tree.select;

import org.dice_research.cel.tree.Feature;

import javolution.util.FastBitSet;

/**
 * Returns the negative information gain!
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class InformationGainBasedComparator implements FeatureValueComparator<Feature, Double> {

    protected static final double LOG_2 = Math.log(2.0);

    @Override
    public Double extractValue(Feature feature, FastBitSet positives, FastBitSet negatives, int posIntersection,
            int negIntersection) {
        return Double.valueOf(-calculateInformationGain(positives.cardinality(), negatives.cardinality(),
                posIntersection, negIntersection));
    }

    @Override
    public Double getWorstValue() {
        return Double.valueOf(Double.MAX_VALUE);
    }

    protected static double calculateInformationGain(int numberOfPositives, int numberOfNegatives,
            int selectedPositives, int selectedNegatives) {
        double elementCount = numberOfPositives + numberOfNegatives;
        double selectionWeight = (selectedPositives + selectedNegatives) / elementCount;
        // Information gain is the entropy of the parent divided by the average entropy
        // of the children
        return calculateEntropy(numberOfPositives, numberOfNegatives)
                - ((selectionWeight * calculateEntropy(selectedPositives, selectedNegatives))
                        + ((1 - selectionWeight) * calculateEntropy(numberOfPositives - selectedPositives,
                                numberOfNegatives - selectedNegatives)));
    }

    protected static double calculateEntropy(int numberOfPositives, int numberOfNegatives) {
        double sum = numberOfPositives + numberOfNegatives;
        double posProb = numberOfPositives / sum;
        double negProb = numberOfNegatives / sum;
        return -posProb * (Math.log(posProb) / LOG_2) - negProb * (Math.log(negProb) / LOG_2);
    }
}
