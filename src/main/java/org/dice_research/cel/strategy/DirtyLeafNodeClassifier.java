package org.dice_research.cel.strategy;

import org.dice_research.cel.data.LearningProblemSatistics;
import org.dice_research.cel.tree.DecisionTreeNode;

public interface DirtyLeafNodeClassifier {

    boolean isDirty(DecisionTreeNode node, LearningProblemSatistics lpStats);
}
