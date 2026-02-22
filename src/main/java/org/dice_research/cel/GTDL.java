package org.dice_research.cel;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.jenax.arq.connection.core.QueryExecutionFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sys.JenaSystem;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.cel.data.LearningProblemSatistics;
import org.dice_research.cel.expression.ClassExpression;
import org.dice_research.cel.expression.NamedClass;
import org.dice_research.cel.expression.ScoredClassExpression;
import org.dice_research.cel.io.IntermediateResultPrinter;
import org.dice_research.cel.io.LearningProblem;
import org.dice_research.cel.io.csv.CSVIntermediateResultPrinter;
import org.dice_research.cel.io.json.JSONLearningProblemReader;
import org.dice_research.cel.refine.instances.InstanceSuggestorBasedRefinementOperator;
import org.dice_research.cel.refine.instances.RefinementOperatorWithInstances;
import org.dice_research.cel.refine.instances.ScoredClassExpressionWithInstances;
import org.dice_research.cel.refine.instances.ScoredIRIWithInstances;
import org.dice_research.cel.refine.suggest.sparql.SparqlBasedSuggestorWithInstances;
import org.dice_research.cel.score.F1MeasureCalculator;
import org.dice_research.cel.score.LengthBasedRefinementScorer;
import org.dice_research.cel.score.ScoreCalculator;
import org.dice_research.cel.score.ScoreCalculatorFactory;
import org.dice_research.cel.strategy.FeatureSelectionStrategy;
import org.dice_research.cel.strategy.SimpleDirtyLeafNodeClassifier;
import org.dice_research.cel.strategy.SingleNodeFeatureSelectionStrategy;
import org.dice_research.cel.tree.DecisionTreeNode;
import org.dice_research.cel.tree.Feature;
import org.dice_research.cel.tree.TreeTransformer;
import org.dice_research.cel.tree.UpdatingDecisionTreeLearner;
import org.dice_research.cel.tree.select.AdditionallySelectedElementsComparator;
import org.dice_research.cel.tree.select.BiggestPureSetComparator;
import org.dice_research.cel.tree.select.ClassExpressionLengthComparator;
import org.dice_research.cel.tree.select.FeatureSelectionFilter;
import org.dice_research.cel.tree.select.FeatureSelector;
import org.dice_research.cel.tree.select.GenericFeatureSelector;
import org.dice_research.cel.tree.select.MinGiniIndexBasedComparator;
import org.dice_research.cel.tree.select.MinNodeSizeFeaturesFilter;
import org.dice_research.cel.tree.select.NoneffectiveFeaturesFilter;
import org.dice_research.cel.tree.select.SelectedExamplesCountComparator;
import org.dice_research.cel.tree.select.SelectedPositiveExamplesComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javolution.util.FastBitSet;

public class GTDL extends AbstractConceptLearner {

    public static final int DEFAULT_MIN_ABSOLUTE_CARDINALITY = 2;
    public static final double DEFAULT_MIN_RELATIVE_CARDINALITY = 0.0;

    private static final Logger LOGGER = LoggerFactory.getLogger(GTDL.class);

    protected SparqlBasedSuggestorWithInstances suggestor;
    protected DescriptionLogic logic;
    protected ScoreCalculatorFactory calculatorFactory;
    protected FeatureSelectionStrategy strategy;
    protected UpdatingDecisionTreeLearner<ScoredClassExpressionWithInstances> learner;
//    protected DecisionTreeLearner<ScoredClassExpressionWithInstances> learner = new DecisionTreeLearner<ScoredClassExpressionWithInstances>(
//            new GenericFeatureSelector<ScoredClassExpressionWithInstances>(new NoneffectiveFeaturesFilter(),
//                    new MinGiniIndexBasedComparator(), new BiggestPureSetComparator(),
//                    new ClassExpressionLengthComparator(), new SelectedPositiveExamplesComparator()));
    @SuppressWarnings("unchecked")
    protected FeatureSelector<ScoredClassExpressionWithInstances> refinementFeatureSelector = new GenericFeatureSelector<ScoredClassExpressionWithInstances>(
            new SelectedExamplesCountComparator(), new ClassExpressionLengthComparator(),
            new AdditionallySelectedElementsComparator());
    protected boolean debugMode = false;

    public GTDL(SparqlBasedSuggestorWithInstances suggestor, DescriptionLogic logic,
            ScoreCalculatorFactory calculatorFactory) {
        this(suggestor, logic, calculatorFactory, DEFAULT_MIN_ABSOLUTE_CARDINALITY, DEFAULT_MIN_RELATIVE_CARDINALITY);
    }

    @SuppressWarnings("unchecked")
    public GTDL(SparqlBasedSuggestorWithInstances suggestor, DescriptionLogic logic,
            ScoreCalculatorFactory calculatorFactory, int minAbsoluteCardinality, double minRelativeCardinality) {
        super();
        this.suggestor = suggestor;
        logic = (DescriptionLogic) logic.clone();
        logic.setSupportsComplexConceptNegation(false);
        logic.setSupportsConceptUnion(false);
        logic.setSupportsConceptIntersection(false);
        logic.setSupportsAtomicNegation(false); // TODO double check whether that is correct, but it seems like it
                                                // introduces issues and we can get negations of single elements from
                                                // the combination of complex expressions.
        this.logic = logic;
        this.calculatorFactory = calculatorFactory;
        this.strategy = new SingleNodeFeatureSelectionStrategy(
                new SimpleDirtyLeafNodeClassifier(minAbsoluteCardinality, minRelativeCardinality));

        this.learner = new UpdatingDecisionTreeLearner<ScoredClassExpressionWithInstances>(
                new GenericFeatureSelector<ScoredClassExpressionWithInstances>(
                        Arrays.asList(new FeatureSelectionFilter[] { new NoneffectiveFeaturesFilter(),
                                new MinNodeSizeFeaturesFilter(minAbsoluteCardinality, minRelativeCardinality) }),
                        new MinGiniIndexBasedComparator(), new BiggestPureSetComparator(),
                        new ClassExpressionLengthComparator(), new SelectedPositiveExamplesComparator()));
    }

    @Override
    public List<ScoredClassExpression> findClassExpression(Collection<String> positive, Collection<String> negative,
            OutputStream logStream, IntermediateResultPrinter iResultPrinter, long startTime, long timeToStop) {
        Map<String, Integer> positiveMaps = toMap(positive);
        Map<String, Integer> negativeMaps = toMap(negative);
        LearningProblemSatistics lpStats = new LearningProblemSatistics(positiveMaps.size(), negativeMaps.size());

//        @SuppressWarnings("unchecked")
//        DecisionTreeLearner<ScoredClassExpressionWithInstances> learner = new DecisionTreeLearner<ScoredClassExpressionWithInstances>(
//                new GenericFeatureSelector<ScoredClassExpressionWithInstances>(new GiniIndexBasedComparator(),
//                        new BiggestPureSetComparator(), new ClassExpressionLengthComparator()));

        ScoreCalculator scoreCalculator = calculatorFactory.create(lpStats.numberOfPositives,
                lpStats.numberOfNegatives);
        RefinementOperatorWithInstances rho = new InstanceSuggestorBasedRefinementOperator(suggestor, logic,
                scoreCalculator, positive, negative, positiveMaps, negativeMaps);
        ((InstanceSuggestorBasedRefinementOperator) rho).setLogStream(logStream);
        ((InstanceSuggestorBasedRefinementOperator) rho).setDebugMode(debugMode);

        Set<ScoredClassExpressionWithInstances> seenCEs = new HashSet<>();
        // The class expressions which have been refined before, excluding some standard
        // concepts that won't be helpful
        Set<ClassExpression> refinedCEs = new HashSet<>();
//                Arrays.asList(new NamedClass(OWL2.Thing.getURI()), new NamedClass(OWL2.NamedIndividual.getURI())));
        seenCEs.addAll(filterRefinementResults(rho.refine(NamedClass.TOP, timeToStop), lpStats.numberOfExamples));
        List<ScoredClassExpressionWithInstances> newlyCreatedFeatures = new ArrayList<>();

        DecisionTreeNode root = null;
        int iterationCount = 0;
        int refinedCount = 0;
        boolean needsMoreRefinement = true;
        Set<ScoredClassExpressionWithInstances> newExpressions;
        do {
            // Update the tree if newly created features suggest to do so
            root = learner.update(root, seenCEs, newlyCreatedFeatures, lpStats);

            // Output the structure of the trained tree for inspection (this part is just a
            // simple example).
            if (debugMode)
                printTree(root);

            Feature[] selectionPatterns = strategy.determinePatterns(root, lpStats);
            needsMoreRefinement = selectionPatterns.length > 0;

            if (needsMoreRefinement) {
                if (LOGGER.isDebugEnabled()) {
                    int pos = 0;
                    int neg = 0;
                    for (int i = 0; i < selectionPatterns.length; ++i) {
                        pos += selectionPatterns[i].getSelectedPositives().cardinality();
                        neg += selectionPatterns[i].getSelectedNegatives().cardinality();
                    }
                    LOGGER.debug("Solution has issues with separating {} positives from {} negatives.", pos, neg);
                }

                ScoredClassExpressionWithInstances[] classExp4Ref = determineClassExpressions(selectionPatterns,
                        seenCEs, refinedCEs, lpStats);
                refinedCount = 0;
                newlyCreatedFeatures.clear();
                for (ScoredClassExpressionWithInstances classExp : classExp4Ref) {
                    newExpressions = rho.refine(classExp.getClassExpression(), timeToStop);
                    ++refinedCount;
//                LOGGER.info("*** Refinement of {} ***", classExp.getClassExpression());
                    newlyCreatedFeatures.addAll(newExpressions);
                }
                seenCEs.addAll(newlyCreatedFeatures);
                if (refinedCount == 0) {
                    LOGGER.warn(
                            "Couldn't get any more suggestions. Either the learner is not good enough or the data is limited in its expressiveness.");
                    // TODO at this point, we may have to train the tree one last time, in case we
                    // can still improve the length of the expression compared to the currently
                    // existing tree...
                }
            }
            iterationCount++;
        } while (
        // 1. We are not done
        needsMoreRefinement && (refinedCount > 0) &&
        // 2. We haven't reached the maximum number of iterations (if defined)
                (maxIterations == 0 || iterationCount < maxIterations) &&
                // 2. We haven't reached the maximum amount of time that we are allowed to use
                (maxTime == 0 || (System.currentTimeMillis() < timeToStop)));
        if (debugMode)
            printTree(root);
        ScoredClassExpression result = transformTree(root, positiveMaps.size(), negativeMaps.size());
        // If TOP would be a better result, return TOP
        double topScore = scoreCalculator.calculateClassificationScore(positive.size(), negative.size());
        if (topScore > result.getClassificationScore()) {
            result = scoreCalculator.score(NamedClass.TOP, positive.size(), negative.size(), false);
        }
        return Arrays.asList(result);
    }

    /**
     * Method to filter the refinement result. Equivalent to the "strict"
     * configuration of PruneCEL.
     * 
     * @param refine
     * @param numberOfExamples
     * @return
     */
    private Collection<? extends ScoredClassExpressionWithInstances> filterRefinementResults(
            Set<? extends ScoredClassExpressionWithInstances> refine, final int numberOfExamples) {
        return refine.stream().filter(s -> filterRefinementResult(s, numberOfExamples)).toList();
    }

    private boolean filterRefinementResult(ScoredClassExpressionWithInstances scoredCE, int numberOfExamples) {
        int selectedSum = scoredCE.getPosCount() + scoredCE.getNegCount();
        return scoredCE.isAddedEdge() || ((selectedSum > 0) && (selectedSum < numberOfExamples));
    }

    protected ScoredClassExpression transformTree(DecisionTreeNode root, int numOfPositives, int numOfNegatives) {
        TreeTransformer transformer = new TreeTransformer();
        return transformer.transformTree(root, calculatorFactory.create(numOfPositives, numOfNegatives));
        // TODO iterate over the tree
        // TODO Collect
        // 1) number of positive and negative elements in the tree (based on the leave
        // nodes)
        // 2) number of correctly classified examples (based on the leave nodes)
        // 3) the class expression
        // calculatorFactory.create(, ).score(null, iterationCount, iterationCount,
        // false);
        // return calculatorFactory.create(1, 1).score(NamedClass.TOP, 1, 1, false);
    }

    public void setStrategy(FeatureSelectionStrategy strategy) {
        this.strategy = strategy;
    }

    static {
        JenaSystem.init();
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            LOGGER.error("Wrong usage! Bloom-CL <lp-file> <sparql-endpoint> [output-file]");
            return;
        }
//        Thread.sleep(10000);
        long time = System.currentTimeMillis();
//        String datasetName;
        String endpoint = args[1];
        String lpFile = args[0];
        String resultFile = "results.txt";
        if ((args.length > 2) && (!args[2].trim().isEmpty())) {
            resultFile = args[2].trim();
        }
//        int maxFold = 1;

//        datasetName = "Family";
        // datasetName = Premierleague;
        // datasetName = Lymphography;

//        endpoint = "http://localhost:3030/" + datasetName.toLowerCase() + "/sparql";
        // String endpoint = "http://localhost:3030/family/sparql";
        // String endpoint = "http://localhost:3030/premierleague/sparql";
        // String endpoint = "http://localhost:3030/lymphography/sparql";
        // QALD 10
//        String endpoint = "http://131.234.28.27:9080/sparql";
        // QALD 9+DB
//        String endpoint = "http://131.234.28.27:9050/sparql";
//        String endpoint = "http://localhost:3030/imdb10000/sparql";
        long maxRunTime = 30000;// 300000;

//        lpFile = "LPs/" + datasetName + "/lps.json";
        ScoreCalculatorFactory scorerFactory = new F1MeasureCalculator.Factory();

        DescriptionLogic logic = DescriptionLogic.parse("ALC");
//        boolean useCache = true;
//        boolean debugMode = false;
        SparqlBasedSuggestorWithInstances suggestor = SparqlBasedSuggestorWithInstances.create(endpoint, logic);
        suggestor.addToClassBlackList(Arrays.asList(OWL.Thing.getURI(), OWL2.NamedIndividual.getURI()));
        suggestor.addToPropertyBlackList(RDF.type.getURI());

        GTDL conceptLearner = new GTDL(suggestor, logic, scorerFactory);
        conceptLearner.setMaxTime(maxRunTime);

        boolean printLogs = false;
//        conceptLearner.findClassExpression(positive, negative);

        JSONLearningProblemReader reader = new JSONLearningProblemReader();
        Collection<LearningProblem> problems = reader.readProblems(lpFile);

        try (PrintStream pout = new PrintStream(resultFile)) {
            for (LearningProblem problem : problems) {
                if (printLogs) {
                    try (OutputStream logStream = new BufferedOutputStream(
                            new FileOutputStream(problem.getName() + ".log"));
                            CSVIntermediateResultPrinter irp = new CSVIntermediateResultPrinter(
                                    new PrintStream(problem.getName() + ".csv"))) {
                        PruneCEL.runSearch(problem.getName(), problem.getPositiveExamples(),
                                problem.getNegativeExamples(), conceptLearner, pout, logStream, irp);
                    }
                } else {
                    PruneCEL.runSearch(problem.getName(), problem.getPositiveExamples(), problem.getNegativeExamples(),
                            conceptLearner, pout, null, null);
                }
            }
        }
        System.out.println("Seems like I am done (after " + (System.currentTimeMillis() - time) + "ms).");
    }

    protected ScoredClassExpressionWithInstances[] determineClassExpressions(Feature[] selectionPatterns,
            Set<ScoredClassExpressionWithInstances> seenCEs, Set<ClassExpression> refinedCEs,
            LearningProblemSatistics lpStats) {
        List<ScoredClassExpressionWithInstances> results = new ArrayList<>(selectionPatterns.length);
        for (Feature selectionPattern : selectionPatterns) {
            ScoredClassExpressionWithInstances selectedCE = determineClassExpression(selectionPattern, seenCEs,
                    refinedCEs, lpStats);
//            ///// DEBUG
//            ScoredClassExpressionWithInstances selectedCEOld = determineClassExpression_old(selectionPattern, seenCEs,
//                    refinedCEs);
//            System.out.println("New : " + ((selectedCE != null) ? selectedCE.toString() : "null"));
//            System.out.println("Old : " + ((selectedCEOld != null) ? selectedCEOld.toString() : "null"));
//            ///// DEBUG END

            if (selectedCE != null) {
                refinedCEs.add(selectedCE.getClassExpression());
                results.add(selectedCE);
            } else {
                LOGGER.warn("Found no class expression to refine for slection pattern {}.", selectionPattern);
            }
        }
        return results.toArray(ScoredClassExpressionWithInstances[]::new);
    }

    /**
     * This implements a "naive" linear search. This could be done better...
     * 
     * @param selectionPattern
     * @param seenCEs
     * @param refinedCEs
     * @return
     */
    protected static ScoredClassExpressionWithInstances determineClassExpression_old(Feature selectionPattern,
            Set<ScoredClassExpressionWithInstances> seenCEs,
            Set<ClassExpression> refinedCEs/* , ScoreCalculatorFactory scorerFactory */) {
        FastBitSet pos = selectionPattern.getSelectedPositives();
        FastBitSet neg = selectionPattern.getSelectedNegatives();
        ScoredClassExpressionWithInstances chosenCE = null;
        int chosenCECount = 0;
        int chosenCELength = Integer.MAX_VALUE;
        int chosenCEAdditionalCount = Integer.MAX_VALUE;
        int currentCECount;
        int currentCELength;
        int currentCEAdditionalCount;
        FastBitSet cePos;
        FastBitSet ceNeg;
        boolean isCurrentBetter;
        // Iterate over all known CEs
        for (ScoredClassExpressionWithInstances ce : seenCEs) {
            // We only look at expressions that haven't been refined before
            if (!refinedCEs.contains(ce.getClassExpression())) {
                isCurrentBetter = false;
                cePos = ce.getSelectedPositives();
                ceNeg = ce.getSelectedNegatives();
                currentCECount = FastBitSet.andCardinality(pos, cePos) + FastBitSet.andCardinality(neg, ceNeg);
                // If the coverage is at least not worse
                if (currentCECount >= chosenCECount) {
                    isCurrentBetter = currentCECount > chosenCECount;
                    currentCELength = LengthBasedRefinementScorer.getLength(ce.getClassExpression());
                    // If the length is at least not worse
                    if (isCurrentBetter || (currentCELength <= chosenCELength)) {
                        currentCEAdditionalCount = (cePos.cardinality() + ceNeg.cardinality()) - currentCECount;
                        if (isCurrentBetter || (currentCELength < chosenCELength)
                                || (currentCEAdditionalCount < chosenCEAdditionalCount)) {
                            chosenCE = ce;
                            chosenCECount = currentCECount;
                            chosenCELength = currentCELength;
                            chosenCEAdditionalCount = currentCEAdditionalCount;
                        }
                    }
                }
            }
        }
        if (chosenCE == null) {
            LOGGER.info(
                    "Nothing to choose for this node... Maybe I have already chosen all available expressions for previous nodes...");
        } else {
            LOGGER.info("Choosing {} ({}/{} of {}/{}, length={}, additional={})", chosenCE.getClassExpression(),
                    chosenCE.getPosCount(), chosenCE.getNegCount(), pos.cardinality(), neg.cardinality(),
                    chosenCELength, chosenCEAdditionalCount);
        }
        return chosenCE;
    }

    protected ScoredClassExpressionWithInstances determineClassExpression(Feature selectionPattern,
            Set<ScoredClassExpressionWithInstances> seenCEs, Set<ClassExpression> refinedCEs,
            LearningProblemSatistics lpStats) {
        return refinementFeatureSelector.selectBestFeature(
                seenCEs.stream().filter(ce -> !refinedCEs.contains(ce.getClassExpression())),
                selectionPattern.getSelectedPositives(), selectionPattern.getSelectedNegatives(), lpStats);
    }

    public static void printTree(DecisionTreeNode node) {
        System.out.print(node.getPositives().cardinality());
        System.out.print('/');
        System.out.print(node.getNegatives().cardinality());
        if (node.isLeaf()) {
            System.out.print(" class=");
            System.out.println(node.getClassLabel());
        } else {
            System.out.print(" feature=");
            System.out.println(node.getFeature());
            printTree(node.getTrueChild(), "", true);
            printTree(node.getFalseChild(), "", false);
        }
    }

    protected static void printTree(DecisionTreeNode node, String prefix, boolean leftChild) {
        if (node == null)
            return;
//        System.out.println("Feature: " + node.getFeature());
//        System.out
//                .println("Pos / Neg: " + node.getPositives().cardinality() + " / " + node.getNegatives().cardinality());
        System.out.print(prefix);
        System.out.print(leftChild ? '\u251c' : '\u2514');
        System.out.print(node.getPositives().cardinality());
        System.out.print('/');
        System.out.print(node.getNegatives().cardinality());
        if (node.isLeaf()) {
            System.out.print(" class=");
            System.out.println(node.getClassLabel());
        } else {
            System.out.print(" feature=");
            System.out.println(node.getFeature());
            String newPrefix = prefix + (leftChild ? '\u2502' : ' ');
            printTree(node.getTrueChild(), newPrefix, true);
            printTree(node.getFalseChild(), newPrefix, false);
        }
    }

    protected static Map<String, Integer> toMap(Collection<String> examples) {
        int pos = 0;
        Map<String, Integer> result = new HashMap<>();
        for (String example : examples) {
            // We get a collection, so we need to ensure that the examples are not repeated
            if (!result.containsKey(example)) {
                result.put(example, pos);
                ++pos;
            }
        }
        return result;
    }

    @Deprecated
    protected static List<ScoredIRIWithInstances> executeSelection(QueryExecutionFactory queryExecFactory, String query,
            Map<String, Integer> positiveMaps, Map<String, Integer> negativeMaps) {
        List<ScoredIRIWithInstances> results = new ArrayList<>();
        try (QueryExecution execution = queryExecFactory.createQueryExecution(query)) {
            ResultSet rs = execution.execSelect();
            Set<String> examples = new HashSet<>();
            String iri = null;
            String prevIri = "";
            QuerySolution s;
            while (rs.hasNext()) {
                s = rs.next();
                iri = s.getResource("i").getURI();
                if (!prevIri.equals(iri)) {
                    if (examples.size() > 0) {
                        results.add(ScoredIRIWithInstances.create(prevIri, examples, positiveMaps, negativeMaps));
                    }
                    prevIri = iri;
                    examples.clear();
                }
                examples.add(s.getResource("e").getURI());
            }
            if ((examples.size() > 0) && iri != null) {
                results.add(ScoredIRIWithInstances.create(iri, examples, positiveMaps, negativeMaps));
            }
        }
        return results;
    }

}
