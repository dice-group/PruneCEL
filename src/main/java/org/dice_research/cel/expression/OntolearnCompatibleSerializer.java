package org.dice_research.cel.expression;

public class OntolearnCompatibleSerializer {

    public String getSerialization(ClassExpression ce) {
        SerializingVisitor visitor = new SerializingVisitor();
        ce.accept(visitor);
        return visitor.getSerialization();
    }

    protected static class SerializingVisitor implements ClassExpressionVisitor {

        protected StringBuilder builder = new StringBuilder();

        @Override
        public void visitNamedClass(NamedClass node) {
            if (node.isNegated()) {
                builder.append('¬');
            }
            if (NamedClass.TOP.equals(node) || NamedClass.BOTTOM.equals(node)) {
                builder.append(node.getName());
            } else {
                builder.append("<");
                builder.append(node.getName());
                builder.append(">");
            }
        }

        @Override
        public void visitJunction(Junction node) {
            boolean first = true;
            builder.append('(');
            for (ClassExpression child : node.getChildren()) {
                if (!first) {
                    builder.append(node.isConjunction() ? " ⊓ " : " ⊔ ");
                }
                if (child != null) {
                    child.accept(this);
                } else {
                    builder.append("null");
                }
                first = false;
            }
            builder.append(')');
        }

        @Override
        public void visitSimpleQuantificationRole(SimpleQuantifiedRole node) {
            if (node.isExists()) {
                builder.append('∃');
            } else {
                builder.append('∀');
            }
            builder.append("<");
            builder.append(node.getRole());
            builder.append(">");
            if (node.isInverted()) {
                builder.append('-');
            }
            builder.append('.');
            ClassExpression child = node.getTailExpression();
            if (child != null) {
                child.accept(this);
            } else {
                builder.append("null");
            }
        }

        public String getSerialization() {
            return builder.toString();
        }
    }
}