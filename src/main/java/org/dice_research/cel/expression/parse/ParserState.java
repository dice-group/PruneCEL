package org.dice_research.cel.expression.parse;

import java.util.Deque;
import java.util.LinkedList;

import org.dice_research.cel.expression.ClassExpression;

public class ParserState {
    public String expressionString;
    public int nextChar = 0;
    public int length;
    public Deque<ClassExpression> stack = new LinkedList<>();

    public ParserState(String expressionString) {
        this.expressionString = expressionString;
        length = expressionString.length();
    }
}