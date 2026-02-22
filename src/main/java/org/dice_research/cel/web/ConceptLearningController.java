package org.dice_research.cel.web;

import java.util.Arrays;
import java.util.List;

import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.cel.ConceptLearner;
import org.dice_research.cel.DescriptionLogic;
import org.dice_research.cel.GTDL;
import org.dice_research.cel.PruneCEL;
import org.dice_research.cel.SimpleRecursivePruneCEL;
import org.dice_research.cel.expression.NamedClass;
import org.dice_research.cel.expression.OntolearnCompatibleSerializer;
import org.dice_research.cel.expression.ScoredClassExpression;
import org.dice_research.cel.refine.suggest.sparql.SparqlBasedSuggestor;
import org.dice_research.cel.refine.suggest.sparql.SparqlBasedSuggestorWithInstances;
import org.dice_research.cel.score.AvoidingPickySolutionsDecorator;
import org.dice_research.cel.score.F1MeasureCalculator;
import org.dice_research.cel.score.LengthBasedRefinementScorer;
import org.dice_research.cel.score.ScoreCalculatorFactory;
import org.dice_research.cel.strategy.AllNodesFeatureSelectionStrategy;
import org.dice_research.cel.strategy.SimpleDirtyLeafNodeClassifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;

@RestController
public class ConceptLearningController {

    @GetMapping("/")
    public String index() {
        return "Concept learner web service is running.";
    }

    @PostMapping(path = "/learn", consumes = { "application/json" }, produces = { "application/json" })
    public ConceptLearningResponse learn(@RequestBody ConceptLearningRequest request) {
        ScoreCalculatorFactory scorerFactory = new F1MeasureCalculator.Factory();
        scorerFactory = new LengthBasedRefinementScorer.Factory(scorerFactory);
        scorerFactory = new AvoidingPickySolutionsDecorator.Factory(scorerFactory);
        ConceptLearner learner;

        if (Strings.isNullOrEmpty(request.endpoint)) {
            throw new IllegalArgumentException("Undefined SPARQL endpoint.");
        }

        SparqlBasedSuggestorWithInstances suggestorInst = null;
        SparqlBasedSuggestor suggestorExt = null;
        try {
            if (request.model.startsWith("BloomCL")) {
                DescriptionLogic logic = DescriptionLogic.parse("ALC");
                suggestorInst = SparqlBasedSuggestorWithInstances.create(request.getEndpoint(), logic);
                suggestorInst.addToClassBlackList(Arrays.asList(OWL.Thing.getURI(), OWL2.NamedIndividual.getURI()));
                suggestorInst.addToPropertyBlackList(RDF.type.getURI());

                double relCardThreshold = getProvidedThreshold(request.model, GTDL.DEFAULT_MIN_RELATIVE_CARDINALITY);

                GTDL gtdlLearner = new GTDL(suggestorInst, logic, scorerFactory, GTDL.DEFAULT_MIN_ABSOLUTE_CARDINALITY,
                        relCardThreshold);
                gtdlLearner.setMaxTime(request.getMax_runtime() * 1000);
                gtdlLearner.setMaxIterations(request.getIter_bound());
                if (request.model.contains("-A")) {
                    AllNodesFeatureSelectionStrategy strategy = new AllNodesFeatureSelectionStrategy(
                            new SimpleDirtyLeafNodeClassifier(GTDL.DEFAULT_MIN_ABSOLUTE_CARDINALITY, relCardThreshold));
                    gtdlLearner.setStrategy(strategy);
                }

                learner = gtdlLearner;
            } else if (request.model.startsWith("PruneCEL")) {
                // Integrate Java implementation of PruneCEL
                DescriptionLogic logic = DescriptionLogic.parse("ALC");
                ScoreCalculatorFactory factory = new F1MeasureCalculator.Factory();

                suggestorExt = SparqlBasedSuggestor.create(request.getEndpoint(), logic, true);
                suggestorExt.addToClassBlackList(Arrays.asList(OWL.Thing.getURI(), OWL2.NamedIndividual.getURI()));
                suggestorExt.addToPropertyBlackList(RDF.type.getURI());

                // PruneCEL-R
                PruneCEL pruneCEL = new SimpleRecursivePruneCEL(suggestorExt, logic, factory, suggestorExt);
                pruneCEL.setMaxTime(request.getMax_runtime() * 1000);
                pruneCEL.setMaxIterations(request.getIter_bound());
                // PruneCEL-S
                pruneCEL.setSkipNonImprovingStmts(true);

                learner = pruneCEL;
            } else {
                throw new IllegalArgumentException("Unknown learner \"" + request.model + "\" requested.");
            }

            List<ScoredClassExpression> expressions = learner.findClassExpression(Arrays.asList(request.getPos()),
                    Arrays.asList(request.getNeg()), null, null);

            if (expressions.size() > 0) {
                OntolearnCompatibleSerializer serializer = new OntolearnCompatibleSerializer();
                return new ConceptLearningResponse(
                        serializer.getSerialization(expressions.get(0).getClassExpression()));
            } else {
                return new ConceptLearningResponse(NamedClass.TOP.getName());
            }
        } finally {
            if (suggestorInst != null) {
                try {
                    suggestorInst.close();
                } catch (Exception e) {
                    // We ignore this
                }
            }
            if (suggestorExt != null) {
                try {
                    suggestorExt.close();
                } catch (Exception e) {
                    // We ignore this
                }
            }
        }
    }

    public double getProvidedThreshold(String modelName, double defaultValue) {
        try {
            String splittedName[] = modelName.split("-");
            return Double.parseDouble(splittedName[splittedName.length - 1]);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
