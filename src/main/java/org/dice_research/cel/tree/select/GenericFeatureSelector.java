package org.dice_research.cel.tree.select;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.dice_research.cel.data.LearningProblemSatistics;
import org.dice_research.cel.tree.Feature;

import javolution.util.FastBitSet;

public class GenericFeatureSelector<T extends Feature> implements FeatureSelector<T> {

    @SuppressWarnings("rawtypes")
    protected FeatureValueComparator[] featureComparator;
    @Nullable
    protected List<FeatureSelectionFilter<? super T>> filters;

    public GenericFeatureSelector(
            @SuppressWarnings("unchecked") FeatureValueComparator<? super T, ?>... featureComparator) {
        this(null, featureComparator);
    }

    public GenericFeatureSelector(List<FeatureSelectionFilter<? super T>> filters,
            @SuppressWarnings("unchecked") FeatureValueComparator<? super T, ?>... featureComparator) {
        super();
        this.filters = filters;
        this.featureComparator = featureComparator;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public T selectBestFeature(Stream<T> featureStream, FastBitSet positives, FastBitSet negatives,
            LearningProblemSatistics lpStats) {
        T bestFeature = null;
        Comparable[] bestProfile = new Comparable[featureComparator.length];
        for (int i = 0; i < featureComparator.length; ++i) {
            bestProfile[i] = featureComparator[i].getWorstValue();
        }
        Comparable[] currentProfile = new Comparable[featureComparator.length];
        int posCount = positives.cardinality();
        int negCount = negatives.cardinality();
        int posIntersection;
        int negIntersection;
        Iterator<T> iterator = featureStream.iterator();
        T feature;
        while (iterator.hasNext()) {
            // TODO Make the filter below optional as it is needed when choosing a feature
            // for learning, but not when choosing it for refinement!!!
            feature = iterator.next();
            posIntersection = FastBitSet.andCardinality(positives, feature.getSelectedPositives());
            negIntersection = FastBitSet.andCardinality(negatives, feature.getSelectedNegatives());

            // Exclude features that do not change anything
            if (filters == null || check(feature, posCount, negCount, posIntersection, negIntersection, lpStats)) {
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

    protected boolean check(T feature, int posCount, int negCount, int posIntersection, int negIntersection,
            LearningProblemSatistics lpStats) {
        // TODO This loop is not so nice... this could be improved...
        for (FeatureSelectionFilter<? super T> filter : filters) {
            if (!filter.check(feature, posCount, negCount, posIntersection, negIntersection, lpStats))
                return false;
        }
        return true;
    }

}
