package org.dice_research.cel.refine.suggest;

public class SelectionScores {

    protected int posCount;
    protected int negCount;

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
