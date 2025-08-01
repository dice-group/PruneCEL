package org.dice_research.cel.tree.select;

import java.util.Collection;

import org.dice_research.cel.tree.Feature;

import javolution.util.FastBitSet;

public interface FeatureSelector<T extends Feature> {

    T selectBestFeature(Collection<T> features, FastBitSet positives, FastBitSet negatives);
}
