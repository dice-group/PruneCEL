package org.dice_research.cel.refine.instances;

import java.util.Collection;
import java.util.Map;

import org.dice_research.cel.expression.ClassExpression;
import org.dice_research.cel.refine.suggest.Suggestor;

/**
 * An interface of a class that is able to suggest nodes from a graph that could
 * be used to extend a given class expression at a marked position. All
 * suggestions come with counts how many positive and negative examples would be
 * selected if the class expression is extended with the returned suggestion.
 * 
 * <p>
 * <b>Note:</b> It is important that the given class expressions have exactly
 * one position that is marked. The {@link Suggestor#CONTEXT_POSITION_MARKER} should be
 * used for marking the position.
 * </p>
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public interface SuggestorWithInstances {

    /**
     * Method that suggests named classes that can be used at the marked position.
     * 
     * @param positive positive examples
     * @param negative negative examples
     * @param context  the class expression that contains the position marking
     * @return the IRIs of the suggested classes together with their counts
     */
    Collection<ScoredIRIWithInstances> suggestClassWithInstances(Collection<String> positive,
            Collection<String> negative, Map<String, Integer> positiveMaps, Map<String, Integer> negativeMaps,
            ClassExpression context);

    /**
     * Method that suggests negated named classes that can be used at the marked
     * position.
     * 
     * @param positive positive examples
     * @param negative negative examples
     * @param context  the class expression that contains the position marking
     * @return the IRIs of the suggested classes together with their counts
     */
    Collection<ScoredIRIWithInstances> suggestNegatedClassWithInstances(Collection<String> positive,
            Collection<String> negative, Map<String, Integer> positiveMaps, Map<String, Integer> negativeMaps,
            ClassExpression context);

    /**
     * Method that suggests named roles that can be used at the marked position
     * together with an existential quantifier.
     * 
     * @param positive positive examples
     * @param negative negative examples
     * @param context  the class expression that contains the position marking
     * @return the IRIs of the suggested roles together with their counts
     */
    Collection<ScoredIRIWithInstances> suggestPropertyWithInstances(Collection<String> positive,
            Collection<String> negative, Map<String, Integer> positiveMaps, Map<String, Integer> negativeMaps,
            ClassExpression context);

}
