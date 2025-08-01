package org.dice_research.cel;

import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

import org.dice_research.cel.expression.ScoredClassExpression;
import org.dice_research.cel.io.IntermediateResultPrinter;

public abstract class AbstractConceptLearner implements ConceptLearner {

    protected long maxTime = 0L;
    protected int maxIterations = 0;
    protected boolean debugMode = false;

    /**
     * @return the maxTime
     */
    public long getMaxTime() {
        return maxTime;
    }

    /**
     * @param maxTime the maxTime to set
     */
    public void setMaxTime(long maxTime) {
        this.maxTime = maxTime;
    }

    /**
     * @return the maxIterations
     */
    public int getMaxIterations() {
        return maxIterations;
    }

    /**
     * @param maxIterations the maxIterations to set
     */
    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    @Override
    public List<ScoredClassExpression> findClassExpression(Collection<String> positive, Collection<String> negative,
            OutputStream logStream, IntermediateResultPrinter iResultPrinter) {
        long startTime = System.currentTimeMillis();
        if (iResultPrinter != null) {
            iResultPrinter.setStartTime(startTime);
        }
        long timeToStop = maxTime > 0 ? startTime + maxTime : 0;
        return findClassExpression(positive, negative, logStream, iResultPrinter, startTime, timeToStop);
    }

    public abstract List<ScoredClassExpression> findClassExpression(Collection<String> positive, Collection<String> negative,
            OutputStream logStream, IntermediateResultPrinter iResultPrinter, long startTime, long timeToStop); 
}
