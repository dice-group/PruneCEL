package org.dice_research.cel.tree;

import javolution.util.FastBitSet;

public class SimpleFeatureProfile implements Feature {

    protected FastBitSet positives;
    protected FastBitSet negatives;
    
    public SimpleFeatureProfile() {
        this.positives = FastBitSet.newInstance();
        this.negatives = FastBitSet.newInstance();
    }

    public SimpleFeatureProfile(FastBitSet positives, FastBitSet negatives) {
        super();
        this.positives = positives;
        this.negatives = negatives;
    }



    /**
     * @return the positives
     */
    @Override
    public FastBitSet getSelectedPositives() {
        return positives;
    }

    /**
     * @param positives the positives to set
     */
    public void setSelectedPositives(FastBitSet positives) {
        this.positives = positives;
    }

    /**
     * @return the negatives
     */
    @Override
    public FastBitSet getSelectedNegatives() {
        return negatives;
    }

    /**
     * @param negatives the negatives to set
     */
    public void setSelectedNegatives(FastBitSet negatives) {
        this.negatives = negatives;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SimpleFeatureProfile [positives=");
        builder.append(positives);
        builder.append(", negatives=");
        builder.append(negatives);
        builder.append("]");
        return builder.toString();
    }

}
