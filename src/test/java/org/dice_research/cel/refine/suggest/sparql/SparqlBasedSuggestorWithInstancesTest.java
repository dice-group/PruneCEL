package org.dice_research.cel.refine.suggest.sparql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aksw.jenax.arq.connection.core.QueryExecutionFactory;
import org.aksw.jenax.connection.query.QueryExecutionFactoryDataset;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.cel.DescriptionLogic;
import org.dice_research.cel.TestHelper;
import org.dice_research.cel.expression.ClassExpression;
import org.dice_research.cel.expression.SimpleQuantifiedRole;
import org.dice_research.cel.refine.instances.ScoredIRIWithInstances;
import org.dice_research.cel.refine.suggest.ScoredIRI;
import org.dice_research.cel.refine.suggest.Suggestor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import javolution.util.FastBitSet;

@RunWith(Parameterized.class)
public class SparqlBasedSuggestorWithInstancesTest implements Comparator<ScoredIRI> {

    protected String logic;
    protected Model model;
    protected Collection<String> positives;
    protected Collection<String> negatives;
    protected ClassExpression input;
    protected ScoredIRIWithInstances[] expectedClasses;
    protected ScoredIRIWithInstances[] expectedProperties;
    protected Map<String, Integer> positiveMap;
    protected Map<String, Integer> negativeMap;

    public SparqlBasedSuggestorWithInstancesTest(ClassExpression input, String logic, Model model,
            Map<String, Integer> positiveMap, Map<String, Integer> negativeMap, String[] positives, String[] negatives,
            ScoredIRIWithInstances[] expectedClasses, ScoredIRIWithInstances[] expectedProperties) {
        super();
        this.input = input;
        this.logic = logic;
        this.model = model;
        this.positiveMap = positiveMap;
        this.negativeMap = negativeMap;
        this.positives = Arrays.asList(positives);
        this.negatives = Arrays.asList(negatives);
        this.expectedClasses = expectedClasses;
        Arrays.sort(this.expectedClasses, this);
        this.expectedProperties = expectedProperties;
        Arrays.sort(this.expectedProperties, this);
    }

    @Test
    public void test() throws Exception {
        DescriptionLogic dl = DescriptionLogic.parse(logic);
        Assert.assertNotNull(dl);
        Dataset dataset = DatasetFactory.create(model);
        try (QueryExecutionFactory qef = new QueryExecutionFactoryDataset(dataset);
                SparqlBasedSuggestorWithInstances suggestor = new SparqlBasedSuggestorWithInstances(qef, dl)) {
            suggestor.addToClassBlackList(OWL2.NamedIndividual.getURI());
            suggestor.addToPropertyBlackList(RDF.type.getURI());
            Collection<ScoredIRIWithInstances> suggestions;
            ScoredIRIWithInstances[] result;

            suggestions = suggestor.suggestClassWithInstances(positives, negatives, positiveMap, negativeMap, input);
            result = suggestions.toArray(ScoredIRIWithInstances[]::new);
            Arrays.sort(result, this);
            Assert.assertArrayEquals(expectedClasses, result);

            suggestions = suggestor.suggestPropertyWithInstances(positives, negatives, positiveMap, negativeMap, input);
            result = suggestions.toArray(ScoredIRIWithInstances[]::new);
            Arrays.sort(result, this);
            Assert.assertArrayEquals(expectedProperties, result);
        }
    }

    @Override
    public int compare(ScoredIRI o1, ScoredIRI o2) {
        int diff = 0;
        if (o1 != null) {
            if (o1.iri != null) {
                if (o2.iri != null) {
                    diff = o1.iri.compareTo(o2.iri);
                } else {
                    return 1;
                }
            } else {
                diff = o2.iri != null ? -1 : 0;
            }
        }
        if (diff == 0) {
            diff = Boolean.compare(o1.inverted, o2.inverted);
        }
        if (diff == 0) {
            diff = Integer.compare(o1.posCount, o2.posCount);
        }
        if (diff == 0) {
            diff = Integer.compare(o1.negCount, o2.negCount);
        }
        return diff;
    }

    public static FastBitSet transform(boolean... bits) {
        FastBitSet result = FastBitSet.newInstance();
        for (int i = 0; i < bits.length; ++i) {
            if (bits[i]) {
                result.set(i);
            }
        }
        return result;
    }

    @Parameters
    public static List<Object[]> parameters() {
        List<Object[]> testCases = new ArrayList<>();
        Map<String, Integer> positiveMap = new HashMap<>();
        Map<String, Integer> negativeMap = new HashMap<>();

        Model model;
        ClassExpression input;
        Resource pos1 = ResourceFactory.createResource("http://example.org/pos1");
        positiveMap.put(pos1.getURI(), 0);
        Resource pos2 = ResourceFactory.createResource("http://example.org/pos2");
        positiveMap.put(pos2.getURI(), 1);

        Resource neg1 = ResourceFactory.createResource("http://example.org/neg1");
        negativeMap.put(neg1.getURI(), 0);
        Resource neg2 = ResourceFactory.createResource("http://example.org/neg2");
        negativeMap.put(neg2.getURI(), 1);
        Resource neg3 = ResourceFactory.createResource("http://example.org/neg3");
        negativeMap.put(neg3.getURI(), 2);

        Resource x1 = ResourceFactory.createResource("http://example.org/x1");
        Resource x2 = ResourceFactory.createResource("http://example.org/x2");
        Resource x3 = ResourceFactory.createResource("http://example.org/x3");
        Resource x4 = ResourceFactory.createResource("http://example.org/x4");
        Resource[] individuals = new Resource[] { pos1, pos2, neg1, neg2, neg3, x1, x2, x3, x4 };

        Resource classA = ResourceFactory.createResource("http://example.org/classA");
        Resource classB = ResourceFactory.createResource("http://example.org/classB");
        Resource classC = ResourceFactory.createResource("http://example.org/classC");
        Resource[] classes = new Resource[] { classA, classB, classC };

        Property role1 = ResourceFactory.createProperty("http://example.org/role1");
        Property role2 = ResourceFactory.createProperty("http://example.org/role2");
        Property role3 = ResourceFactory.createProperty("http://example.org/role3");
        Resource[] roles = new Resource[] { role1, role2, role3 };

//        // ⌖⊔∀role1.⊥
//        input = new Junction(false, Suggestor.CONTEXT_POSITION_MARKER,
//                new SimpleQuantifiedRole(false, role1.getURI(), false, NamedClass.BOTTOM));
//        model = TestHelper.initModel(classes, roles, individuals);
//        model.add(pos1, role1, x1);
//        model.add(pos1, RDF.type, classA);
//        model.add(pos2, RDF.type, classC);
//        model.add(neg1, role1, x2);
//        model.add(neg1, RDF.type, classA);
//        model.add(neg2, role1, x3);
//        model.add(neg2, RDF.type, classB);
//        model.add(neg3, role1, x4);
//        testCases.add(new Object[] { input, "ALC", model, new String[] { pos1.getURI(), pos2.getURI() },
//                new String[] { neg1.getURI(), neg2.getURI(), neg3.getURI() },
//                new ScoredIRI[] { new ScoredIRI(classA.getURI(), 2, 1), new ScoredIRI(classB.getURI(), 1, 1) },
//                new ScoredIRI[] { new ScoredIRI(role1.getURI(), 2, 3) } });
//
//        // ∃role1.(⌖⊓∀role2.⊥)
//        input = new SimpleQuantifiedRole(true, role1.getURI(), false,
//                new Junction(true, Suggestor.CONTEXT_POSITION_MARKER,
//                        new SimpleQuantifiedRole(false, role2.getURI(), false, NamedClass.BOTTOM)));
//        model = TestHelper.initModel(classes, roles, individuals);
//        model.add(pos1, role1, x1);
//        model.add(pos2, RDF.type, classC);
//        model.add(neg1, role1, x2);
//        model.add(neg2, role1, x3);
//        model.add(neg3, role1, x4);
//        model.add(x1, RDF.type, classA);
//        model.add(x2, RDF.type, classA);
//        model.add(x3, RDF.type, classB);
//        model.add(x3, role1, x4);
//        model.add(x4, role2, x1);
//        testCases.add(new Object[] { input, "ALC", model, new String[] { pos1.getURI(), pos2.getURI() },
//                new String[] { neg1.getURI(), neg2.getURI(), neg3.getURI() },
//                new ScoredIRI[] { new ScoredIRI(classA.getURI(), 1, 1), new ScoredIRI(classB.getURI(), 0, 1) },
//                new ScoredIRI[] { new ScoredIRI(role1.getURI(), 0, 1) } });
//
//        // ∀role1.(⌖⊓∀role2.⊥)
//        input = new SimpleQuantifiedRole(false, role1.getURI(), false,
//                new Junction(true, Suggestor.CONTEXT_POSITION_MARKER,
//                        new SimpleQuantifiedRole(false, role2.getURI(), false, NamedClass.BOTTOM)));
//        model = TestHelper.initModel(classes, roles, individuals);
//        model.add(pos1, role1, x1);
//        model.add(pos2, RDF.type, classC);
//        model.add(neg1, role1, x2);
//        model.add(neg2, role1, x3);
//        model.add(neg3, role1, x4);
//        model.add(x1, RDF.type, classA);
//        model.add(x2, RDF.type, classA);
//        model.add(x3, RDF.type, classB);
//        model.add(x3, role1, x4);
//        model.add(x4, role2, x1);
//        testCases.add(new Object[] { input, "ALC", model, new String[] { pos1.getURI(), pos2.getURI() },
//                new String[] { neg1.getURI(), neg2.getURI(), neg3.getURI() },
//                new ScoredIRI[] { new ScoredIRI(classA.getURI(), 2, 1), new ScoredIRI(classB.getURI(), 1, 1),
//                        new ScoredIRI(classC.getURI(), 1, 0) },
//                new ScoredIRI[] { new ScoredIRI(role1.getURI(), 1, 1), new ScoredIRI(role2.getURI(), 1, 0) } });
//
//        // ∀role1.(⌖⊔∀role2.⊥)
//        input = new SimpleQuantifiedRole(false, role1.getURI(), false,
//                new Junction(false, Suggestor.CONTEXT_POSITION_MARKER,
//                        new SimpleQuantifiedRole(false, role2.getURI(), false, NamedClass.BOTTOM)));
//        model = TestHelper.initModel(classes, roles, individuals);
//        model.add(pos1, role1, x1);
//        model.add(pos2, RDF.type, classC);
//        model.add(neg1, role1, x2);
//        model.add(neg2, role1, x3);
//        model.add(neg3, role1, x4);
//        model.add(x1, RDF.type, classA);
//        model.add(x2, RDF.type, classA);
//        model.add(x3, RDF.type, classB);
//        model.add(x3, role1, x4);
//        model.add(x4, role2, x1);
//        model.add(x4, role1, x3);
//        model.add(x4, RDF.type, classC);
//        testCases.add(new Object[] { input, "ALC", model, new String[] { pos1.getURI(), pos2.getURI() },
//                new String[] { neg1.getURI(), neg2.getURI(), neg3.getURI() },
//                new ScoredIRI[] { new ScoredIRI(classA.getURI(), 2, 2), new ScoredIRI(classB.getURI(), 2, 2),
//                        new ScoredIRI(classC.getURI(), 2, 3) },
//                new ScoredIRI[] { new ScoredIRI(role1.getURI(), 2, 3), new ScoredIRI(role2.getURI(), 2, 3) } });
//
//        // ∀role1.⊥⊔∀role2.⌖
//        input = new Junction(false, new SimpleQuantifiedRole(false, role1.getURI(), false, NamedClass.BOTTOM),
//                new SimpleQuantifiedRole(false, role2.getURI(), false, Suggestor.CONTEXT_POSITION_MARKER));
//        model = TestHelper.initModel(classes, roles, individuals);
//        model.add(pos1, role1, x1);
//        model.add(pos1, role2, x1);
//        model.add(pos2, RDF.type, classC); // ∀role1.⊥
//        model.add(pos2, role2, x3);
//        model.add(neg1, role1, x2);
//        model.add(neg1, role2, x3);
//        model.add(neg2, role1, x3);
//        model.add(neg3, role2, x4); // ∀role1.⊥
//        model.add(x1, RDF.type, classA);
//        model.add(x2, RDF.type, classA);
//        model.add(x3, RDF.type, classB);
//        model.add(x3, role1, x4);
//        model.add(x4, role2, x1);
//        model.add(x4, role1, x3);
//        model.add(x4, RDF.type, classC);
//        testCases.add(new Object[] { input, "ALC", model, new String[] { pos1.getURI(), pos2.getURI() },
//                new String[] { neg1.getURI(), neg2.getURI(), neg3.getURI() },
//                new ScoredIRI[] { new ScoredIRI(classA.getURI(), 2, 2), new ScoredIRI(classB.getURI(), 1, 3),
//                        new ScoredIRI(classC.getURI(), 1, 2) },
//                new ScoredIRI[] { new ScoredIRI(role1.getURI(), 1, 3), new ScoredIRI(role2.getURI(), 1, 2) } });

        // ∀role1.∀role2.∃role3.⌖
        input = new SimpleQuantifiedRole(false, role1.getURI(), false, new SimpleQuantifiedRole(false, role2.getURI(),
                false, new SimpleQuantifiedRole(true, role3.getURI(), false, Suggestor.CONTEXT_POSITION_MARKER)));
        model = TestHelper.initModel(classes, roles, individuals);
        model.add(pos1, role1, x1); // pos1 -r1-> x1 -r2-> x2 -r3-> x1,x2,x3
        model.add(pos1, role2, x2);
        model.add(pos2, RDF.type, classC);
        model.add(pos2, role2, x2); // pos2 -r1-> ⊥
        model.add(neg1, role1, x2); // neg1 -r1-> x2 -r2-> x1,x4 -r3-> ⊥
        model.add(neg1, role2, x3);
        model.add(neg2, role1, x3); // neg1 -r1-> x3 -r2-> x1,x4 -r3-> ⊥
        model.add(neg3, role2, x4); // neg3 -r1-> ⊥
        model.add(x1, RDF.type, classA);
        model.add(x2, RDF.type, classA);
        model.add(x3, RDF.type, classB);
        model.add(x1, role2, x2);
        model.add(x2, role2, x1);
        model.add(x2, role2, x4);
        model.add(x3, role2, x1);
        model.add(x3, role2, x4);
        model.add(x4, role2, x1);
        model.add(x1, role3, x1);
        model.add(x2, role3, x1);
        model.add(x2, role3, x2);
        model.add(x2, role3, x3);
        model.add(x4, RDF.type, classC);
        testCases.add(new Object[] { input, "ALC", model, positiveMap, negativeMap,
                new String[] { pos1.getURI(), pos2.getURI() },
                new String[] { neg1.getURI(), neg2.getURI(), neg3.getURI() },
                new ScoredIRIWithInstances[] {
                        new ScoredIRIWithInstances(classA.getURI(), transform(true, true),
                                transform(false, false, true)),
                        new ScoredIRIWithInstances(classB.getURI(), transform(true, true),
                                transform(false, false, true)),
                        new ScoredIRIWithInstances(classC.getURI(), transform(false, true),
                                transform(false, false, true)), },
                new ScoredIRIWithInstances[] {
                        new ScoredIRIWithInstances(role1.getURI(), transform(false, true),
                                transform(false, false, true)),
                        new ScoredIRIWithInstances(role2.getURI(), transform(true, true),
                                transform(false, false, true)),
                        new ScoredIRIWithInstances(role3.getURI(), transform(true, true),
                                transform(false, false, true)) } });

        Dataset dataset = DatasetFactory.create();
        dataset.addNamedModel("http://ex.org/model", model);
        String query;
        query = "SELECT ?example ?instance ?class FROM <http://ex.org/model> WHERE {"
                + "VALUES ?example2 { <http://example.org/pos1> <http://example.org/pos2> <http://example.org/neg1> <http://example.org/neg2> <http://example.org/neg3> }"
                + "?example2 <http://example.org/role1> ?x0 ."
                + "?x0 <http://example.org/role2> ?x1 ."
                + "?x1 <http://example.org/role3> ?instance ."
                + "VALUES ?example { <http://example.org/pos1> <http://example.org/pos2> <http://example.org/neg1> <http://example.org/neg2> <http://example.org/neg3> }"
                + "FILTER NOT EXISTS {"
                + "?example <http://example.org/role1> ?x01 ."
                + "?x01 <http://example.org/role2> ?x11 ."
                + "FILTER NOT EXISTS {"
                + "?x11 <http://example.org/role3> ?instance .}}OPTIONAL {?instance a ?class .}}";
        
//         query = "SELECT DISTINCT ?class ?x2 ?example FROM <http://ex.org/model> WHERE { { VALUES ?example { <http://example.org/pos1> <http://example.org/pos2> <http://example.org/neg1> <http://example.org/neg2> <http://example.org/neg3> } FILTER NOT EXISTS { ?example <http://example.org/role1> ?x0 . } ?class a <http://www.w3.org/2002/07/owl#Class> . } UNION { VALUES ?example { <http://example.org/pos1> <http://example.org/pos2> <http://example.org/neg1> <http://example.org/neg2> <http://example.org/neg3> } ?example <http://example.org/role1> ?x0 . ?x0 <http://example.org/role2> ?x1 . FILTER NOT EXISTS { ?x1 <http://example.org/role3> ?x2 . ?x2 a ?class . } ?class a <http://www.w3.org/2002/07/owl#Class> . } }";
         query = " SELECT DISTINCT ?class ?x2 ?example FROM <http://ex.org/model> WHERE { VALUES ?example { <http://example.org/pos1> <http://example.org/pos2> <http://example.org/neg1> <http://example.org/neg2> <http://example.org/neg3> } ?example <http://example.org/role1> ?x0 . ?x0 <http://example.org/role2> ?x1 . FILTER NOT EXISTS { ?x1 <http://example.org/role3> ?x2 . ?x2 a ?class . } ?class a <http://www.w3.org/2002/07/owl#Class> . }";
//         query = " SELECT DISTINCT ?s ?p ?o FROM <http://ex.org/model> WHERE { ?s ?p ?o . }";

        try (QueryExecutionFactoryDataset qef = new QueryExecutionFactoryDataset(dataset);
                QueryExecution qe = qef.createQueryExecution(query);) {
            ResultSet result = qe.execSelect();
            while (result.hasNext()) {
                System.out.println(result.next());
            }
            result.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return testCases;
    }
}
