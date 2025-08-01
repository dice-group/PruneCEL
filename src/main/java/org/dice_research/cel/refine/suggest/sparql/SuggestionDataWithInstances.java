package org.dice_research.cel.refine.suggest.sparql;

import java.util.Collection;

import org.apache.jena.query.Query;
import org.dice_research.cel.expression.ClassExpression;
import org.dice_research.cel.refine.instances.ScoredIRIWithInstances;
import org.dice_research.cel.refine.instances.SelectionScoresWithInstances;

import javolution.util.FastBitSet;

public class SuggestionDataWithInstances {

    public ClassExpression suggestionPart;
    public ClassExpression basePart;
    public Query suggestionQuery;
    public FastBitSet positive;
    public FastBitSet negative;
    public int maxPos;
    public int maxNeg;

    public void addBaseScore(Collection<ScoredIRIWithInstances> scoredIris) {
        if (basePart != null) {
            scoredIris.forEach(s -> s.add(positive, negative));
        }
    }

    public void setBaseCounts(SelectionScoresWithInstances scores) {
        if (scores != null) {
            this.positive = scores.getSelectedPositives();
            this.negative = scores.getSelectedNegatives();
        }
    }
}
