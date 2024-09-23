package org.dice_research.cel.refine.suggest.sparql;

import java.util.List;

import org.dice_research.cel.expression.ClassExpression;
import org.dice_research.cel.refine.suggest.ScoredIRI;
import org.dice_research.cel.refine.suggest.SelectionScores;

public class SuggestionData {

    public ClassExpression suggestionPart;
    public ClassExpression basePart;
    public String suggestionQuery;
    public int posCount;
    public int negCount;
    public int maxPos;
    public int maxNeg;

    public void addBaseScore(List<ScoredIRI> scoredIris) {
        if (basePart != null) {
            scoredIris.forEach(s -> s.add(posCount, negCount));
        }
    }

    public void setBaseCounts(SelectionScores scores) {
        if (scores != null) {
            this.posCount = scores.posCount;
            this.negCount = scores.negCount;
        }
    }
}
