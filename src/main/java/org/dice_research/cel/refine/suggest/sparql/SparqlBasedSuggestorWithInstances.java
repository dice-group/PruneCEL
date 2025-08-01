package org.dice_research.cel.refine.suggest.sparql;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jenax.arq.connection.core.QueryExecutionFactory;
import org.aksw.jenax.stmt.parser.query.SparqlQueryParser;
import org.aksw.jenax.stmt.parser.query.SparqlQueryParserImpl;
import org.apache.commons.io.FileUtils;
import org.apache.jena.ext.com.google.common.collect.Iterators;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.DatasetDescription;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.cel.DescriptionLogic;
import org.dice_research.cel.expression.ClassExpression;
import org.dice_research.cel.expression.Junction;
import org.dice_research.cel.expression.NegatingVisitor;
import org.dice_research.cel.refine.instances.ExtendedSuggestorWithInstances;
import org.dice_research.cel.refine.instances.ScoredIRIWithInstances;
import org.dice_research.cel.refine.instances.SelectionScoresWithInstances;
import org.dice_research.cel.refine.suggest.Suggestor;
import org.dice_research.cel.sparql.InstanceRetriever;
import org.dice_research.cel.sparql.MultiNotExistFilterFixingVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javolution.util.FastBitSet;

public class SparqlBasedSuggestorWithInstances
        implements ExtendedSuggestorWithInstances, InstanceRetriever, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SparqlBasedSuggestorWithInstances.class);

    private static final String CLASS_VAR_NAME = "class";
    private static final String CLASS_VAR = "?" + CLASS_VAR_NAME;
    private static final String CLASS_TYPE_STMT = CLASS_VAR + " a <" + OWL.Class.getURI() + "> .";
    private static final String EXAMPLE_VAR_NAME = "example";
    private static final String EXAMPLE_VAR = "?" + EXAMPLE_VAR_NAME;
    private static final String PROP_VAR_NAME = "prop";
    private static final String PROP_VAR = "?" + PROP_VAR_NAME;
    private static final String PROP_TYPE_STMT = PROP_VAR + " a <" + RDF.Property.getURI() + "> .";

    protected QueryExecutionFactory queryExecFactory;
    protected Set<String> classBlackList = new HashSet<String>();
    protected Set<String> propertyBlackList = new HashSet<String>();
    protected DescriptionLogic logic;
    protected DisjunctionCheckingVisitor checker = new DisjunctionCheckingVisitor();
    protected SuggestionCheckingVisitor sugChecker = new SuggestionCheckingVisitor();
    protected ExpressionPreProcessor preprocessor = new ExpressionPreProcessor();
    protected SparqlQueryParser queryParser = new SparqlQueryParserImpl();
    protected MultiNotExistFilterFixingVisitor classQueryVisitor = new MultiNotExistFilterFixingVisitor(CLASS_VAR,
            OWL.Class.getURI());
    protected MultiNotExistFilterFixingVisitor propertyQueryVisitor = new MultiNotExistFilterFixingVisitor(PROP_VAR,
            RDF.Property.getURI());

    public SparqlBasedSuggestorWithInstances(QueryExecutionFactory queryExecFactory, DescriptionLogic logic) {
        this.queryExecFactory = queryExecFactory;
        this.logic = logic;
    }

    @Override
    public void close() throws Exception {
        if (queryExecFactory != null) {
            queryExecFactory.close();
        }
    }

    protected Collection<ScoredIRIWithInstances> performQuery(SuggestionDataWithInstances data,
            Collection<String> positive, Collection<String> negative, Map<String, Integer> positiveMaps,
            Map<String, Integer> negativeMaps, Set<String> blacklist, String iriVariable,
            Collection<ScoredIRIWithInstances> results) {
        // Execute base query
        if (data.basePart != null) {
            data.setBaseCounts(scorePreparedExpression(data.basePart, positive, negative, positiveMaps, negativeMaps));
        }

        LOGGER.trace("Sending query {}", data.suggestionQuery);
        // Create the query execution with try-catch to ensure that it will be closed
        try (QueryExecution qe = queryExecFactory.createQueryExecution(data.suggestionQuery);) {
            ResultSet result = qe.execSelect();
            Collection<ScoredIRIWithInstances> scoredIris = collectIris(result, iriVariable, positiveMaps, negativeMaps,
                    blacklist);
            result.close();
            data.addBaseScore(scoredIris);
            Optional<ScoredIRIWithInstances> faultyResult = scoredIris.stream().filter(
                    s -> s.posCount < 0 || s.posCount > data.maxPos || s.negCount < 0 || s.negCount > data.maxNeg)
                    .findFirst();
            if (faultyResult.isPresent()) {
                LOGGER.error("Got a faulty count: #positives={}, #negatives={}, scoredIRI={}", data.maxPos, data.maxNeg,
                        faultyResult);
            }
            results.addAll(scoredIris);
            // Check whether there is a
            return results;
        } catch (Exception e) {
            LOGGER.error(
                    "Exception while executing SPARQL request. Query is printed to 'error-query.txt'. Returning empty list.",
                    e);
            try {
                FileUtils.writeStringToFile(new File("error-query.txt"), data.suggestionQuery.toString(),
                        StandardCharsets.UTF_8);
            } catch (IOException e1) {
                LOGGER.error("Couldn't print SPARQL query to file. Query = " + data.suggestionQuery.toString(), e1);
            }
            return Collections.emptyList();
        }
    }

    protected Collection<ScoredIRIWithInstances> collectIris(ResultSet result, String iriVariable,
            Map<String, Integer> positiveMaps, Map<String, Integer> negativeMaps, Set<String> blacklist) {
        Map<String, FastBitSet[]> scoredIris = new HashMap<>();
        while (result.hasNext()) {
            processSolution(result.next(), iriVariable, positiveMaps, negativeMaps, blacklist, scoredIris);
        }
        return scoredIris.entrySet().stream()
                .map(e -> new ScoredIRIWithInstances(e.getKey(), e.getValue()[0], e.getValue()[1])).toList();
    }

    protected void processSolution(QuerySolution solution, String iriVariable, Map<String, Integer> positiveMaps,
            Map<String, Integer> negativeMaps, Set<String> blacklist, Map<String, FastBitSet[]> scoredIris) {
        Resource selResource = solution.getResource(iriVariable);
        if ((selResource == null) || (!selResource.isURIResource())) {
            return;
        }
        Resource exResource = solution.getResource(EXAMPLE_VAR_NAME);
        if ((exResource == null) || (!exResource.isURIResource())) {
            return;
        }
        String iri = selResource.getURI();
        if (blacklist.contains(iri)) {
            return;
        }
        FastBitSet[] sets;
        if (scoredIris.containsKey(iri)) {
            sets = scoredIris.get(iri);
        } else {
            sets = new FastBitSet[] { FastBitSet.newInstance(), FastBitSet.newInstance() };
            scoredIris.put(iri, sets);
        }
        updateBitSets(sets[0], sets[1], positiveMaps, negativeMaps, exResource.getURI());
    }

    protected void updateBitSets(FastBitSet positive, FastBitSet negative, Map<String, Integer> positiveMaps,
            Map<String, Integer> negativeMaps, String exampleIri) {
        if (exampleIri == null) {
            // nothing to do
            return;
        }
        if (positiveMaps.containsKey(exampleIri)) {
            positive.set(positiveMaps.get(exampleIri));
        } else if (negativeMaps.containsKey(exampleIri)) {
            negative.set(negativeMaps.get(exampleIri));
        } else {
            LOGGER.error("Got an IRI ({}) that is neither a positive nor a negative example!", exampleIri);
        }
    }

    protected Collection<ScoredIRIWithInstances> performClassSelection(SuggestionDataWithInstances data,
            Collection<String> positive, Collection<String> negative, Map<String, Integer> positiveMaps,
            Map<String, Integer> negativeMaps) {
        List<ScoredIRIWithInstances> results = new ArrayList<>();
        performQuery(data, positive, negative, positiveMaps, negativeMaps, classBlackList, CLASS_VAR, results);
        return results;
    }

    protected ClassExpression prepareClassExpression(ClassExpression ce) {
        if (checker.containsDisjunction(ce)) {
            return preprocessor.preprocess(ce);
        } else {
            return ce;
        }
    }

    protected SuggestionDataWithInstances prepareForSuggestion(ClassExpression ce, int numPositives, int numNegatives) {
        SuggestionDataWithInstances data = new SuggestionDataWithInstances();
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

    protected void splitExpression(ClassExpression normalForm, SuggestionDataWithInstances data) {
        if (!(normalForm instanceof Junction) || ((Junction) normalForm).isConjunction()) {
            // There is a conjunction but the preprocessor didn't move it up to the root.
            // Hence, we should ignore it and the given normalForm is the suggestionPart.
            data.suggestionPart = normalForm;
            return;
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
//            We won't add the negation of the base part here. Instead, we add them as filters later on. That removes the need to negate them twice and also reduces the amount of potential errors.
            // Add the negation to the suggestion children
            ClassExpression baseNegation = data.basePart.accept(new NegatingVisitor());
            Junction childJunction;
            for (int i = 0; i < sugExp.size(); ++i) {
                ClassExpression sugChild = sugExp.get(i);
                if ((sugChild instanceof Junction) && ((Junction) sugChild).isConjunction()) {
                    // We can add the negations directly to the existing conjunction
                    childJunction = ((Junction) sugChild);
                } else {
                    // We create a new conjunction
                    childJunction = new Junction(true, sugChild);
                }
                if ((baseNegation instanceof Junction) && ((Junction) baseNegation).isConjunction()) {
                    childJunction.getChildren().addAll(((Junction) baseNegation).getChildren());
                } else {
                    childJunction.getChildren().add(baseNegation);
                }
                sugExp.set(i, childJunction);
            }
        }
        if (sugExp.size() > 1) {
            data.suggestionPart = prepareClassExpression(new Junction(false, sugExp.toArray(ClassExpression[]::new)));
//            data.suggestionPart = new Junction(false, sugExp.toArray(ClassExpression[]::new));
        } else {
            data.suggestionPart = prepareClassExpression(sugExp.get(0));
//            data.suggestionPart = sugExp.get(0);
        }
    }

    @Override
    public Collection<ScoredIRIWithInstances> suggestClassWithInstances(Collection<String> positive,
            Collection<String> negative, Map<String, Integer> positiveMaps, Map<String, Integer> negativeMaps,
            ClassExpression context) {
        LOGGER.trace("Suggesting classes for {}", context);
        SuggestionDataWithInstances data = prepareForSuggestion(context, positive.size(), negative.size());
        String suggestionQuery;
        suggestionQuery = generateClassQuery(positive, negative, data.suggestionPart, null);
        data.suggestionQuery = prepareQuery(suggestionQuery, classQueryVisitor);
        return performClassSelection(data, positive, negative, positiveMaps, negativeMaps);
    }

    protected Query prepareQuery(String queryString, MultiNotExistFilterFixingVisitor visitor) {
        Query query = queryParser.apply(queryString);
        visitor.fixQuery(query);
        return query;
    }

    public Collection<ScoredIRIWithInstances> suggestNegatedClassWithInstances(Collection<String> positive,
            Collection<String> negative, Map<String, Integer> positiveMaps, Map<String, Integer> negativeMaps,
            ClassExpression context) {
        LOGGER.trace("Suggesting negated classes for {}", context);
        SuggestionDataWithInstances data = prepareForSuggestion(context, positive.size(), negative.size());
        data.suggestionQuery = prepareQuery(generateNegatedClassQuery(positive, negative, data.suggestionPart, null),
                classQueryVisitor);
        return performClassSelection(data, positive, negative, positiveMaps, negativeMaps);
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
            ClassExpression context, ClassExpression filterExpression) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ");
        queryBuilder.append(CLASS_VAR);
        queryBuilder.append(' ');
        queryBuilder.append(EXAMPLE_VAR);
        queryBuilder.append(" WHERE {\n    ");
        String valuesString = generateValuesStmt(EXAMPLE_VAR,
                Iterators.concat(positive.iterator(), negative.iterator()));
        StringBuilder contextBuilder = new StringBuilder();
        SparqlBuildingVisitor visitor = new SparqlBuildingVisitor(contextBuilder, EXAMPLE_VAR, valuesString,
                createNotExistsFilter(filterExpression, EXAMPLE_VAR), CLASS_TYPE_STMT,
                v -> v + " a " + CLASS_VAR + " .");
        context.accept(visitor);
        String contextString = contextBuilder.toString();
        queryBuilder.append(contextString);
        queryBuilder.append('}');
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
            ClassExpression context, ClassExpression filterExpression) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ");
        queryBuilder.append(CLASS_VAR);
        queryBuilder.append(' ');
        queryBuilder.append(EXAMPLE_VAR);
        queryBuilder.append(" WHERE {\n    ");
        String valuesString = generateValuesStmt(EXAMPLE_VAR,
                Iterators.concat(positive.iterator(), negative.iterator()));
        StringBuilder contextBuilder = new StringBuilder();
        SparqlBuildingVisitor visitor = new SparqlBuildingVisitor(contextBuilder, EXAMPLE_VAR, valuesString,
                createNotExistsFilter(filterExpression, EXAMPLE_VAR), CLASS_TYPE_STMT,
                v -> CLASS_TYPE_STMT + "\n        FILTER NOT EXISTS { " + v + " a " + CLASS_VAR + " . }");
        context.accept(visitor);
        String contextString = contextBuilder.toString();
        queryBuilder.append(contextString);
        queryBuilder.append('}');
        return queryBuilder.toString();
    }

    @Override
    public Collection<ScoredIRIWithInstances> suggestPropertyWithInstances(Collection<String> positive,
            Collection<String> negative, Map<String, Integer> positiveMaps, Map<String, Integer> negativeMaps,
            ClassExpression context) {
        LOGGER.trace("Suggesting properties for {}", context);
        SuggestionDataWithInstances data = prepareForSuggestion(context, positive.size(), negative.size());
        Collection<ScoredIRIWithInstances> results = suggestProperty(data, positive, negative, positiveMaps,
                negativeMaps, false);
        if (logic.supportsInverseProperties()) {
            results.addAll(suggestProperty(data, positive, negative, positiveMaps, negativeMaps, true));
        }
        return results;
    }

    protected Collection<ScoredIRIWithInstances> suggestProperty(SuggestionDataWithInstances data,
            Collection<String> positive, Collection<String> negative, Map<String, Integer> positiveMaps,
            Map<String, Integer> negativeMaps, boolean inverted) {
        Collection<ScoredIRIWithInstances> results = new ArrayList<>();
        String suggestionQuery = generatePropertyQuery(positive, negative, data.suggestionPart, null, inverted);
        data.suggestionQuery = prepareQuery(suggestionQuery, propertyQueryVisitor);
        performQuery(data, positive, negative, positiveMaps, negativeMaps, propertyBlackList, PROP_VAR, results);
        return results;
    }

    protected String generatePropertyQuery(Collection<String> positive, Collection<String> negative,
            ClassExpression context, ClassExpression filterExpression, boolean inverted) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ");
        queryBuilder.append(PROP_VAR);
        queryBuilder.append(' ');
        queryBuilder.append(EXAMPLE_VAR);
        queryBuilder.append(" WHERE {\n    ");
        String valuesString = generateValuesStmt(EXAMPLE_VAR,
                Iterators.concat(positive.iterator(), negative.iterator()));
        StringBuilder contextBuilder = new StringBuilder();
        SparqlBuildingVisitor visitor = new SparqlBuildingVisitor(contextBuilder, EXAMPLE_VAR, valuesString,
                createNotExistsFilter(filterExpression, PROP_VAR), PROP_TYPE_STMT,
                inverted ? v -> new StringBuilder().append(" [] ").append(PROP_VAR).append(" ").append(v).append(" .")
                        .toString()
                        : v -> new StringBuilder().append(v).append(" ").append(PROP_VAR).append(" [] .").toString());
        context.accept(visitor);
        String contextString = contextBuilder.toString();
        queryBuilder.append(contextString);
        queryBuilder.append('}');
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
    public SelectionScoresWithInstances scoreExpressionWithInstances(ClassExpression expression,
            Collection<String> positive, Collection<String> negative, Map<String, Integer> positiveMaps,
            Map<String, Integer> negativeMaps) {
        LOGGER.trace("Scoring expression {}", expression);
        return scorePreparedExpression(prepareClassExpression(expression), positive, negative, positiveMaps,
                negativeMaps);
    }

    protected SelectionScoresWithInstances scorePreparedExpression(ClassExpression prepared,
            Collection<String> positive, Collection<String> negative, Map<String, Integer> positiveMaps,
            Map<String, Integer> negativeMaps) {
        String query = generateScoreQueryForGeneralNegation(positive, negative, prepared);
        LOGGER.trace("Sending query {}", query);
        FastBitSet selPositive = FastBitSet.newInstance();
        FastBitSet selNegative = FastBitSet.newInstance();
        // Create the query execution with try-catch to ensure that it will be closed
        try (QueryExecution qe = queryExecFactory.createQueryExecution(query);) {
            ResultSet result = qe.execSelect();
            while (result.hasNext()) {
                Resource exResource = result.next().getResource(EXAMPLE_VAR_NAME);
                if ((exResource != null) && (exResource.isURIResource())) {
                    updateBitSets(selPositive, selNegative, positiveMaps, negativeMaps, exResource.getURI());
                }
            }
            result.close();
        } catch (Exception e) {
            LOGGER.error("Exception while executing SPARQL request. query=" + query, e);
            throw e;
        }
        return new SelectionScoresWithInstances(selPositive, selNegative);
    }

    protected String generateScoreQueryForGeneralNegation(Collection<String> positive, Collection<String> negative,
            ClassExpression expression) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ");
        queryBuilder.append(EXAMPLE_VAR);
        queryBuilder.append(" WHERE {\n    ");
        String valuesString = generateValuesStmt(EXAMPLE_VAR,
                Iterators.concat(positive.iterator(), negative.iterator()));
        StringBuilder contextBuilder = new StringBuilder();
        SparqlBuildingVisitor visitor = new SparqlBuildingVisitor(contextBuilder, EXAMPLE_VAR, valuesString, null, null,
                v -> "");
        expression.accept(visitor);
        String contextString = contextBuilder.toString();
        queryBuilder.append(contextString);
        queryBuilder.append("}");
        return queryBuilder.toString();
    }

    @Override
    public Set<String> retrieveInstances(ClassExpression expression, Collection<String> positive,
            Collection<String> negative) {
        Set<String> instances = new HashSet<>();
        LOGGER.trace("Retrieving instances of expression {}", expression);
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
        queryBuilder.append("SELECT ");
        queryBuilder.append(EXAMPLE_VAR);
        queryBuilder.append(" WHERE {\n    ");
        String valuesString = generateValuesStmt(EXAMPLE_VAR,
                Iterators.concat(positive.iterator(), negative.iterator()));
        StringBuilder contextBuilder = new StringBuilder();
        SparqlBuildingVisitor visitor = new SparqlBuildingVisitor(contextBuilder, EXAMPLE_VAR, valuesString, null, null,
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

    protected String createNotExistsFilter(ClassExpression filterExpression, String instanceVariable) {
        String filter = null;
        if (filterExpression == null) {
            return null;
        }
        StringBuilder filterBuilder = new StringBuilder();
        SparqlBuildingVisitor visitor = new SparqlBuildingVisitor(filterBuilder, instanceVariable, null, null, null,
                null);
        visitor.setIntermediateVariableName("?y");
        visitor.visitNotExistsFilter(filterExpression);
        filter = filterBuilder.toString();
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

    public static SparqlBasedSuggestorWithInstances create(String endpoint,
            DescriptionLogic logic/* , boolean useCache */) {
        HttpClient client = HttpClient.newHttpClient();
        QueryExecutionFactory queryExecFactory = new QueryExecutionFactoryHttp(endpoint, new DatasetDescription(),
                client);
//        if (useCache) {
//            return new CachingSparqlBasedSuggestor(queryExecFactory, logic);
//        } else {
        return new SparqlBasedSuggestorWithInstances(queryExecFactory, logic);
//        }
    }

}
