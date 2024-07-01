package org.dice_research.cel.refine.suggest;

public class ScoredIRI {

    private String iri;
    private int posCount;
    private int negCount;
    private boolean inverted;

    public ScoredIRI(String iri, int posCount, int negCount) {
        this(iri, posCount, negCount, false);
    }

    public ScoredIRI(String iri, int posCount, int negCount, boolean inverted) {
        super();
        this.iri = iri;
        this.posCount = posCount;
        this.negCount = negCount;
        this.inverted = inverted;
    }

    /**
     * @return the iri
     */
    public String getIri() {
        return iri;
    }

    /**
     * @param iri the iri to set
     */
    public void setIri(String iri) {
        this.iri = iri;
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

    /**
     * @return the inverted
     */
    public boolean isInverted() {
        return inverted;
    }

    /**
     * @param inverted the inverted to set
     */
    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ScoredIRI [iri=");
        builder.append(iri);
        builder.append(", posCount=");
        builder.append(posCount);
        builder.append(", negCount=");
        builder.append(negCount);
        builder.append(", inverted=");
        builder.append(inverted);
        builder.append("]");
        return builder.toString();
    }
}
