package org.dice_research.cel.refine.instances;

import org.dice_research.cel.refine.suggest.SelectionScores;
import org.dice_research.cel.tree.Feature;

import javolution.util.FastBitSet;

/**
 * A simple data class that stores the number of positive and negative examples
 * that an expression selected (without having an explicit relation to the
 * expression).
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class SelectionScoresWithInstances extends SelectionScores implements Feature {

    protected FastBitSet selectedPos;
    protected FastBitSet selectedNeg;

    public SelectionScoresWithInstances(FastBitSet selectedPos, FastBitSet selectedNeg) {
        super(selectedPos.cardinality(), selectedNeg.cardinality());
        this.selectedPos = selectedPos;
        this.selectedNeg = selectedNeg;
    }

    @Override
    public FastBitSet getSelectedPositives() {
        return selectedPos;
    }

    @Override
    public FastBitSet getSelectedNegatives() {
        return selectedNeg;
    }

}
