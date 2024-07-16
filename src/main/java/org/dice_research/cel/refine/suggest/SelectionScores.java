package org.dice_research.cel.refine.suggest;

/**
 * A simple data class that stores the number of positive and negative examples
 * that an expression selected (without having an explicit relation to the
 * expression).
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class SelectionScores {

    /**
     * The number of positive examples that have been selected.
     */
    protected int posCount;
    /**
     * The number of negative examples that have been selected.
     */
    protected int negCount;

    /**
     * Constructor.
     * 
     * @param posCount The number of positive examples that have been selected
     * @param negCount The number of negative examples that have been selected
     */
    public SelectionScores(int posCount, int negCount) {
        super();
        this.posCount = posCount;
        this.negCount = negCount;
    }

    /**
     * @return the posCount
     */
    public int getPosCount() {
        return posCount;
    }

    /**
     * @param posCount the posCount to set
     */
    public void setPosCount(int posCount) {
        this.posCount = posCount;
    }

    /**
     * @return the negCount
     */
    public int getNegCount() {
        return negCount;
    }

    /**
     * @param negCount the negCount to set
     */
    public void setNegCount(int negCount) {
        this.negCount = negCount;
    }
}
