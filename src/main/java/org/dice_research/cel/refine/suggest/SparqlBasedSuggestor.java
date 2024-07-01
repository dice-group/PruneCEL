package org.dice_research.cel.refine.suggest;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
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
import org.dice_research.cel.expression.ClassExpressionVisitor;
import org.dice_research.cel.expression.Junction;
import org.dice_research.cel.expression.NamedClass;
import org.dice_research.cel.expression.NegatingVisitor;
import org.dice_research.cel.expression.SimpleQuantifiedRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparqlBasedSuggestor implements Suggestor, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SparqlBasedSuggestor.class);

    protected QueryExecutionFactory queryExecFactory;
    protected Set<String> classBlackList = new HashSet<String>();
    protected Set<String> propertyBlackList = new HashSet<String>();
    protected DescriptionLogic logic;

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

    @Override
    public Collection<ScoredIRI> suggestClass(Collection<String> positive, Collection<String> negative,
            ClassExpression context) {
        String query;
        if (logic.supportsComplexConceptNegation()) {
            query = generateClassQueryForGeneralNegation(positive, negative, context);
        } else {
            query = generateClassQuery(positive, negative, context);
        }
        return performClassSelection(query);
    }

    public Collection<ScoredIRI> suggestNegatedClass(Collection<String> positive, Collection<String> negative,
            ClassExpression context) {
        return performClassSelection(generateClassQueryForGeneralNegation(positive, negative, context));
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
        queryBuilder.append("    { SELECT ?class (COUNT(DISTINCT ?pos) AS ?tp) WHERE {\n");
        queryBuilder.append("        VALUES ?pos {");
        for (String pos : positive) {
            queryBuilder.append(" <");
            queryBuilder.append(pos);
            queryBuilder.append('>');
        }
        queryBuilder.append(" }\n");
        StringBuilder contextBuilder = new StringBuilder();
        SparqlBuildingVisitor visitor = new SparqlBuildingVisitor(contextBuilder, "?pos", v -> v + " a ?class .");
        context.accept(visitor);
        String contextString = contextBuilder.toString();
        queryBuilder.append(contextString);
        queryBuilder.append("    } GROUP BY ?class }\n");
        queryBuilder.append("    VALUES ?neg {");
        for (String neg : negative) {
            queryBuilder.append(" <");
            queryBuilder.append(neg);
            queryBuilder.append('>');
        }
        queryBuilder.append(" }\n    OPTIONAL {\n");
        queryBuilder.append(contextString.replaceAll("\\?pos ", "?neg "));
        queryBuilder.append("    }} GROUP BY ?class\nORDER BY DESC(?posHits) (?negHits)");
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
        queryBuilder.append("    { SELECT ?class (COUNT(DISTINCT ?pos) AS ?tp) (MAX(?neg) AS ?fp) WHERE {\n");
        queryBuilder.append("        VALUES ?pos {");
        for (String pos : positive) {
            queryBuilder.append(" <");
            queryBuilder.append(pos);
            queryBuilder.append('>');
        }
        queryBuilder.append(" }\n");
        StringBuilder contextBuilder = new StringBuilder();
        SparqlBuildingVisitor visitor = new SparqlBuildingVisitor(contextBuilder, "?pos",
                v -> "?class a <" + OWL.Class.getURI() + "> .        \nFILTER NOT EXISTS { " + v + " a ?class . }");
        context.accept(visitor);
        String contextString = contextBuilder.toString();
        queryBuilder.append(contextString);
        queryBuilder.append("        BIND (0 as ?neg)\n      } GROUP BY ?class\n");
        queryBuilder.append("    } UNION {\n");
        queryBuilder.append("      SELECT ?class (MAX(?pos) AS ?tp) (COUNT(DISTINCT ?neg) AS ?fp) WHERE {");
        queryBuilder.append("        VALUES ?neg {");
        for (String neg : negative) {
            queryBuilder.append(" <");
            queryBuilder.append(neg);
            queryBuilder.append('>');
        }
        queryBuilder.append(" }\n");
        queryBuilder.append(contextString.replaceAll("\\?pos ", "?neg "));
        queryBuilder.append("        BIND (0 as ?pos)\n      } GROUP BY ?class\n    }\n");
        queryBuilder.append("} GROUP BY ?class\nORDER BY DESC(?posHits) (?negHits)");
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
        queryBuilder.append("    { SELECT ?class (COUNT(DISTINCT ?pos) AS ?tp) (MAX(?neg) AS ?fp) WHERE {\n");
        queryBuilder.append("        VALUES ?pos {");
        for (String pos : positive) {
            queryBuilder.append(" <");
            queryBuilder.append(pos);
            queryBuilder.append('>');
        }
        queryBuilder.append(" }\n");
        StringBuilder contextBuilder = new StringBuilder();
        SparqlBuildingVisitor visitor = new SparqlBuildingVisitor(contextBuilder, "?pos", v -> v + " a ?class .");
        context.accept(visitor);
        String contextString = contextBuilder.toString();
        queryBuilder.append(contextString);
        queryBuilder.append("        BIND (0 as ?neg)\n      } GROUP BY ?class\n");
        queryBuilder.append("    } UNION {\n");
        queryBuilder.append("      SELECT ?class (MAX(?pos) AS ?tp) (COUNT(DISTINCT ?neg) AS ?fp) WHERE {");
        queryBuilder.append("        VALUES ?neg {");
        for (String neg : negative) {
            queryBuilder.append(" <");
            queryBuilder.append(neg);
            queryBuilder.append('>');
        }
        queryBuilder.append(" }\n");
        queryBuilder.append(contextString.replaceAll("\\?pos ", "?neg "));
        queryBuilder.append("        BIND (0 as ?pos)\n      } GROUP BY ?class\n    }\n");
        queryBuilder.append("} GROUP BY ?class\nORDER BY DESC(?posHits) (?negHits)");
        return queryBuilder.toString();
    }

    @Override
    public Collection<ScoredIRI> suggestProperty(Collection<String> positive, Collection<String> negative,
            ClassExpression context) {
        List<ScoredIRI> results = new ArrayList<>();
        String query;
        if (logic.supportsAtomicNegation()) {
            query = generatePropertyQuery(positive, negative, context);
        } else {
            query = generatePropertyQueryWithoutNegation(positive, negative, context);
        }
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
                                s.getLiteral("negHits").getInt()));
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
            ClassExpression context) {
        // TODO add usage of inverse properties!
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ?prop (MAX(?tp) AS ?posHits) (MAX(?fp) AS ?negHits) WHERE {\n");
        queryBuilder.append("    { SELECT ?prop (COUNT(DISTINCT ?pos) AS ?tp) (MAX(?neg) AS ?fp) WHERE {\n");
        queryBuilder.append("        VALUES ?pos {");
        for (String pos : positive) {
            queryBuilder.append(" <");
            queryBuilder.append(pos);
            queryBuilder.append('>');
        }
        queryBuilder.append(" }\n");
        StringBuilder contextBuilder = new StringBuilder();
        SparqlBuildingVisitor visitor = new SparqlBuildingVisitor(contextBuilder, "?pos", v -> v + " ?prop [] .");
        context.accept(visitor);
        String contextString = contextBuilder.toString();
        queryBuilder.append(contextString);
        queryBuilder.append("        BIND (0 as ?neg)\n      } GROUP BY ?prop\n");
        queryBuilder.append("    } UNION {\n");
        queryBuilder.append("      SELECT ?prop (MAX(?pos) AS ?tp) (COUNT(DISTINCT ?neg) AS ?fp) WHERE {");
        queryBuilder.append("        VALUES ?neg {");
        for (String neg : negative) {
            queryBuilder.append(" <");
            queryBuilder.append(neg);
            queryBuilder.append('>');
        }
        queryBuilder.append(" }\n");
        queryBuilder.append(contextString.replaceAll("\\?pos ", "?neg "));
        queryBuilder.append("        BIND (0 as ?pos)\n      } GROUP BY ?prop\n    }\n");
        queryBuilder.append("} GROUP BY ?prop\nORDER BY DESC(?posHits) (?negHits)");
        return queryBuilder.toString();
    }

    protected String generatePropertyQueryWithoutNegation(Collection<String> positive, Collection<String> negative,
            ClassExpression context) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ?prop (MAX(?pc) AS ?posHits) (COUNT(DISTINCT ?negId) AS ?negHits) WHERE {\n");
        queryBuilder.append("    { SELECT ?prop (COUNT(DISTINCT ?pos) AS ?pc) WHERE {\n");
        queryBuilder.append("        VALUES ?pos {");
        for (String pos : positive) {
            queryBuilder.append(" <");
            queryBuilder.append(pos);
            queryBuilder.append('>');
        }
        queryBuilder.append(" }\n");
        StringBuilder contextBuilder = new StringBuilder();
        SparqlBuildingVisitor visitor = new SparqlBuildingVisitor(contextBuilder, "?pos", v -> v + " ?prop [] .");
        context.accept(visitor);
        String contextString = contextBuilder.toString();
        queryBuilder.append(contextString);
        queryBuilder.append("    } GROUP BY ?prop }\n");
        queryBuilder.append("    VALUES ?neg {");
        for (String neg : negative) {
            queryBuilder.append(" <");
            queryBuilder.append(neg);
            queryBuilder.append('>');
        }
        queryBuilder.append(" }\n    OPTIONAL {\n");
        queryBuilder.append(contextString.replaceAll("\\?pos ", "?neg "));
        queryBuilder.append("        BIND (CONCAT(STR(?neg),STR(?prop)) as ?negId)}\n");
        queryBuilder.append("    } GROUP BY ?prop\nORDER BY DESC(?posHits) (?negHits)");
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

    public static class SparqlBuildingVisitor implements ClassExpressionVisitor {

        protected static final String INTERMEDIATE_VARIABLE_NAME = "?x";

        protected StringBuilder queryBuilder;
        protected Deque<String> variables = new ArrayDeque<String>();
        protected int nextVariableId = 0;
        protected Function<String, String> variableToStmtOnMarkedPosition;
        protected NegatingVisitor negator = new NegatingVisitor();

        public SparqlBuildingVisitor(StringBuilder queryBuilder, String firstVariable,
                Function<String, String> variableToStmtOnMarkedPosition) {
            super();
            this.queryBuilder = queryBuilder;
            this.variableToStmtOnMarkedPosition = variableToStmtOnMarkedPosition;
            variables.addFirst(firstVariable);
        }

        protected String getNextVariable() {
            return INTERMEDIATE_VARIABLE_NAME + nextVariableId++;
        }

        @Override
        public void visitNamedClass(NamedClass node) {
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
                for (ClassExpression child : node.getChildren()) {
                    child.accept(this);
                }
            } else {
                // This is a conjunction, so we have to create UNION statements
                boolean first = true;
                queryBuilder.append("        {\n");
                for (ClassExpression child : node.getChildren()) {
                    if (first) {
                        first = false;
                    } else {
                        queryBuilder.append("        } UNION {\n");
                    }
                    child.accept(this);
                }
                queryBuilder.append("        }\n");
            }
        }

        @Override
        public void visitSimpleQuantificationRole(SimpleQuantifiedRole node) {
            String nextVariable = getNextVariable();
            ClassExpression tail;
            if (node.isExists()) {
                // Ensure that there is a connection to at least one node that fulfills the tail
                // node
                queryBuilder.append("        ");
                tail = node.getTailExpression();
            } else {
                // Ensure that for all possible instantiations of the tail node, they do not
                // fulfill the negation of the tail node expression.
                queryBuilder.append("        FILTER NOT EXISTS { ");
                tail = negator.negateExpression(node);
            }
            queryBuilder.append(variables.peek());
            queryBuilder.append(" <");
            queryBuilder.append(node.getRole());
            queryBuilder.append("> ");
            queryBuilder.append(nextVariable);
            queryBuilder.append(" .\n");
            variables.addFirst(nextVariable);
            tail.accept(this);
            variables.removeFirst();
            if (!node.isExists()) {
                // Close the bracket of the FILTER statement
                queryBuilder.append("}");
            }
        }

    }

}
