package org.dice_research.cel.refine.suggest;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jenax.arq.connection.core.QueryExecutionFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.core.DatasetDescription;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.dice_research.cel.DescriptionLogic;
import org.dice_research.cel.expression.ClassExpression;
import org.dice_research.cel.expression.ClassExpressionVisitingCreator;
import org.dice_research.cel.expression.ClassExpressionVisitor;
import org.dice_research.cel.expression.Junction;
import org.dice_research.cel.expression.NamedClass;
import org.dice_research.cel.expression.NegatingVisitor;
import org.dice_research.cel.expression.SimpleQuantifiedRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparqlBasedSuggestor implements ExtendedSuggestor, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SparqlBasedSuggestor.class);

    protected QueryExecutionFactory queryExecFactory;
    protected Set<String> classBlackList = new HashSet<String>();
    protected Set<String> propertyBlackList = new HashSet<String>();
    protected DescriptionLogic logic;
    protected DisjunctionCheckingVisitor checker = new DisjunctionCheckingVisitor();
    protected ExpressionPreProcessor preprocessor = new ExpressionPreProcessor();

    public SparqlBasedSuggestor(String endpoint, DescriptionLogic logic) throws IOException {
        this(endpoint, logic, false);
    }

    public SparqlBasedSuggestor(String endpoint, DescriptionLogic logic, boolean useInverseProperties)
            throws IOException {
        HttpClient client = HttpClient.newHttpClient();
        this.queryExecFactory = new QueryExecutionFactoryHttp(endpoint, new DatasetDescription(), client);
        this.logic = logic;
    }

    @Override
    public void close() throws Exception {
        queryExecFactory.close();
    }

    protected Collection<ScoredIRI> performQuery(String query, Function<QuerySolution, ScoredIRI> transformation,
            Collection<ScoredIRI> results) {
        LOGGER.trace("Sending query {}", query);
        // Create the query execution with try-catch to ensure that it will be closed
        try (QueryExecution qe = queryExecFactory.createQueryExecution(query);) {
            ResultSet result = qe.execSelect();
            ScoredIRI iri;
            while (result.hasNext()) {
                iri = transformation.apply(result.next());
                if (iri != null) {
                    results.add(iri);
                }
            }
            return results;
        } catch (Exception e) {
            LOGGER.error("Exception while executing SPARQL request. query=" + query, e);
            throw e;
        }
    }

    protected Collection<ScoredIRI> performClassSelection(String query) {
        List<ScoredIRI> results = new ArrayList<>();
        performQuery(query, new Function<QuerySolution, ScoredIRI>() {
            @Override
            public ScoredIRI apply(QuerySolution s) {
                if (s.contains("?class")) {
                    String iri = s.getResource("?class").getURI();
                    if (!classBlackList.contains(iri)) {
                        return new ScoredIRI(s.getResource("?class").getURI(), s.getLiteral("posHits").getInt(),
                                s.getLiteral("negHits").getInt());
                    }
                }
                return null;
            }
        }, results);
        return results;
    }

    protected ClassExpression prepareClassExpression(ClassExpression ce) {
        if (checker.containsDisjunction(ce)) {
            return preprocessor.preprocess(ce);
        } else {
            return ce;
        }
    }

    @Override
    public Collection<ScoredIRI> suggestClass(Collection<String> positive, Collection<String> negative,
            ClassExpression context) {
        String query;
        LOGGER.trace("Suggesting classes for {}", context);
        ClassExpression prepared = prepareClassExpression(context);
        if (logic.supportsComplexConceptNegation()) {
            query = generateClassQueryForGeneralNegation(positive, negative, prepared);
        } else {
            query = generateClassQuery(positive, negative, prepared);
        }
        return performClassSelection(query);
    }

    public Collection<ScoredIRI> suggestNegatedClass(Collection<String> positive, Collection<String> negative,
            ClassExpression context) {
        LOGGER.trace("Suggesting negated classes for {}", context);
        ClassExpression prepared = prepareClassExpression(context);
        return performClassSelection(generateNegatedClassQuery(positive, negative, prepared));
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
        String valuesString = generateValuesStmt("?pos", positive);
        StringBuilder contextBuilder = new StringBuilder();
        SparqlBuildingVisitor visitor = new SparqlBuildingVisitor(contextBuilder, "?pos", valuesString,
                v -> v + " a ?class .");
        context.accept(visitor);
        String contextString = contextBuilder.toString();
        queryBuilder.append(contextString);
        queryBuilder.append("    } GROUP BY ?class }\n");
        queryBuilder.append("    OPTIONAL {\n        ");
        // generate the negative context by replacing all VALUES statements and after
        // that all remaining ?pos occurrences.
        queryBuilder.append(
                contextString.replaceAll("VALUES[ ]+\\?pos[ ]+\\{[^}]*\\}", generateValuesStmt("?neg", negative))
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
        String valuesString = generateValuesStmt("?pos", positive);
        StringBuilder contextBuilder = new StringBuilder();
        SparqlBuildingVisitor visitor = new SparqlBuildingVisitor(contextBuilder, "?pos", valuesString,
                v -> "?class a <" + OWL.Class.getURI() + "> .        \nFILTER NOT EXISTS { " + v + " a ?class . }");
        context.accept(visitor);
        String contextString = contextBuilder.toString();
        queryBuilder.append(contextString);
        queryBuilder.append("      } GROUP BY ?class\n");
        queryBuilder.append("    } UNION {\n");
        queryBuilder.append("      SELECT ?class (0 AS ?tp) (COUNT(DISTINCT ?neg) AS ?fp) WHERE {\n        ");
        // generate the negative context by replacing all VALUES statements and after
        // that all remaining ?pos occurrences.
        queryBuilder.append(
                contextString.replaceAll("VALUES[ ]+\\?pos[ ]+\\{[^}]*\\}", generateValuesStmt("?neg", negative))
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
        String valuesString = generateValuesStmt("?pos", positive);
        StringBuilder contextBuilder = new StringBuilder();
        SparqlBuildingVisitor visitor = new SparqlBuildingVisitor(contextBuilder, "?pos", valuesString,
                v -> v + " a ?class .");
        context.accept(visitor);
        String contextString = contextBuilder.toString();
        queryBuilder.append(contextString);
        queryBuilder.append("      } GROUP BY ?class\n");
        queryBuilder.append("    } UNION {\n");
        queryBuilder.append("      SELECT ?class (0 AS ?tp) (COUNT(DISTINCT ?neg) AS ?fp) WHERE {\n        ");
        // generate the negative context by replacing all VALUES statements and after
        // that all remaining ?pos occurrences.
        queryBuilder.append(
                contextString.replaceAll("VALUES[ ]+\\?pos[ ]+\\{[^}]*\\}", generateValuesStmt("?neg", negative))
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
        ClassExpression prepared = prepareClassExpression(context);
        Collection<ScoredIRI> results = suggestProperty(positive, negative, prepared, false);
        if (logic.supportsInverseProperties()) {
            results.addAll(suggestProperty(positive, negative, prepared, true));
        }
        return results;
    }

    protected Collection<ScoredIRI> suggestProperty(Collection<String> positive, Collection<String> negative,
            ClassExpression context, boolean inverted) {
        List<ScoredIRI> results = new ArrayList<>();
        String query;
        if (logic.supportsAtomicNegation()) {
            query = generatePropertyQuery(positive, negative, context, inverted);
        } else {
            query = generatePropertyQueryWithoutNegation(positive, negative, context, inverted);
        }
        LOGGER.trace("Sending query {}", query);
        // Create the query execution with try-catch to ensure that it will be closed
        try (QueryExecution qe = queryExecFactory.createQueryExecution(query);) {
            ResultSet result = qe.execSelect();
            String iri;
            while (result.hasNext()) {
                QuerySolution s = result.next();
                if (s.contains("?prop")) {
                    iri = s.getResource("?prop").getURI();
                    if (!propertyBlackList.contains(iri)) {
                        results.add(new ScoredIRI(s.getResource("?prop").getURI(), s.getLiteral("posHits").getInt(),
                                s.getLiteral("negHits").getInt(), inverted));
                    }
                }
            }
            return results;
        } catch (Exception e) {
            LOGGER.error("Exception while executing SPARQL request. query=" + query, e);
            throw e;
        }
    }

    protected String generatePropertyQuery(Collection<String> positive, Collection<String> negative,
            ClassExpression context, boolean inverted) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ?prop (MAX(?tp) AS ?posHits) (MAX(?fp) AS ?negHits) WHERE {\n");
        queryBuilder.append("    { SELECT ?prop (COUNT(DISTINCT ?pos) AS ?tp) (0 AS ?fp) WHERE {\n        ");
        String valuesString = generateValuesStmt("?pos", positive);
        StringBuilder contextBuilder = new StringBuilder();
        SparqlBuildingVisitor visitor = new SparqlBuildingVisitor(contextBuilder, "?pos", valuesString,
                inverted ? v -> " [] ?prop v ." : v -> v + " ?prop [] .");
        context.accept(visitor);
        String contextString = contextBuilder.toString();
        queryBuilder.append(contextString);
        queryBuilder.append("      } GROUP BY ?prop\n");
        queryBuilder.append("    } UNION {\n");
        queryBuilder.append("      SELECT ?prop (0 AS ?tp) (COUNT(DISTINCT ?neg) AS ?fp) WHERE {\n        ");
        // generate the negative context by replacing all VALUES statements and after
        // that all remaining ?pos occurrences.
        queryBuilder.append(
                contextString.replaceAll("VALUES[ ]+\\?pos[ ]+\\{[^}]*\\}", generateValuesStmt("?neg", negative))
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
        String valuesString = generateValuesStmt("?pos", positive);
        StringBuilder contextBuilder = new StringBuilder();
        SparqlBuildingVisitor visitor = new SparqlBuildingVisitor(contextBuilder, "?pos", valuesString,
                inverted ? v -> " [] ?prop v ." : v -> v + " ?prop [] .");
        context.accept(visitor);
        String contextString = contextBuilder.toString();
        queryBuilder.append(contextString);
        queryBuilder.append("    } GROUP BY ?prop }\n");
        queryBuilder.append("    OPTIONAL {\n        ");
        // generate the negative context by replacing all VALUES statements and after
        // that all remaining ?pos occurrences.
        queryBuilder.append(
                contextString.replaceAll("VALUES[ ]+\\?pos[ ]+\\{[^}]*\\}", generateValuesStmt("?neg", negative))
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
        ClassExpression prepared = prepareClassExpression(expression);
        String query = generateScoreQueryForGeneralNegation(positive, negative, prepared);
        LOGGER.trace("Sending query {}", query);
        // Create the query execution with try-catch to ensure that it will be closed
        try (QueryExecution qe = queryExecFactory.createQueryExecution(query);) {
            ResultSet result = qe.execSelect();
            if (result.hasNext()) {
                QuerySolution s = result.next();
                return new SelectionScores(s.getLiteral("posHits").getInt(), s.getLiteral("negHits").getInt());
            } else {
                LOGGER.warn("Got an empty result fo the expression {}. Returning a zero score.", expression);
                return new SelectionScores(0, 0);
            }
        } catch (Exception e) {
            LOGGER.error("Exception while executing SPARQL request. query=" + query, e);
            throw e;
        }
    }

    protected String generateScoreQueryForGeneralNegation(Collection<String> positive, Collection<String> negative,
            ClassExpression expression) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ?posHits ?negHits WHERE {\n");
        queryBuilder.append("    { SELECT (COUNT(DISTINCT ?pos) AS ?posHits) WHERE {\n        ");
        String valuesString = generateValuesStmt("?pos", positive);
        StringBuilder contextBuilder = new StringBuilder();
        SparqlBuildingVisitor visitor = new SparqlBuildingVisitor(contextBuilder, "?pos", valuesString, v -> "");
        expression.accept(visitor);
        String contextString = contextBuilder.toString();
        queryBuilder.append(contextString);
        queryBuilder.append("    }\n");
        queryBuilder.append("    { SELECT (COUNT(DISTINCT ?neg) AS ?negHits) WHERE {\n        ");
        // generate the negative context by replacing all VALUES statements and after
        // that all remaining ?pos occurrences.
        queryBuilder.append(
                contextString.replaceAll("VALUES[ ]+\\?pos[ ]+\\{[^}]*\\}", generateValuesStmt("?neg", negative))
                        .replaceAll("\\?pos ", "?neg "));
        queryBuilder.append("    }\n");
        queryBuilder.append("}");
        return queryBuilder.toString();
    }

    protected String generateValuesStmt(String variable, Collection<String> values) {
        StringBuilder valuesBuilder = new StringBuilder();
        appendValues(valuesBuilder, variable, values);
        return valuesBuilder.toString();
    }

    protected void appendValues(StringBuilder queryBuilder, String variable, Collection<String> values) {
        queryBuilder.append("VALUES ");
        queryBuilder.append(variable);
        queryBuilder.append(" {");
        for (String value : values) {
            queryBuilder.append(" <");
            queryBuilder.append(value);
            queryBuilder.append('>');
        }
        queryBuilder.append(" }\n");
    }

    /**
     * FIXME Before transforming the expression to a SPARQL query, we should move
     * all disjunctions/UNION up the tree to ensure that they are the root node of
     * the expression. Otherwise, we will always face performance issues on some
     * SPARQL stores.
     * 
     * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
     *
     */
    public static class SparqlBuildingVisitor implements ClassExpressionVisitor {

        protected static final String INTERMEDIATE_VARIABLE_NAME = "?x";

        protected StringBuilder queryBuilder;
        protected Deque<String> variables = new ArrayDeque<String>();
        protected String valuesString;
        protected int nextVariableId = 0;
        protected boolean isRoot = true;
        protected Function<String, String> variableToStmtOnMarkedPosition;
        protected NegatingVisitor negator = new NegatingVisitor();

        public SparqlBuildingVisitor(StringBuilder queryBuilder, String firstVariable, String valuesString,
                Function<String, String> variableToStmtOnMarkedPosition) {
            super();
            this.queryBuilder = queryBuilder;
            this.valuesString = valuesString;
            this.variableToStmtOnMarkedPosition = variableToStmtOnMarkedPosition;
            variables.addFirst(firstVariable);
        }

        protected String getNextVariable() {
            return INTERMEDIATE_VARIABLE_NAME + nextVariableId++;
        }

        @Override
        public void visitNamedClass(NamedClass node) {
            // If this is the root node, we can simply add the values
            if (isRoot) {
                queryBuilder.append("        ");
                queryBuilder.append(valuesString);
            }
            // Check if this is the marked position
            if (Suggestor.CONTEXT_POSITION_MARKER.equals(node)) {
                queryBuilder.append("        ");
                queryBuilder.append(variableToStmtOnMarkedPosition.apply(variables.peek()));
                queryBuilder.append("\n");
            } else if (NamedClass.TOP.equals(node)) {
                // Nothing to do
            } else if (NamedClass.BOTTOM.equals(node)) {
                queryBuilder.append("        ");
                queryBuilder.append(variables.peek());
                queryBuilder.append(" a <");
                queryBuilder.append(OWL2.Nothing.getURI());
                queryBuilder.append("> .\n");
            } else {
                if (node.isNegated()) {
                    queryBuilder.append("        FILTER NOT EXISTS {");
                    queryBuilder.append(variables.peek());
                    queryBuilder.append(" a <");
                    queryBuilder.append(node.getName());
                    queryBuilder.append("> . }\n");
                } else {
                    queryBuilder.append("        ");
                    queryBuilder.append(variables.peek());
                    queryBuilder.append(" a <");
                    queryBuilder.append(node.getName());
                    queryBuilder.append("> .\n");
                }
            }
        }

        @Override
        public void visitJunction(Junction node) {
            // If this is a conjunction, we can simply visit all children and let them add
            // their triple patterns
            if (node.isConjunction()) {
                // If this is the root node, we can simply add the values
                if (isRoot) {
                    queryBuilder.append("        ");
                    queryBuilder.append(valuesString);
                }
                boolean oldRoot = isRoot;
                isRoot = false;
                for (ClassExpression child : node.getChildren()) {
                    child.accept(this);
                }
                isRoot = oldRoot;
            } else {
                // This is a disjunction, so we have to create UNION statements
                boolean first = true;
                queryBuilder.append("        {\n");
                for (ClassExpression child : node.getChildren()) {
                    if (first) {
                        first = false;
                    } else {
                        queryBuilder.append("        } UNION {\n");
                    }
                    // Note: we do not change the isRoot flag, because if the disjunction is the
                    // root node, the children of the disjunction need to know the VALUES
                    // restriction.
                    child.accept(this);
                }
                queryBuilder.append("        }\n");
            }
        }

        @Override
        public void visitSimpleQuantificationRole(SimpleQuantifiedRole node) {
            // If this is the root node, we can simply add the values
            if (isRoot) {
                queryBuilder.append("        ");
                queryBuilder.append(valuesString);
            }
            if (node.isExists()) {
                String nextVariable = getNextVariable();
                // Ensure that there is a connection to at least one node that fulfills the tail
                // node
                queryBuilder.append("        ");
                queryBuilder.append(node.isInverted() ? nextVariable : variables.peek());
                queryBuilder.append(" <");
                queryBuilder.append(node.getRole());
                queryBuilder.append("> ");
                queryBuilder.append(node.isInverted() ? variables.peek() : nextVariable);
                queryBuilder.append(" .\n");
                boolean oldRoot = isRoot;
                isRoot = false;
                variables.addFirst(nextVariable);
                node.getTailExpression().accept(this);
                variables.removeFirst();
                isRoot = oldRoot;
            } else {
                // Ensure that for all possible instantiations of the tail node, they do not
                // fulfill the negation of the tail node expression.
                queryBuilder.append("        FILTER NOT EXISTS { ");
                ClassExpression negation = negator.negateExpression(node);
                boolean oldRoot = isRoot;
                isRoot = false;
                negation.accept(this);
                isRoot = oldRoot;
                // Close the bracket of the FILTER statement
                queryBuilder.append("        }\n");
            }
        }

    }

    /**
     * A simple visitor that checks whether the expression contains a disjunction.
     * 
     * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
     *
     */
    public static class DisjunctionCheckingVisitor implements ClassExpressionVisitingCreator<Boolean> {

        public boolean containsDisjunction(ClassExpression ce) {
            return ce.accept(this);
        }

        @Override
        public Boolean visitNamedClass(NamedClass node) {
            return Boolean.FALSE;
        }

        @Override
        public Boolean visitJunction(Junction node) {
            if (node.isConjunction()) {
                for (ClassExpression child : node.getChildren()) {
                    if (child.accept(this)) {
                        return Boolean.TRUE;
                    }
                }
                return Boolean.FALSE;
            } else {
                return Boolean.TRUE;
            }
        }

        @Override
        public Boolean visitSimpleQuantificationRole(SimpleQuantifiedRole node) {
            return node.getTailExpression().accept(this);
        }

    }

    public static class ExpressionPreProcessor implements ClassExpressionVisitingCreator<ClassExpression[]> {

        public ClassExpression preprocess(ClassExpression ce) {
            ClassExpression[] subExpressions = ce.accept(this);
            if (subExpressions.length == 1) {
                return subExpressions[0];
            } else {
                return new Junction(false, subExpressions);
            }
        }

        @Override
        public ClassExpression[] visitNamedClass(NamedClass node) {
            return new ClassExpression[] { node };
        }

        @Override
        public ClassExpression[] visitJunction(Junction node) {
            List<ClassExpression> expressions = new ArrayList<>(node.getChildren().size());
            if (node.isConjunction()) {
                // Create all possible combinations of the arrays that we get.
                ClassExpression[][] arrays = new ClassExpression[node.getChildren().size()][];
                int pos = 0;
                for (ClassExpression child : node.getChildren()) {
                    arrays[pos] = child.accept(this);
                    ++pos;
                }
                int indexes[] = new int[arrays.length];
                ClassExpression[] combination = new ClassExpression[arrays.length];
                boolean moreCombinations = true;
                while (moreCombinations) {
                    // Create new combination according to indexes
                    for (int i = 0; i < indexes.length; ++i) {
                        combination[i] = arrays[i][indexes[i]];
                    }
                    expressions.add(new Junction(true, combination));
                    // increase indexes (works like a clock / counter)
                    pos = 0;
                    ++indexes[0];
                    while ((pos < indexes.length) && (indexes[pos] >= arrays[pos].length)) {
                        indexes[pos] = 0;
                        ++pos;
                        if (pos < indexes.length) {
                            ++indexes[pos];
                        } else {
                            // We saw all combinations
                            moreCombinations = false;
                        }
                    }
                }
            } else {
                // We simply forward all results of the children
                ClassExpression[] currentArray;
                for (ClassExpression child : node.getChildren()) {
                    currentArray = child.accept(this);
                    Collections.addAll(expressions, currentArray);
                }
            }
            return expressions.toArray(ClassExpression[]::new);
        }

        @Override
        public ClassExpression[] visitSimpleQuantificationRole(SimpleQuantifiedRole node) {
            ClassExpression[] tailExpressions = node.getTailExpression().accept(this);
            for (int i = 0; i < tailExpressions.length; ++i) {
                tailExpressions[i] = new SimpleQuantifiedRole(node.isExists(), node.getRole(), node.isInverted(),
                        tailExpressions[i]);
            }
            return tailExpressions;
        }

    }

}
