package org.dice_research.cel;

import org.dice_research.cel.tree.DecisionTreeNode;
import org.dice_research.cel.tree.Feature;

public interface FeatureSelectionStrategy {

    Feature[] determinePatterns(DecisionTreeNode root);
}
