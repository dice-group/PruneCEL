package org.dice_research.cel.strategy;

import java.util.ArrayList;
import java.util.List;

import org.dice_research.cel.data.LearningProblemSatistics;
import org.dice_research.cel.tree.DecisionTreeNode;
import org.dice_research.cel.tree.Feature;
import org.dice_research.cel.tree.SimpleFeatureProfile;

import javolution.util.FastBitSet;

public class SingleNodeFeatureSelectionStrategy extends AbstractFeatureSelectionStrategy {

    /**
     * Constructor.
     * 
     * @param leafClassifier Classifier deciding whether a given leaf node should be
     *                       seen as dirty or not.
     */
    public SingleNodeFeatureSelectionStrategy(DirtyLeafNodeClassifier leafClassifier) {
        super(leafClassifier);
    }

    @Override
    public Feature[] determinePatterns(DecisionTreeNode root, LearningProblemSatistics lpStats) {
        List<Feature> result = new ArrayList<>();
        handleNode(root, result, lpStats);
        return result.toArray(Feature[]::new);
    }

    protected void handleDirtyLeafNode(DecisionTreeNode node, List<Feature> result) {
        result.add(new SimpleFeatureProfile(cloneBitSet(node.getPositives()), cloneBitSet(node.getNegatives())));
    }

    private FastBitSet cloneBitSet(FastBitSet leafPos) {
        FastBitSet result = FastBitSet.newInstance();
        result.or(leafPos);
        return result;
    }
}