package org.dice_research.cel.tree;

import java.util.Collection;
import java.util.stream.Stream;

import org.dice_research.cel.data.LearningProblemSatistics;
import org.dice_research.cel.tree.select.FeatureSelector;

import javolution.util.FastBitSet;

/**
 * A simple decision tree learner that can update the tree given a collection of
 * newly created features.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class UpdatingDecisionTreeLearner<T extends Feature> extends DecisionTreeLearner<T> {

    public UpdatingDecisionTreeLearner(FeatureSelector<T> selector) {
        super(selector);
    }

    // Method to train a decision tree.
    public DecisionTreeNode learn(Collection<T> features, LearningProblemSatistics lpStats) {
        return update(null, features, null, lpStats);
    }

    // Method to train a decision tree.
    public DecisionTreeNode update(DecisionTreeNode root, Collection<T> allFeatures, Collection<T> newFeatures,
            LearningProblemSatistics lpStats) {
        FastBitSet positives = FastBitSet.newInstance();
        positives.set(0, lpStats.numberOfPositives);
        FastBitSet negatives = FastBitSet.newInstance();
        negatives.set(0, lpStats.numberOfNegatives);
        return updateTree(root, allFeatures, newFeatures, positives, negatives, lpStats);
    }

    @SuppressWarnings("unchecked")
    protected DecisionTreeNode updateTree(DecisionTreeNode node, Collection<T> allFeatures, Collection<T> newFeatures,
            FastBitSet positives, FastBitSet negatives, LearningProblemSatistics lpStats) {
        // If all examples have the same label, create a leaf node.
        if (positives.cardinality() == 0) {
            // Nothing to change --> Return the old node or (if it does not exist) create a
            // new one
            return node == null ? new DecisionTreeNode(false, positives, negatives) : node;
        }
        if (negatives.cardinality() == 0) {
            // Nothing to change --> Return the old node or (if it does not exist) create a
            // new one
            return node == null ? new DecisionTreeNode(true, positives, negatives) : node;
        }

        // Select the best feature to split on
        Feature bestFeature;
        if (node == null) {
            bestFeature = selector.selectBestFeature(allFeatures, positives, negatives, lpStats);
        } else {
            Stream<T> features = newFeatures.stream();
            if (node.getFeature() != null) {
                features = Stream.concat(Stream.of((T) node.getFeature()), features);
            }
            bestFeature = selector.selectBestFeature(features, positives, negatives, lpStats);
        }
        if (bestFeature == null) {
            // Nothing to change --> Return the old node or (if it does not exist) create a
            // new one
            return node == null
                    ? new DecisionTreeNode(positives.cardinality() >= negatives.cardinality(), positives, negatives)
                    : node;
        }

        DecisionTreeNode root;
        // If the old node was already based on the best feature
        if ((node != null) && (node.getFeature() != null) && (node.getFeature().equals(bestFeature))) {
            DecisionTreeNode child;
            root = node;
            // Update the true child
            child = node.getTrueChild();
            root.setTrueChild(
                    updateTree(child, allFeatures, newFeatures, child.getPositives(), child.getNegatives(), lpStats));
            // Update the false child
            child = node.getFalseChild();
            root.setFalseChild(
                    updateTree(child, allFeatures, newFeatures, child.getPositives(), child.getNegatives(), lpStats));
        } else {
            // We found a better feature than the old one... create a new node
            // Create the root node with the selected feature and threshold
            root = new DecisionTreeNode(bestFeature, positives, negatives);

            // Split data based on the chosen feature and recurse for each subset.
            FastBitSet newPositives = createBitSetForChild(positives, lpStats.numberOfPositives,
                    bestFeature.getSelectedPositives(), false);
            FastBitSet newNegatives = createBitSetForChild(negatives, lpStats.numberOfNegatives,
                    bestFeature.getSelectedNegatives(), false);

            root.setTrueChild(updateTree(null, allFeatures, newFeatures, newPositives, newNegatives, lpStats));

            newPositives = createBitSetForChild(positives, lpStats.numberOfPositives,
                    bestFeature.getSelectedPositives(), true);
            newNegatives = createBitSetForChild(negatives, lpStats.numberOfNegatives,
                    bestFeature.getSelectedNegatives(), true);
            root.setFalseChild(updateTree(null, allFeatures, newFeatures, newPositives, newNegatives, lpStats));
        }

        return root;
    }

}