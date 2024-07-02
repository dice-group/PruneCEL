package org.dice_research.cel.refine.suggest;

import java.util.Collection;

import org.dice_research.cel.expression.ClassExpression;
import org.dice_research.cel.expression.NamedClass;

public interface Suggestor {

    public static final NamedClass CONTEXT_POSITION_MARKER = new NamedClass("⌖");

    Collection<ScoredIRI> suggestClass(Collection<String> positive, Collection<String> negative,
            ClassExpression context);

    Collection<ScoredIRI> suggestNegatedClass(Collection<String> positive, Collection<String> negative,
            ClassExpression context);

    Collection<ScoredIRI> suggestProperty(Collection<String> positive, Collection<String> negative,
            ClassExpression context);

}
