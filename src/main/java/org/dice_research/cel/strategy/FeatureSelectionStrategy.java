package org.dice_research.cel.strategy;

import org.dice_research.cel.data.LearningProblemSatistics;
import org.dice_research.cel.tree.DecisionTreeNode;
import org.dice_research.cel.tree.Feature;

public interface FeatureSelectionStrategy {

    Feature[] determinePatterns(DecisionTreeNode root, LearningProblemSatistics lpStats);
}
