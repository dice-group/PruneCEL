package org.dice_research.cel.tree.select;

import java.util.Collection;
import java.util.stream.Stream;

import org.dice_research.cel.data.LearningProblemSatistics;
import org.dice_research.cel.tree.Feature;

import javolution.util.FastBitSet;

public interface FeatureSelector<T extends Feature> {

    default T selectBestFeature(Collection<T> features, FastBitSet positives, FastBitSet negatives,
            LearningProblemSatistics lpStats) {
        return selectBestFeature(features.stream(), positives, negatives, lpStats);
    }

    T selectBestFeature(Stream<T> features, FastBitSet positives, FastBitSet negatives,
            LearningProblemSatistics lpStats);
}
