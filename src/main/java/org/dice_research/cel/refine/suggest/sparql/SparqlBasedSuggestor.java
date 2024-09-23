package org.dice_research.cel.refine.suggest.sparql;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jenax.arq.connection.core.QueryExecutionFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.core.DatasetDescription;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.cel.DescriptionLogic;
import org.dice_research.cel.expression.ClassExpression;
import org.dice_research.cel.expression.Junction;
import org.dice_research.cel.expression.NegatingVisitor;
import org.dice_research.cel.refine.suggest.ExtendedSuggestor;
import org.dice_research.cel.refine.suggest.ScoredIRI;
import org.dice_research.cel.refine.suggest.SelectionScores;
import org.dice_research.cel.refine.suggest.Suggestor;
import org.dice_research.cel.sparql.InstanceRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jsonldjava.shaded.com.google.common.collect.Iterators;

public class SparqlBasedSuggestor implements ExtendedSuggestor, InstanceRetriever, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SparqlBasedSuggestor.class);

    protected QueryExecutionFactory queryExecFactory;
    protected Set<String> classBlackList = new HashSet<String>();
    protected Set<String> propertyBlackList = new HashSet<String>();
    protected DescriptionLogic logic;
    protected DisjunctionCheckingVisitor checker = new DisjunctionCheckingVisitor();
    protected SuggestionCheckingVisitor sugChecker = new SuggestionCheckingVisitor();
    protected ExpressionPreProcessor preprocessor = new ExpressionPreProcessor();

    public SparqlBasedSuggestor(QueryExecutionFactory queryExecFactory, DescriptionLogic logic) {
        this.queryExecFactory = queryExecFactory;
        this.logic = logic;
    }

    @Override
    public void close() throws Exception {
        if (queryExecFactory != null) {
            queryExecFactory.close();
        }
    }

    protected Collection<ScoredIRI> performQuery(SuggestionData data, Collection<String> positive,
            Collection<String> negative, Function<QuerySolution, ScoredIRI> transformation,
            Collection<ScoredIRI> results) {
        // Execute base query
        if (data.basePart != null) {
            data.setBaseCounts(scorePreparedExpression(data.basePart, positive, negative));
        }

        LOGGER.trace("Sending query {}", data.suggestionQuery);
        // Create the query execution with try-catch to ensure that it will be closed
        try (QueryExecution qe = queryExecFactory.createQueryExecution(data.suggestionQuery);) {
            ResultSet result = qe.execSelect();
            ScoredIRI sIri;
            List<ScoredIRI> scoredIris = new ArrayList<>();
            while (result.hasNext()) {
                sIri = transformation.apply(result.next());
                if ((sIri != null) && ((sIri.posCount + sIri.negCount) > 0)) {
                    scoredIris.add(sIri);
                }
                // else: we found a blank node or a filtered IRI. OR we found IRIs that do not
                // have any count > 0. We can ignore them.
            }
            data.addBaseScore(scoredIris);
            Optional<ScoredIRI> faultyResult = scoredIris.stream().filter(
                    s -> s.posCount < 0 || s.posCount > data.maxPos || s.negCount < 0 || s.posCount > data.maxNeg)
                    .findFirst();
            if (faultyResult.isPresent()) {
                LOGGER.error("Got a faulty count: #positives={}, #negatives={}, scoredIRI={}", data.maxPos, data.maxNeg,
                        faultyResult);
            }
            results.addAll(scoredIris);
            // Check whether there is a
            return results;
        } catch (Exception e) {
            LOGGER.error("Exception while executing SPARQL request. query=" + data.suggestionQuery, e);
            throw e;
        }
    }

    protected Collection<ScoredIRI> performClassSelection(SuggestionData data, Collection<String> positive,
            Collection<String> negative) {
        List<ScoredIRI> results = new ArrayList<>();
        performQuery(data, positive, negative, new ScoredIriQuerySolutionMapper("?class", classBlackList), results);
        return results;
    }

    protected ClassExpression prepareClassExpression(ClassExpression ce) {
        if (checker.containsDisjunction(ce)) {
            return preprocessor.preprocess(ce);
        } else {
            return ce;
        }
    }

    protected SuggestionData prepareForSuggestion(ClassExpression ce, int numPositives, int numNegatives) {
        SuggestionData data = new SuggestionData();
        data.maxPos = numPositives;
        data.maxNeg = numNegatives;
        if (checker.containsDisjunction(ce)) {
            ClassExpression normalForm = preprocessor.preprocess(ce);
            splitExpression(normalForm, data);
        } else {
            data.suggestionPart = ce;
        }
        return data;
    }

    protected void splitExpression(ClassExpression normalForm, SuggestionData data) {
        if (!(normalForm instanceof Junction) || ((Junction) normalForm).isConjunction()) {
            LOGGER.error("Something went wrong. Expected a disjunction at this point.");
        }
        // Split the expression into the two parts
        List<ClassExpression> baseExp = new ArrayList<ClassExpression>();
        List<ClassExpression> sugExp = new ArrayList<ClassExpression>();
        for (ClassExpression child : ((Junction) normalForm)) {
            if (child.accept(sugChecker)) {
                sugExp.add(child);
            } else {
                baseExp.add(child);
            }
        }
        if (sugExp.isEmpty()) {
            LOGGER.error("Got an expression without the suggestion marker. This is not expected at this point.");
            return;
        }
        if (!baseExp.isEmpty()) {
            if (baseExp.size() > 1) {
                data.basePart = new Junction(false, baseExp.toArray(ClassExpression[]::new));
            } else {
                data.basePart = baseExp.get(0);
            }
            // Add the negation to the suggestion children
            ClassExpression baseNegation = data.basePart.accept(new NegatingVisitor());
            for (int i = 0; i < sugExp.size(); ++i) {
                ClassExpression sugChild = sugExp.get(i);
                if ((sugChild instanceof Junction) && ((Junction) sugChild).isConjunction()) {
                    // We can add the negations directly to the existing conjunction
                    ((Junction) sugChild).getChildren().add(baseNegation);
                } else {
                    // We create a new conjunction
                    sugExp.set(i, new Junction(true, sugChild));
                }
            }
        }
        if (sugExp.size() > 1) {
            data.suggestionPart = prepareClassExpression(new Junction(false, sugExp.toArray(ClassExpression[]::new)));
        } else {
            data.suggestionPart = sugExp.get(0);
        }
    }

    @Override
    public Collection<ScoredIRI> suggestClass(Collection<String> positive, Collection<String> negative,
            ClassExpression context) {
        LOGGER.trace("Suggesting classes for {}", context);
        SuggestionData data = prepareForSuggestion(context, positive.size(), negative.size());
        if (logic.supportsComplexConceptNegation()) {
            data.suggestionQuery = generateClassQueryForGeneralNegation(positive, negative, data.suggestionPart);
        } else {
            data.suggestionQuery = generateClassQuery(positive, negative, data.suggestionPart);
        }
        return performClassSelection(data, positive, negative);
    }

    public Collection<ScoredIRI> suggestNegatedClass(Collection<String> positive, Collection<String> negative,
            ClassExpression context) {
        LOGGER.trace("Suggesting negated classes for {}", context);
        SuggestionData data = prepareForSuggestion(context, positive.size(), negative.size());
        data.suggestionQuery = generateNegatedClassQuery(positive, negative, data.suggestionPart);
        return performClassSelection(data, positive, negative);
    }

    /**
     * A class query that retrieves all classes that select at least one positive
     * examples within the given context together with the number of the selected
     * examples.
     * 
     * @param positive positive examples
     * @param negative negative examples
     * @param context  a class expression that marks a position with the
     *                 {@link Suggestor#CONTEXT_POSITION_MARKER} instance.
     * @return a SPARQL query that can be used to select the IRIs described above
     */
    protected String generateClassQuery(Collection<String> positive, Collection<String> negative,
            ClassExpression context) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ?class (MAX(?tp) AS ?posHits) (COUNT(DISTINCT ?neg) AS ?negHits) WHERE {\n");
        queryBuilder.append("    { SELECT ?class (COUNT(DISTINCT ?pos) AS ?tp) WHERE {\n        ");
        String valuesString = generateValuesStmt("?pos", positive.iterator());
        StringBuilder contextBuilder = new StringBuilder();
        SparqlBuildingVisitor visitor = new SparqlBuildingVisitor(contextBuilder, "?pos", valuesString,
                // createNotExistsFilter(context, "?pos")
                null, "?class a <" + OWL.Class.getURI() + "> .", v -> v + " a ?class .");
        context.accept(visitor);
        String contextString = contextBuilder.toString();
        queryBuilder.append(contextString);
        queryBuilder.append("    } GROUP BY ?class }\n");
        queryBuilder.append("    OPTIONAL {\n        ");
        // generate the negative context by replacing all VALUES statements and after
        // that all remaining ?pos occurrences.
        queryBuilder.append(contextString
                .replaceAll("VALUES[ ]+\\?pos[ ]+\\{[^}]*\\}", generateValuesStmt("?neg", negative.iterator()))
                .replaceAll("\\?pos ", "?neg "));
        queryBuilder.append("    }} GROUP BY ?class");
        // queryBuilder.append(" }} GROUP BY ?class\nORDER BY DESC(?posHits)
        // (?negHits)");
        return queryBuilder.toString();
    }

    /**
     * A class query that retrieves all classes that select at least one positive
     * example when the class is negated within the given context together with the
     * number of the selected examples.
     * 
     * @param positive positive examples
     * @param negative negative examples
     * @param context  a class expression that marks a position with the
     *                 {@link Suggestor#CONTEXT_POSITION_MARKER} instance.
     * @return a SPARQL query that can be used to select the IRIs described above
     */
    protected String generateNegatedClassQuery(Collection<String> positive, Collection<String> negative,
            ClassExpression context) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ?class (MAX(?tp) AS ?posHits) (MAX(?fp) AS ?negHits) WHERE {\n");
        queryBuilder.append("    { SELECT ?class (COUNT(DISTINCT ?pos) AS ?tp) (0 AS ?fp) WHERE {\n        ");
        String valuesString = generateValuesStmt("?pos", positive.iterator());
        StringBuilder contextBuilder = new StringBuilder();
        SparqlBuildingVisitor visitor = new SparqlBuildingVisitor(contextBuilder, "?pos", valuesString,
                // createNotExistsFilter(context, "?pos"),
                null, "?class a <" + OWL.Class.getURI() + "> .",
                v -> "?class a <" + OWL.Class.getURI() + "> .        \nFILTER NOT EXISTS { " + v + " a ?class . }");
        context.accept(visitor);
        String contextString = contextBuilder.toString();
        queryBuilder.append(contextString);
        queryBuilder.append("      } GROUP BY ?class\n");
        queryBuilder.append("    } UNION {\n");
        queryBuilder.append("      SELECT ?class (0 AS ?tp) (COUNT(DISTINCT ?neg) AS ?fp) WHERE {\n        ");
        // generate the negative context by replacing all VALUES statements and after
        // that all remaining ?pos occurrences.
        queryBuilder.append(contextString
                .replaceAll("VALUES[ ]+\\?pos[ ]+\\{[^}]*\\}", generateValuesStmt("?neg", negative.iterator()))
                .replaceAll("\\?pos ", "?neg "));
        queryBuilder.append("      } GROUP BY ?class\n    }\n");
        queryBuilder.append("} GROUP BY ?class");
        // queryBuilder.append("} GROUP BY ?class\nORDER BY DESC(?posHits) (?negHits)");
        return queryBuilder.toString();
    }

    /**
     * A class query that retrieves all classes that select any positive and
     * negative examples within the given context together with the number of the
     * selected examples. These counts can be used to extend the context with the
     * retrieved classes but also to create the negation of the given context and
     * the retrieved classes.
     * 
     * @param positive positive examples
     * @param negative negative examples
     * @param context  a class expression that marks a position with the
     *                 {@link Suggestor#CONTEXT_POSITION_MARKER} instance.
     * @return a SPARQL query that can be used to select the IRIs described above
     */
    protected String generateClassQueryForGeneralNegation(Collection<String> positive, Collection<String> negative,
            ClassExpression context) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ?class (MAX(?tp) AS ?posHits) (MAX(?fp) AS ?negHits) WHERE {\n");
        queryBuilder.append("    { SELECT ?class (COUNT(DISTINCT ?pos) AS ?tp) (0 AS ?fp) WHERE {\n        ");
        String valuesString = generateValuesStmt("?pos", positive.iterator());
        StringBuilder contextBuilder = new StringBuilder();
        SparqlBuildingVisitor visitor = new SparqlBuildingVisitor(contextBuilder, "?pos", valuesString,
                // createNotExistsFilter(context, "?pos"),
                null, "?class a <" + OWL.Class.getURI() + "> .", v -> v + " a ?class .");
        context.accept(visitor);
        String contextString = contextBuilder.toString();
        queryBuilder.append(contextString);
        queryBuilder.append("      } GROUP BY ?class\n");
        queryBuilder.append("    } UNION {\n");
        queryBuilder.append("      SELECT ?class (0 AS ?tp) (COUNT(DISTINCT ?neg) AS ?fp) WHERE {\n        ");
        // generate the negative context by replacing all VALUES statements and after
        // that all remaining ?pos occurrences.
        queryBuilder.append(contextString
                .replaceAll("VALUES[ ]+\\?pos[ ]+\\{[^}]*\\}", generateValuesStmt("?neg", negative.iterator()))
                .replaceAll("\\?pos ", "?neg "));
        queryBuilder.append("      } GROUP BY ?class\n    }\n");
        queryBuilder.append("} GROUP BY ?class");
        // queryBuilder.append("} GROUP BY ?class\nORDER BY DESC(?posHits) (?negHits)");
        return queryBuilder.toString();
    }

    @Override
    public Collection<ScoredIRI> suggestProperty(Collection<String> positive, Collection<String> negative,
            ClassExpression context) {
        LOGGER.trace("Suggesting properties for {}", context);
        SuggestionData data = prepareForSuggestion(context, positive.size(), negative.size());
        Collection<ScoredIRI> results = suggestProperty(data, positive, negative, false);
        if (logic.supportsInverseProperties()) {
            results.addAll(suggestProperty(data, positive, negative, true));
        }
        return results;
    }

    protected Collection<ScoredIRI> suggestProperty(SuggestionData data, Collection<String> positive,
            Collection<String> negative, boolean inverted) {
        List<ScoredIRI> results = new ArrayList<>();
        if (logic.supportsAtomicNegation()) {
            data.suggestionQuery = generatePropertyQuery(positive, negative, data.suggestionPart, inverted);
        } else {
            data.suggestionQuery = generatePropertyQueryWithoutNegation(positive, negative, data.suggestionPart,
                    inverted);
        }
        performQuery(data, positive, negative, new ScoredIriQuerySolutionMapper("?prop", propertyBlackList), results);
        return results;
    }

    protected String generatePropertyQuery(Collection<String> positive, Collection<String> negative,
            ClassExpression context, boolean inverted) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ?prop (MAX(?tp) AS ?posHits) (MAX(?fp) AS ?negHits) WHERE {\n");
        queryBuilder.append("    { SELECT ?prop (COUNT(DISTINCT ?pos) AS ?tp) (0 AS ?fp) WHERE {\n        ");
        String valuesString = generateValuesStmt("?pos", positive.iterator());
        StringBuilder contextBuilder = new StringBuilder();
        SparqlBuildingVisitor visitor = new SparqlBuildingVisitor(contextBuilder, "?pos", valuesString,
                // createNotExistsFilter(context, "?pos")
                null, "?prop a <" + RDF.Property.getURI() + "> .",
                inverted ? v -> " [] ?prop v ." : v -> v + " ?prop [] .");
        context.accept(visitor);
        String contextString = contextBuilder.toString();
        queryBuilder.append(contextString);
        queryBuilder.append("      } GROUP BY ?prop\n");
        queryBuilder.append("    } UNION {\n");
        queryBuilder.append("      SELECT ?prop (0 AS ?tp) (COUNT(DISTINCT ?neg) AS ?fp) WHERE {\n        ");
        // generate the negative context by replacing all VALUES statements and after
        // that all remaining ?pos occurrences.
        queryBuilder.append(contextString
                .replaceAll("VALUES[ ]+\\?pos[ ]+\\{[^}]*\\}", generateValuesStmt("?neg", negative.iterator()))
                .replaceAll("\\?pos ", "?neg "));
        queryBuilder.append("      } GROUP BY ?prop\n    }\n");
        queryBuilder.append("} GROUP BY ?prop");
        // queryBuilder.append("} GROUP BY ?prop\nORDER BY DESC(?posHits) (?negHits)");
        return queryBuilder.toString();
    }

    protected String generatePropertyQueryWithoutNegation(Collection<String> positive, Collection<String> negative,
            ClassExpression context, boolean inverted) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ?prop (MAX(?pc) AS ?posHits) (COUNT(DISTINCT ?negId) AS ?negHits) WHERE {\n");
        queryBuilder.append("    { SELECT ?prop (COUNT(DISTINCT ?pos) AS ?pc) WHERE {\n        ");
        String valuesString = generateValuesStmt("?pos", positive.iterator());
        StringBuilder contextBuilder = new StringBuilder();
        SparqlBuildingVisitor visitor = new SparqlBuildingVisitor(contextBuilder, "?pos", valuesString, null,
                "?prop a <" + RDF.Property.getURI() + "> .", inverted ? v -> " [] ?prop v ." : v -> v + " ?prop [] .");
        context.accept(visitor);
        String contextString = contextBuilder.toString();
        queryBuilder.append(contextString);
        queryBuilder.append("    } GROUP BY ?prop }\n");
        queryBuilder.append("    OPTIONAL {\n        ");
        // generate the negative context by replacing all VALUES statements and after
        // that all remaining ?pos occurrences.
        queryBuilder.append(contextString
                .replaceAll("VALUES[ ]+\\?pos[ ]+\\{[^}]*\\}", generateValuesStmt("?neg", negative.iterator()))
                .replaceAll("\\?pos ", "?neg "));
        queryBuilder.append("        BIND (CONCAT(STR(?neg),STR(?prop)) as ?negId)}\n");
        queryBuilder.append("    } GROUP BY ?prop");
        // queryBuilder.append(" } GROUP BY ?prop\nORDER BY DESC(?posHits) (?negHits)");
        return queryBuilder.toString();
    }

    public void addToClassBlackList(String classIRI) {
        this.classBlackList.add(classIRI);
    }

    public void addToClassBlackList(Collection<String> classIRIs) {
        this.classBlackList.addAll(classIRIs);
    }

    public void addToPropertyBlackList(String propertyIRI) {
        this.propertyBlackList.add(propertyIRI);
    }

    public void addToPropertyBlackList(Collection<String> propertyIRIs) {
        this.propertyBlackList.addAll(propertyIRIs);
    }

    @Override
    public SelectionScores scoreExpression(ClassExpression expression, Collection<String> positive,
            Collection<String> negative) {
        LOGGER.trace("Scoring expression {}", expression);
        return scorePreparedExpression(prepareClassExpression(expression), positive, negative);
    }

    protected SelectionScores scorePreparedExpression(ClassExpression prepared, Collection<String> positive,
            Collection<String> negative) {
        String query = generateScoreQueryForGeneralNegation(positive, negative, prepared);
        LOGGER.trace("Sending query {}", query);
        // Create the query execution with try-catch to ensure that it will be closed
        try (QueryExecution qe = queryExecFactory.createQueryExecution(query);) {
            ResultSet result = qe.execSelect();
            if (result.hasNext()) {
                QuerySolution s = result.next();
                return new SelectionScores(s.getLiteral("posHits").getInt(), s.getLiteral("negHits").getInt());
            }
        } catch (Exception e) {
            LOGGER.error("Exception while executing SPARQL request. query=" + query, e);
            throw e;
        }
        LOGGER.warn("Got an empty result fo the expression {}. Returning a zero score.", prepared);
        return new SelectionScores(0, 0);
    }

    protected String generateScoreQueryForGeneralNegation(Collection<String> positive, Collection<String> negative,
            ClassExpression expression) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ?posHits ?negHits WHERE {\n");
        queryBuilder.append("    { SELECT (COUNT(DISTINCT ?pos) AS ?posHits) WHERE {\n        ");
        String valuesString = generateValuesStmt("?pos", positive.iterator());
        StringBuilder contextBuilder = new StringBuilder();
        SparqlBuildingVisitor visitor = new SparqlBuildingVisitor(contextBuilder, "?pos", valuesString, null, null,
                v -> "");
        expression.accept(visitor);
        String contextString = contextBuilder.toString();
        queryBuilder.append(contextString);
        queryBuilder.append("    }}\n");
        queryBuilder.append("    { SELECT (COUNT(DISTINCT ?neg) AS ?negHits) WHERE {\n        ");
        // generate the negative context by replacing all VALUES statements and after
        // that all remaining ?pos occurrences.
        queryBuilder.append(contextString
                .replaceAll("VALUES[ ]+\\?pos[ ]+\\{[^}]*\\}", generateValuesStmt("?neg", negative.iterator()))
                .replaceAll("\\?pos ", "?neg "));
        queryBuilder.append("    }}\n");
        queryBuilder.append("}");
        return queryBuilder.toString();
    }

    @Override
    public Set<String> retrieveInstances(ClassExpression expression, Collection<String> positive,
            Collection<String> negative) {
        Set<String> instances = new HashSet<>();
        LOGGER.trace("Scoring expression {}", expression);
        ClassExpression prepared = prepareClassExpression(expression);
        String query = generateSelectQueryForGeneralNegation(positive, negative, prepared);
        LOGGER.trace("Sending query {}", query);
        // Create the query execution with try-catch to ensure that it will be closed
        try (QueryExecution qe = queryExecFactory.createQueryExecution(query);) {
            ResultSet result = qe.execSelect();
            while (result.hasNext()) {
                instances.add(result.next().getResource("instance").getURI());
            }
            return instances;
        } catch (Exception e) {
            LOGGER.error("Exception while executing SPARQL request. query=" + query, e);
            throw e;
        }
    }

    protected String generateSelectQueryForGeneralNegation(Collection<String> positive, Collection<String> negative,
            ClassExpression expression) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ?instance WHERE {\n");
        String valuesString = generateValuesStmt("?instance",
                Iterators.concat(positive.iterator(), negative.iterator()));
        StringBuilder contextBuilder = new StringBuilder();
        SparqlBuildingVisitor visitor = new SparqlBuildingVisitor(contextBuilder, "?instance", valuesString, null, null,
                v -> "");
        expression.accept(visitor);
        String contextString = contextBuilder.toString();
        queryBuilder.append(contextString);
        queryBuilder.append("}");
        return queryBuilder.toString();
    }

    protected String generateValuesStmt(String variable, Iterator<String> iterator) {
        StringBuilder valuesBuilder = new StringBuilder();
        appendValues(valuesBuilder, variable, iterator);
        return valuesBuilder.toString();
    }

    protected void appendValues(StringBuilder queryBuilder, String variable, Iterator<String> iterator) {
        queryBuilder.append("VALUES ");
        queryBuilder.append(variable);
        queryBuilder.append(" {");
        String value;
        while (iterator.hasNext()) {
            value = iterator.next();
            queryBuilder.append(" <");
            queryBuilder.append(value);
            queryBuilder.append('>');
        }
        queryBuilder.append(" }\n");
    }

    @Deprecated
    protected String createNotExistsFilter(ClassExpression context, String instanceVariable) {
        String filter = null;
        if (context instanceof Junction) {
            Junction junction = (Junction) context;
            if (!junction.isConjunction()) {
                // 1. Remove the part of the context that contains the marking
                ClassExpression reducedExpression = context.accept(new SubExpressionDeleter());
                List<ClassExpression> expressions = new ArrayList<>();
                addUnionSubExpressionToFilter(reducedExpression, expressions);
                // 2. Use the remaining part as filter
                StringBuilder filterBuilder = new StringBuilder();
                for (ClassExpression expression : expressions) {
                    SparqlBuildingVisitor visitor = new SparqlBuildingVisitor(filterBuilder, "?pos", null, null, null,
                            null);
                    visitor.setIntermediateVariableName("?y");
                    filterBuilder.append("FILTER NOT EXISTS { \n        ");
                    // We have to add a dummy triple
                    filterBuilder.append(instanceVariable);
                    filterBuilder.append(" a <http://www.w3.org/2002/07/owl#NamedIndividual> . \n");
                    expression.accept(visitor);
                    filterBuilder.append(" }\n");
                }
//                ClassExpression filterExpression = new Junction(false, expressions.toArray(ClassExpression[]::new));
//                filterExpression =  filterExpression.accept(new NegatingVisitor());
//                SparqlBuildingVisitor visitor = new SparqlBuildingVisitor(filterBuilder, "?pos", null, null, null);
//                visitor.setIntermediateVariableName("?y");
//                filterExpression.accept(visitor);
                filter = filterBuilder.toString();
            }
        }
        return filter;
    }

    protected void addUnionSubExpressionToFilter(ClassExpression expression, List<ClassExpression> expressions) {
        if (expression instanceof Junction) {
            Junction junction = (Junction) expression;
            if (!junction.isConjunction()) {
                for (ClassExpression child : junction.getChildren()) {
                    addUnionSubExpressionToFilter(child, expressions);
                }
                return;
            }
        }
        expressions.add(expression);
    }

    public static SparqlBasedSuggestor create(String endpoint, DescriptionLogic logic) {
        HttpClient client = HttpClient.newHttpClient();
        QueryExecutionFactory queryExecFactory = new QueryExecutionFactoryHttp(endpoint, new DatasetDescription(),
                client);
        return new SparqlBasedSuggestor(queryExecFactory, logic);
    }

}
