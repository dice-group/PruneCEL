package org.dice_research.cel.refine.suggest.sparql;

import org.dice_research.cel.expression.ClassExpression;
import org.dice_research.cel.expression.ClassExpressionVisitingCreator;
import org.dice_research.cel.expression.Junction;
import org.dice_research.cel.expression.NamedClass;
import org.dice_research.cel.expression.SimpleQuantifiedRole;

public class SuggestionCheckingVisitor implements ClassExpressionVisitingCreator<Boolean> {

    public boolean containsDisjunction(ClassExpression ce) {
        return ce.accept(this);
    }

    @Override
    public Boolean visitNamedClass(NamedClass node) {
        return SparqlBasedSuggestor.CONTEXT_POSITION_MARKER.equals(node);
    }

    @Override
    public Boolean visitJunction(Junction node) {
        for (ClassExpression child : node.getChildren()) {
            if (child.accept(this)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    @Override
    public Boolean visitSimpleQuantificationRole(SimpleQuantifiedRole node) {
        return node.getTailExpression().accept(this);
    }

}
