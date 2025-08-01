package org.dice_research.cel.tree.select;

import java.util.Collection;

import org.dice_research.cel.tree.Feature;

import javolution.util.FastBitSet;

public class GenericFeatureSelector<T extends Feature> implements FeatureSelector<T> {

    @SuppressWarnings("rawtypes")
    protected FeatureValueComparator[] featureComparator;

    public GenericFeatureSelector(
            @SuppressWarnings("unchecked") FeatureValueComparator<? super T, ?>... featureComparator) {
        super();
        this.featureComparator = featureComparator;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public T selectBestFeature(Collection<T> features, FastBitSet positives, FastBitSet negatives) {
        int posCount = positives.cardinality();
        int negCount = negatives.cardinality();
        T bestFeature = null;
        Comparable[] bestProfile = new Comparable[featureComparator.length];
        for (int i = 0; i < featureComparator.length; ++i) {
            bestProfile[i] = featureComparator[i].getWorstValue();
        }
        Comparable[] currentProfile = new Comparable[featureComparator.length];
        int posIntersection;
        int negIntersection;
        for (T feature : features) {
            posIntersection = FastBitSet.andCardinality(positives, feature.getSelectedPositives());
            negIntersection = FastBitSet.andCardinality(negatives, feature.getSelectedNegatives());
            // Exclude features that do not change anything
            if (((posIntersection + negIntersection) > 0)
                    && ((posIntersection < posCount) || (negIntersection < negCount))) {
                int diff = 0;
                for (int i = 0; (diff < 1) && (i < featureComparator.length); ++i) {
                    currentProfile[i] = featureComparator[i].extractValue(feature, positives, negatives,
                            posIntersection, negIntersection);
                    if (diff == 0) {
                        diff = currentProfile[i].compareTo(bestProfile[i]);
                    }
                }
                if (diff < 1) {
                    bestFeature = feature;
                    Comparable[] temp = bestProfile;
                    bestProfile = currentProfile;
                    currentProfile = temp;
                }
            }
        }
        return bestFeature;
    }

}
