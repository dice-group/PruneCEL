package org.dice_research.cel.recursive;

import java.util.Comparator;

import org.dice_research.cel.expression.ScoredCEComparatorForRefinement;
import org.dice_research.cel.expression.ScoredClassExpression;

public class SolutionComparatorForRefinement implements Comparator<Solution> {

    protected Comparator<ScoredClassExpression> sceComparator = null;

    public SolutionComparatorForRefinement() {
        this(new ScoredCEComparatorForRefinement());
    }

    public SolutionComparatorForRefinement(Comparator<ScoredClassExpression> sceComparator) {
        super();
        this.sceComparator = sceComparator;
    }

    @Override
    public int compare(Solution s1, Solution s2) {
        return sceComparator.compare(s1.getMergedSolution(), s2.getMergedSolution());
    }

}
