package org.dice_research.cel;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.cel.expression.ClassExpression;
import org.dice_research.cel.expression.Junction;
import org.dice_research.cel.expression.NamedClass;
import org.dice_research.cel.expression.SimpleQuantifiedRole;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ALCTest extends AbstractCELTest {

    @Parameters
    public static List<Object[]> parameters() {
        List<Object[]> testCases = new ArrayList<>();

        Model model;
        ClassExpression expected;
        Resource pos1 = ResourceFactory.createResource("http://example.org/pos1");
        Resource pos2 = ResourceFactory.createResource("http://example.org/pos2");

        Resource neg1 = ResourceFactory.createResource("http://example.org/neg1");
        Resource neg2 = ResourceFactory.createResource("http://example.org/neg2");

        Resource x1 = ResourceFactory.createResource("http://example.org/x1");
        Resource x2 = ResourceFactory.createResource("http://example.org/x2");
        Resource x3 = ResourceFactory.createResource("http://example.org/x3");

        Resource classA = ResourceFactory.createResource("http://example.org/classA");
        Resource classB = ResourceFactory.createResource("http://example.org/classB");
        Resource classC = ResourceFactory.createResource("http://example.org/classC");

        Property role1 = ResourceFactory.createProperty("http://example.org/role1");

        // Basic example for concept disjunction
        // A(pos1), B(pos2), C(neg1)
        model = ModelFactory.createDefaultModel();
        model.add(pos1, RDF.type, classA);
        model.add(pos2, RDF.type, classB);
        model.add(neg1, RDF.type, classC);
        expected = new Junction(false, new NamedClass(classA.getURI()), new NamedClass(classB.getURI()));
        testCases.add(new Object[] { model, new String[] { pos1.getURI(), pos2.getURI() },
                new String[] { neg1.getURI(), neg2.getURI() }, expected });

        // Basic example for a single named concept
        // A(pos1,pos2),
        model = ModelFactory.createDefaultModel();
        model.add(pos1, RDF.type, classA);
        model.add(pos2, RDF.type, classA);
        expected = new NamedClass(classA.getURI());
        testCases.add(new Object[] { model, new String[] { pos1.getURI(), pos2.getURI() },
                new String[] { neg1.getURI(), neg2.getURI() }, expected });

        // Basic example for concept conjunction
        // A(pos1, pos2, neg1), B(pos1, pos2, neg2)
        model = ModelFactory.createDefaultModel();
        model.add(pos1, RDF.type, classA);
        model.add(pos1, RDF.type, classB);
        model.add(pos2, RDF.type, classA);
        model.add(pos2, RDF.type, classB);
        model.add(neg1, RDF.type, classA);
        model.add(neg2, RDF.type, classB);
        expected = new Junction(true, new NamedClass(classA.getURI()), new NamedClass(classB.getURI()));
        testCases.add(new Object[] { model, new String[] { pos1.getURI(), pos2.getURI() },
                new String[] { neg1.getURI(), neg2.getURI() }, expected });

        // Basic example for a (limited) existential quantifier
        // r(pos1,c), r(pos2,c)
        model = ModelFactory.createDefaultModel();
        model.add(pos1, role1, classC);
        model.add(pos2, role1, classC);
        expected = new SimpleQuantifiedRole(true, role1.getURI(), false, NamedClass.TOP);
        testCases.add(new Object[] { model, new String[] { pos1.getURI(), pos2.getURI() },
                new String[] { neg1.getURI(), neg2.getURI() }, expected });

        // Basic example for an existential quantifier
        // r(pos1,x1), r(pos2,x2) r(neg1, x3) r(neg2, x3) A(x1, x2)
        model = ModelFactory.createDefaultModel();
        model.add(pos1, role1, x1);
        model.add(pos2, role1, x2);
        model.add(neg1, role1, x3);
        model.add(neg2, role1, x3);
        model.add(x1, RDF.type, classA);
        model.add(x2, RDF.type, classA);
        expected = new SimpleQuantifiedRole(true, role1.getURI(), false, new NamedClass(classA.getURI()));
        testCases.add(new Object[] { model, new String[] { pos1.getURI(), pos2.getURI() },
                new String[] { neg1.getURI(), neg2.getURI() }, expected });

        // Basic example for a negated single named concept
        // A(neg1,neg2),
        model = ModelFactory.createDefaultModel();
        model.add(neg1, RDF.type, classA);
        model.add(neg2, RDF.type, classA);
        expected = new NamedClass(classA.getURI(), true);
        testCases.add(new Object[] { model, new String[] { pos1.getURI(), pos2.getURI() },
                new String[] { neg1.getURI(), neg2.getURI() }, expected });

        // Basic example for a universal quantifier
        // r(pos1,x1) r(pos1,x2) r(pos2,x2) r(neg1, x1) r(neg1, x3) r(neg2, x2) r(neg2,
        // x3) A(x1, x2)
        model = ModelFactory.createDefaultModel();
        model.add(pos1, role1, x1);
        model.add(pos1, role1, x2);
        model.add(pos2, role1, x1);
        model.add(pos2, role1, x2);
        model.add(neg1, role1, x1);
        model.add(neg1, role1, x3);
        model.add(neg2, role1, x2);
        model.add(neg2, role1, x3);
        model.add(x1, RDF.type, classA);
        model.add(x2, RDF.type, classA);
        expected = new SimpleQuantifiedRole(false, role1.getURI(), false, new NamedClass(classA.getURI()));
        testCases.add(new Object[] { model, new String[] { pos1.getURI(), pos2.getURI() },
                new String[] { neg1.getURI(), neg2.getURI() }, expected });

        return testCases;
    }

    public ALCTest(Model model, String[] positives, String[] negatives, ClassExpression expected) {
        super("ALC", model, positives, negatives, expected);
    }
}
