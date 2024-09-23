package org.dice_research.cel.refine.suggest.sparql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.dice_research.cel.expression.ClassExpression;
import org.dice_research.cel.expression.ClassExpressionVisitingCreator;
import org.dice_research.cel.expression.Junction;
import org.dice_research.cel.expression.NamedClass;
import org.dice_research.cel.expression.SimpleQuantifiedRole;

public class ExpressionPreProcessor implements ClassExpressionVisitingCreator<ClassExpression[]> {

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