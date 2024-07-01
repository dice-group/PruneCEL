package org.dice_research.cel.expression;

public class ScoredClassExpression {

    private ClassExpression classExpression;
    private double classificationScore;
    private double refinementScore;
    private int posCount;
    private int negCount;

    public ScoredClassExpression(ClassExpression classExpression, double classificationScore, double refinementScore, int posCount, int negCount) {
        super();
        this.classExpression = classExpression;
        this.classificationScore = classificationScore;
        this.refinementScore = refinementScore;
        this.posCount = posCount;
        this.negCount = negCount;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((classExpression == null) ? 0 : classExpression.hashCode());
        result = prime * result + negCount;
        result = prime * result + posCount;
        long temp;
        temp = Double.doubleToLongBits(classificationScore);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ScoredClassExpression other = (ScoredClassExpression) obj;
        if (classExpression == null) {
            if (other.classExpression != null)
                return false;
        } else if (!classExpression.equals(other.classExpression))
            return false;
        if (negCount != other.negCount)
            return false;
        if (posCount != other.posCount)
            return false;
        if (Double.doubleToLongBits(classificationScore) != Double.doubleToLongBits(other.classificationScore))
            return false;
        return true;
    }

    /**
     * @return the classExpression
     */
    public ClassExpression getClassExpression() {
        return classExpression;
    }

    /**
     * @param classExpression the classExpression to set
     */
    public void setClassExpression(ClassExpression classExpression) {
        this.classExpression = classExpression;
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
     * @return the classificationScore
     */
    public double getClassificationScore() {
        return classificationScore;
    }

    /**
     * @param classificationScore the classificationScore to set
     */
    public void setClassificationScore(double classificationScore) {
        this.classificationScore = classificationScore;
    }

    /**
     * @return the refinementScore
     */
    public double getRefinementScore() {
        return refinementScore;
    }

    /**
     * @param refinementScore the refinementScore to set
     */
    public void setRefinementScore(double refinementScore) {
        this.refinementScore = refinementScore;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ScoredClassExpression [classExpression=");
        builder.append(classExpression);
        builder.append(", cScore=");
        builder.append(classificationScore);
        builder.append(", rScore=");
        builder.append(refinementScore);
        builder.append(", posCount=");
        builder.append(posCount);
        builder.append(", negCount=");
        builder.append(negCount);
        builder.append("]");
        return builder.toString();
    }

}
