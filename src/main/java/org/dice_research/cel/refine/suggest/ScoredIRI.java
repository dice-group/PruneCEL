package org.dice_research.cel.refine.suggest;

public class ScoredIRI extends SelectionScores {

    protected String iri;
    protected boolean inverted;

    public ScoredIRI(String iri, int posCount, int negCount) {
        this(iri, posCount, negCount, false);
    }

    public ScoredIRI(String iri, int posCount, int negCount, boolean inverted) {
        super(posCount, negCount);
        this.iri = iri;
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
