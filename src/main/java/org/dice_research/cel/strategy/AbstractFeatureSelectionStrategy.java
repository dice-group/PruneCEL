package org.dice_research.cel.strategy;

import java.util.List;

import org.dice_research.cel.data.LearningProblemSatistics;
import org.dice_research.cel.tree.DecisionTreeNode;
import org.dice_research.cel.tree.Feature;

public abstract class AbstractFeatureSelectionStrategy implements FeatureSelectionStrategy {

    protected DirtyLeafNodeClassifier leafClassifier; // = new SimpleDirtyLeafNodeClassifier();

    /**
     * Constructor.
     * 
     * @param leafClassifier Classifier deciding whether a given leaf node should be
     *                       seen as dirty or not.
     */
    public AbstractFeatureSelectionStrategy(DirtyLeafNodeClassifier leafClassifier) {
        super();
        this.leafClassifier = leafClassifier;
    }

    protected void handleNode(DecisionTreeNode node, List<Feature> result, LearningProblemSatistics lpStats) {
        if (node.isLeaf()) {
            handleLeafNode(node, result, lpStats);
            return;
        }
        if (node.getTrueChild() != null) {
            handleNode(node.getTrueChild(), result, lpStats);
        }
        if (node.getFalseChild() != null) {
            handleNode(node.getFalseChild(), result, lpStats);
        }
    }

    protected void handleLeafNode(DecisionTreeNode node, List<Feature> result, LearningProblemSatistics lpStats) {
        if (leafClassifier.isDirty(node, lpStats)) {
            handleDirtyLeafNode(node, result);
        }
    }

    protected abstract void handleDirtyLeafNode(DecisionTreeNode node, List<Feature> result);

    /**
     * @return the leafClassifier
     */
    public DirtyLeafNodeClassifier getLeafClassifier() {
        return leafClassifier;
    }

    /**
     * @param leafClassifier the leafClassifier to set
     */
    public void setLeafClassifier(DirtyLeafNodeClassifier leafClassifier) {
        this.leafClassifier = leafClassifier;
    }

}