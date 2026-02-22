package org.dice_research.cel.strategy;

import java.util.Arrays;
import java.util.List;

import org.dice_research.cel.data.LearningProblemSatistics;
import org.dice_research.cel.tree.DecisionTreeNode;
import org.dice_research.cel.tree.Feature;
import org.dice_research.cel.tree.SimpleFeatureProfile;

public class AllNodesFeatureSelectionStrategy extends AbstractFeatureSelectionStrategy {

    /**
     * Constructor.
     * 
     * @param leafClassifier Classifier deciding whether a given leaf node should be
     *                       seen as dirty or not.
     */
    public AllNodesFeatureSelectionStrategy(DirtyLeafNodeClassifier leafClassifier) {
        super(leafClassifier);
    }

    @Override
    public Feature[] determinePatterns(DecisionTreeNode root, LearningProblemSatistics lpStats) {
        Feature globalPattern = new SimpleFeatureProfile();
        List<Feature> result = Arrays.asList(globalPattern);
        handleNode(root, result, lpStats);
        if ((globalPattern.getSelectedPositives().cardinality() > 0)
                || (globalPattern.getSelectedNegatives().cardinality() > 0)) {
            return new Feature[] { globalPattern };
        } else {
            return new Feature[] {};
        }
    }

    protected void handleDirtyLeafNode(DecisionTreeNode node, List<Feature> result) {
        Feature globalPattern = result.get(0);
        globalPattern.getSelectedPositives().or(node.getPositives());
        globalPattern.getSelectedNegatives().or(node.getNegatives());
    }
}