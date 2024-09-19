package org.dice_research.cel.io.csv;

import java.io.PrintStream;

import org.dice_research.cel.expression.ScoredClassExpression;
import org.dice_research.cel.io.IntermediateResultPrinter;
import org.dice_research.topicmodeling.commons.collections.TopDoubleObjectCollection;

public class CSVIntermediateResultPrinter implements IntermediateResultPrinter, AutoCloseable {

    protected PrintStream pout;
    protected double oldPerformance = Double.NEGATIVE_INFINITY;

    public CSVIntermediateResultPrinter(PrintStream pout) {
        this.pout = pout;
        pout.print("time,positives,negatives,cscore,rscore,expression");
    }

    @Override
    public void printIntermediateResults(TopDoubleObjectCollection<ScoredClassExpression> topExpressions) {
        ScoredClassExpression exp = (ScoredClassExpression) topExpressions.objects[0];
        if (exp.getClassificationScore() > oldPerformance) {
            pout.print(System.currentTimeMillis());
            pout.print(',');
            pout.print(exp.getPosCount());
            pout.print(',');
            pout.print(exp.getNegCount());
            pout.print(',');
            pout.print(exp.getClassificationScore());
            pout.print(',');
            pout.print(exp.getRefinementScore());
            pout.print(',');
            pout.println(exp.getClassExpression().toString());
            oldPerformance = exp.getClassificationScore();
        }
    }

    @Override
    public void close() throws Exception {
        pout.close();
    }

}
