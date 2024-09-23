package org.dice_research.cel.refine.suggest.sparql;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Function;

import org.apache.jena.vocabulary.OWL2;
import org.dice_research.cel.expression.ClassExpression;
import org.dice_research.cel.expression.ClassExpressionVisitor;
import org.dice_research.cel.expression.Junction;
import org.dice_research.cel.expression.NamedClass;
import org.dice_research.cel.expression.NegatingVisitor;
import org.dice_research.cel.expression.SimpleQuantifiedRole;
import org.dice_research.cel.refine.suggest.Suggestor;

/**
 * FIXME Before transforming the expression to a SPARQL query, we should move
 * all disjunctions/UNION up the tree to ensure that they are the root node of
 * the expression. Otherwise, we will always face performance issues on some
 * SPARQL stores.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class SparqlBuildingVisitor implements ClassExpressionVisitor {

    protected static final String INTERMEDIATE_VARIABLE_NAME = "?x";

    protected StringBuilder queryBuilder;
    protected Deque<String> variables = new ArrayDeque<String>();
    protected String valuesString;
    protected String filterString;
    protected String intermediateVariableName = INTERMEDIATE_VARIABLE_NAME;
    protected int nextVariableId = 0;
    protected boolean isRoot = true;
    protected Function<String, String> variableToStmtOnMarkedPosition;
    protected NegatingVisitor negator = new NegatingVisitor();

    /**
     * Constructor.
     * 
     * @param queryBuilder                   String builder to which the generated
     *                                       SPARQL will be added
     * @param firstVariable                  the name of the first variable
     *                                       (starting with the '?' character)
     * @param valuesString                   the VALUES statement binding the given
     *                                       first variable to a set of values (can
     *                                       be null)
     * @param filterString                   an additional filter that should be
     *                                       added to the selected variable (can be
     *                                       null)
     * @param variableToStmtOnMarkedPosition the function that transforms the given
     *                                       variable name into the select statement
     *                                       that is expected at the marked position
     *                                       within the context to which this
     *                                       visitor is applied
     */
    public SparqlBuildingVisitor(StringBuilder queryBuilder, String firstVariable, String valuesString,
            String filterString, Function<String, String> variableToStmtOnMarkedPosition) {
        super();
        this.queryBuilder = queryBuilder;
        this.valuesString = valuesString;
        this.filterString = filterString;
        this.variableToStmtOnMarkedPosition = variableToStmtOnMarkedPosition;
        variables.addFirst(firstVariable);
    }

    protected String getNextVariable() {
        return intermediateVariableName + nextVariableId++;
    }

    @Override
    public void visitNamedClass(NamedClass node) {
        // If this is the root node, we can simply add the values
        if (isRoot) {
            if (valuesString != null) {
                queryBuilder.append(valuesString);
            }
        }
        // Check if this is the marked position
        if (Suggestor.CONTEXT_POSITION_MARKER.equals(node)) {
            queryBuilder.append("        ");
            queryBuilder.append(variableToStmtOnMarkedPosition.apply(variables.peek()));
            queryBuilder.append('\n');
            if (filterString != null) {
                queryBuilder.append("        ");
                queryBuilder.append(filterString);
                queryBuilder.append('\n');
            }
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
                queryBuilder.append("        FILTER NOT EXISTS { ");
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
                if (valuesString != null) {
                    queryBuilder.append(valuesString);
                }
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
            if (valuesString != null) {
                queryBuilder.append(valuesString);
            }
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
            queryBuilder.append("        FILTER NOT EXISTS {\n");
            ClassExpression negation = negator.negateExpression(node);
            boolean oldRoot = isRoot;
            isRoot = false;
            negation.accept(this);
            isRoot = oldRoot;
            // Close the bracket of the FILTER statement
            queryBuilder.append("        }\n");
        }
    }

    /**
     * @param intermediateVariableName the intermediateVariableName to set
     */
    public void setIntermediateVariableName(String intermediateVariableName) {
        this.intermediateVariableName = intermediateVariableName;
    }

}