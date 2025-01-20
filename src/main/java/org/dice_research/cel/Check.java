package org.dice_research.cel;

import java.util.Collections;

import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.cel.expression.ClassExpression;
import org.dice_research.cel.expression.parse.CEParser;
import org.dice_research.cel.refine.suggest.sparql.SparqlBasedSuggestor;

public class Check {

    public static void main(String[] args) throws Exception {
        // QALD9 DBpedia
        String endpoint = "http://dice-quan.cs.uni-paderborn.de:9050/sparql";
        // QALD9-plus-wikidata
        // String endpoint = "http://dice-quan.cs.uni-paderborn.de:9070/sparql";
        // QALD 10
//        String endpoint = "http://dice-quan.cs.uni-paderborn.de:9080/sparql";
        DescriptionLogic logic = DescriptionLogic.parse("ALC");

//        String questions[] = new String[] { "https://github.com/KGQA/QALD-10/blob/main/data/qald_10/qald_10.json#Q10",
//                "https://github.com/KGQA/QALD-10/blob/main/data/qald_10/qald_10.json#Q111",
//                "https://github.com/KGQA/QALD-10/blob/main/data/qald_10/qald_10.json#Q113",
//                "https://github.com/KGQA/QALD-10/blob/main/data/qald_10/qald_10.json#Q150",
//                "https://github.com/KGQA/QALD-10/blob/main/data/qald_10/qald_10.json#Q231",
//                "https://github.com/KGQA/QALD-10/blob/main/data/qald_10/qald_10.json#Q85",
//                "https://github.com/KGQA/QALD-10/blob/main/data/qald_10/qald_10.json#Q32",
//                "https://github.com/KGQA/QALD-10/blob/main/data/qald_10/qald_10.json#Q2",
//                "https://github.com/KGQA/QALD-10/blob/main/data/qald_10/qald_10.json#Q94",
//                "https://github.com/KGQA/QALD-10/blob/main/data/qald_10/qald_10.json#Q38" };
//        String questions[] = new String[] {
//                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q163",
//                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q198",
//                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q32",
//                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q162",
//                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q160",
//                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q27",
//                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q122",
//                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q103",
//                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q26",
//                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q68" };
        String questions[] = new String[] {
                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q122",
                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q29",
                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q141",
                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q135",
                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q132",
                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q32",
                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q156",
                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q203",
                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q111",
                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q149",
                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q115",
                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q190",
                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q201",
                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q148",
                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q143",
                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q157",
                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q175",
                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q145",
                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q14",
                "https://github.com/KGQA/QALD_9_plus/blob/main/data/qald_9_plus/qald_9_plus_dbpedia.json#Q59" };

        ClassExpression ce = (new CEParser()).parse(
                "∃http://w3id.org/dice-research/qa-bench#hasIRIAnswer.((¬http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#Agent⊓∃http://dbpedia.org/ontology/parentMountainPeak.⊤)⊔http://www.wikidata.org/entity/Q41176)⊔(∃http://w3id.org/dice-research/qa-bench#hasIRIAnswer.(http://dbpedia.org/ontology/Astronaut⊔(¬http://www.wikidata.org/entity/Q24229398⊓¬http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing))⊓∃http://w3id.org/dice-research/qa-bench#hasQuestionWord.⊤)");
//        ClassExpression ce = (new CEParser()).parse(
//                "(∃http://w3id.org/dice-research/qa-bench#hasQuery.∀http://w3id.org/dice-research/qa-bench#hasEntity.∃http://dbpedia.org/ontology/thumbnail.⊤⊓(∃http://w3id.org/dice-research/qa-bench#hasLiteralAnswer.⊤⊔∃http://w3id.org/dice-research/qa-bench#hasIRIAnswer.(http://dbpedia.org/ontology/Building⊔(∃http://www.w3.org/2000/01/rdf-schema#seeAlso.⊤⊓http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#NaturalPerson))))");
//        ClassExpression ce = (new CEParser()).parse(
//                "∃http://w3id.org/dice-research/qa-bench#hasIRIAnswer.(http://www.wikidata.org/entity/Q11424⊔http://www.wikidata.org/entity/Q482994⊔∃http://www.wikidata.org/prop/direct/P19.⊤⊔http://www.wikidata.org/entity/Q8502⊔http://www.wikidata.org/entity/Q11691⊔http://www.wikidata.org/entity/Q383092⊔http://www.wikidata.org/entity/Q476028⊔http://www.wikidata.org/entity/Q210167⊔http://www.wikidata.org/entity/Q1637706⊔(∃http://www.wikidata.org/prop/direct/P460.⊤⊓∃http://www.wikidata.org/prop/direct/P279.⊤))");

        try (SparqlBasedSuggestor suggestor = SparqlBasedSuggestor.create(endpoint, logic, false)) {
            suggestor.addToClassBlackList(OWL2.NamedIndividual.getURI());
            suggestor.addToPropertyBlackList(RDF.type.getURI());

            for (int i = 0; i < questions.length; ++i) {
                System.out.print(questions[i]);
                System.out.print(" -> ");
                System.out.println(check(ce, questions[i], suggestor));
            }
        }
    }

    public static boolean check(ClassExpression expression, String questionIRI, SparqlBasedSuggestor suggestor) {
        return suggestor.scoreExpression(expression, Collections.singleton(questionIRI),
                Collections.EMPTY_SET).posCount > 0;
    }
}
