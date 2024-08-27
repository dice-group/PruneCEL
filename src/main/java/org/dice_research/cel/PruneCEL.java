package org.dice_research.cel;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.collections.SetUtils;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.cel.expression.ClassExpression;
import org.dice_research.cel.expression.Junction;
import org.dice_research.cel.expression.NamedClass;
import org.dice_research.cel.expression.ScoredCEComparatorForRefinement;
import org.dice_research.cel.expression.ScoredClassExpression;
import org.dice_research.cel.io.IntermediateResultPrinter;
import org.dice_research.cel.io.LearningProblem;
import org.dice_research.cel.io.csv.CSVIntermediateResultPrinter;
import org.dice_research.cel.io.json.JSONLearningProblemReader;
import org.dice_research.cel.refine.RefinementOperator;
import org.dice_research.cel.refine.SuggestorBasedRefinementOperator;
import org.dice_research.cel.refine.suggest.ExtendedSuggestor;
import org.dice_research.cel.refine.suggest.SelectionScores;
import org.dice_research.cel.refine.suggest.SparqlBasedSuggestor;
import org.dice_research.cel.score.AvoidingPickySolutionsDecorator;
import org.dice_research.cel.score.BalancedAccuracyCalculator;
import org.dice_research.cel.score.LengthBasedRefinementScorer;
import org.dice_research.cel.score.ScoreCalculator;
import org.dice_research.cel.score.ScoreCalculatorFactory;
import org.dice_research.cel.sparql.InstanceRetriever;
import org.dice_research.topicmodeling.commons.collections.TopDoubleObjectCollection;
import org.dice_research.topicmodeling.commons.collections.TopIntObjectCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PruneCEL {

    private static final Logger LOGGER = LoggerFactory.getLogger(PruneCEL.class);

    protected ExtendedSuggestor suggestor;
    protected DescriptionLogic logic;
    protected ScoreCalculatorFactory calculatorFactory;
    protected int maxIterations = 0;
    protected long maxTime = 0L;
    protected boolean skipNonImprovingStmts = false;

    protected boolean recursiveProblemSolving = false;
    protected InstanceRetriever retriever = null;
    protected double precisionThreshold = 1.0;
    protected double timeForRecursiveIteration = 0.5;

    public PruneCEL(ExtendedSuggestor suggestor, DescriptionLogic logic, ScoreCalculatorFactory calculatorFactory) {
        super();
        this.suggestor = suggestor;
        this.logic = logic;
        this.calculatorFactory = calculatorFactory;
    }

    public void activateRecursiveIteration(InstanceRetriever retriever, double precisionThreshold,
            double timeForRecursiveIteration) {
        this.recursiveProblemSolving = true;
        this.retriever = retriever;
        this.precisionThreshold = precisionThreshold;
        this.timeForRecursiveIteration = timeForRecursiveIteration;
    }

    public List<ScoredClassExpression> findClassExpression(Collection<String> positive, Collection<String> negative) {
        return findClassExpression(positive, negative, null, null);
    }

    public List<ScoredClassExpression> findClassExpression(Collection<String> positive, Collection<String> negative,
            OutputStream logStream, IntermediateResultPrinter iResultPrinter) {
        long startTime = System.currentTimeMillis();
        long timeToStop = startTime + maxTime;
        return findClassExpression(positive, negative, logStream, iResultPrinter, startTime, timeToStop);
    }

    public List<ScoredClassExpression> findClassExpression(Collection<String> positive, Collection<String> negative,
            OutputStream logStream, IntermediateResultPrinter iResultPrinter, long startTime, long timeToStop) {
        TopDoubleObjectCollection<ScoredClassExpression> topExpressions = new TopDoubleObjectCollection<>(10, false);
        Set<ScoredClassExpression> seenExpressions = new HashSet<>();
        Queue<ScoredClassExpression> queue = new PriorityQueue<ScoredClassExpression>(
                new ScoredCEComparatorForRefinement());
        ScoreCalculator scoreCalculator = calculatorFactory.create(positive.size(), negative.size());
        double perfectScore = scoreCalculator.getPerfectScore();

        TopIntObjectCollection<ScoredClassExpression> mostPreciseExpressions = new TopIntObjectCollection<>(10, false);

        RefinementOperator rho = new SuggestorBasedRefinementOperator(suggestor, logic, scoreCalculator, positive,
                negative);
        ((SuggestorBasedRefinementOperator) rho).setLogStream(logStream);

        Collection<ScoredClassExpression> newExpressions;
        ScoredClassExpression nextBestExpression;
        // Create Top expression
        nextBestExpression = scoreCalculator.score(NamedClass.TOP, positive.size(), negative.size(), false);
        queue.add(nextBestExpression);
        topExpressions.add(nextBestExpression.getClassificationScore(), nextBestExpression);
        int iterationCount = 0;
        long timeToCallRecursion = startTime + (maxTime - (long) (timeForRecursiveIteration * maxTime));
        // Iterate over the queue while
        while (// 1. the queue is not empty
        !queue.isEmpty() &&
        // 2. the best expression has not the perfect score
                (topExpressions.values.length == 0 || topExpressions.values[0] < perfectScore) &&
                // 3. We haven't reached the maximum number of iterations (if defined)
                (maxIterations == 0 || iterationCount < maxIterations) &&
                // 4. We haven't reached the maximum amount of time that we are allowed to use
                (maxTime == 0 || (System.currentTimeMillis() < timeToStop))) {
            nextBestExpression = queue.poll();
            LOGGER.info("Refining rScore={}, cScore={}, ce={}", nextBestExpression.getRefinementScore(),
                    nextBestExpression.getClassificationScore(), nextBestExpression.getClassExpression());
            // Refine this expression
            newExpressions = rho.refine(nextBestExpression);
            // Check the expressions
            for (ScoredClassExpression newExpression : newExpressions) {
                // If 1) we haven't seen this before AND 2a) we are configured to not further
                // look into this OR 2b) the classification score of this
                // new expression is better than the expression that we refined OR 2c) we
                // extended the previous expression by adding an edge
                if (seenExpressions.add(newExpression) && (!skipNonImprovingStmts
                        || (nextBestExpression.getClassificationScore() < newExpression.getClassificationScore())
                        || (newExpression.isAddedEdge()))) {
                    queue.add(newExpression);
                    topExpressions.add(newExpression.getClassificationScore(), newExpression);
                    if (recursiveProblemSolving && (getPrecision(newExpression) >= precisionThreshold)) {
                        mostPreciseExpressions.add(newExpression.getPosCount(), newExpression);
                    }
                }
            }
            iterationCount++;
            if (recursiveProblemSolving && (System.currentTimeMillis() > timeToCallRecursion)
                    && (mostPreciseExpressions.size() > 0)) {
                // Try recursion
                ScoredClassExpression mostPrecise = (ScoredClassExpression) mostPreciseExpressions.getObjects()[0];
                LOGGER.info("Removing positives selected by {}", mostPrecise);
                Set<String> remainingPositives = new HashSet<String>(positive);
                @SuppressWarnings("unchecked")
                Set<String> selectedInstances = retriever.retrieveInstances(mostPrecise.getClassExpression(), positive,
                        SetUtils.EMPTY_SET);
                remainingPositives.removeAll(selectedInstances);
                LOGGER.info("Trying to solve the remaining sub problem recursively for the remaining {} positives ...",
                        remainingPositives.size());
                List<ScoredClassExpression> subProblemResults = findClassExpression(remainingPositives, negative,
                        logStream, iResultPrinter, timeToCallRecursion, timeToStop);
                if (subProblemResults.size() > 0) {
                    ClassExpression newExpression = new Junction(false, mostPrecise.getClassExpression(),
                            subProblemResults.get(0).getClassExpression());
                    SelectionScores scores = suggestor.scoreExpression(newExpression, positive, negative);
                    ScoredClassExpression scoredExp = scoreCalculator.score(newExpression, scores.getPosCount(),
                            scores.getNegCount(), false);
                    topExpressions.add(scoredExp.getClassificationScore(), scoredExp);
                }
            }
        }
        LOGGER.info("Stopping search. Saw {} expressions.", seenExpressions.size());
        return Stream.of(topExpressions.getObjects()).map(o -> (ScoredClassExpression) o).toList();
    }

    /**
     * @return the maxIterations
     */
    public int getMaxIterations() {
        return maxIterations;
    }

    /**
     * @param maxIterations the maxIterations to set
     */
    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    /**
     * @return the maxTime
     */
    public long getMaxTime() {
        return maxTime;
    }

    /**
     * @param maxTime the maxTime to set
     */
    public void setMaxTime(long maxTime) {
        this.maxTime = maxTime;
    }

    /**
     * @return the skipNonImprovingStmts
     */
    public boolean isSkipNonImprovingStmts() {
        return skipNonImprovingStmts;
    }

    /**
     * @param skipNonImprovingStmts the skipNonImprovingStmts to set
     */
    public void setSkipNonImprovingStmts(boolean skipNonImprovingStmts) {
        this.skipNonImprovingStmts = skipNonImprovingStmts;
    }

    protected double getPrecision(ScoredClassExpression ce) {
        // Avoid division by 0
        if (ce.getPosCount() == 0) {
            return 0;
        } else {
            return ce.getPosCount() / (ce.getPosCount() + ce.getNegCount());
        }
    }

    public static void main(String[] args) throws Exception {
        // XXX Set SPARQL endpoint
        // String endpoint = "http://localhost:9080/sparql";
//        String endpoint = "http://localhost:3030/exp-bench/sparql";
        String endpoint = "http://localhost:3030/family/sparql";
        // XXX Set description logic
        DescriptionLogic logic = DescriptionLogic.parse("ALC");

        ScoreCalculatorFactory factory = null;
        // XXX Choose either F1 or balanced accuracy
        // factory = new F1MeasureCalculator.Factory();
        factory = new BalancedAccuracyCalculator.Factory();

        // Punish long expressions
        factory = new LengthBasedRefinementScorer.Factory(factory);
        // XXX (Optional) avoid choosing solutions that work only for a single example
        factory = new AvoidingPickySolutionsDecorator.Factory(factory);

        try (SparqlBasedSuggestor suggestor = SparqlBasedSuggestor.create(endpoint, logic)) {
            suggestor.addToClassBlackList(OWL2.NamedIndividual.getURI());
            suggestor.addToPropertyBlackList(RDF.type.getURI());

            boolean printLogs = false;

            // XXX We should use PruneCEL for now, you can try Recursive later
//            PruneCEL cel = new PruneCEL(suggestor, logic, factory);
            //PruneCEL cel = new RecursivePruneCEL(suggestor, logic, factory, suggestor);
            //PruneCEL cel = new SingleThreadRecursivePruneCEL(suggestor, logic, factory, suggestor);
            PruneCEL cel = new SimpleRecursivePruneCEL(suggestor, logic, factory, suggestor);
            // XXX Max iterations of the refinement
            cel.setMaxIterations(1000);
            // XXX Maximum time (in ms)
            cel.setMaxTime(60000);
            // XXX (Optional) try to avoid refining expressions that have not been created
            // in a promising way (i.e., just added a class to an existing expression
            // without changing the accuracy of the expression)
            cel.setSkipNonImprovingStmts(true);
            // XXX Keep this commented for now
            // cel.activateRecursiveIteration(suggestor, 1.0, 0.5);

            // DEBUG CODE!!!
//            ClassExpression ce = new Junction(false,
//                    new SimpleQuantifiedRole(false, "http://quans-namespace.org/#HasLiteralAns", false,
//                NamedClass.BOTTOM),
//                            new Junction(true, Suggestor.CONTEXT_POSITION_MARKER,
//                                    new NamedClass("http://quans-namespace.org/#QUESTION")));
//            suggestor.suggestClass(positives.get(0), negatives.get(0), ce);
            // DEBUG CODE END!!!

            // XXX Choose the learning problem (as JSON file)
            JSONLearningProblemReader reader = new JSONLearningProblemReader();
            Collection<LearningProblem> problems = reader.readProblems("LPs/Family/lps.json");

            try (PrintStream pout = new PrintStream("results.txt")) {
//                for (int i = 0; i < names.size(); ++i) {
                for (LearningProblem problem : problems) {
                    if (printLogs) {
                        try (OutputStream logStream = new BufferedOutputStream(
                                new FileOutputStream(problem.getName() + ".log"));
                                CSVIntermediateResultPrinter irp = new CSVIntermediateResultPrinter(
                                        new PrintStream(problem.getName() + ".csv"))) {
                            runSearch(problem.getName(), problem.getPositiveExamples(), problem.getNegativeExamples(),
                                    cel, pout, logStream, irp);
                        }
                    } else {
                        runSearch(problem.getName(), problem.getPositiveExamples(), problem.getNegativeExamples(), cel,
                                pout, null, null);
                    }
                }
//                }
            }

//            long time = System.currentTimeMillis();
//            List<ScoredClassExpression> expressions = cel.findClassExpression(positive, negative);
//            time = System.currentTimeMillis() - time;
//            System.out.print("Result after ");
//            System.out.print(time);
//            System.out.println("ms:");
//            for (ScoredClassExpression exp : expressions) {
//                System.out.println(exp.toString());
//            }
        }
    }

    public static void runSearch(String name, List<String> positive, List<String> negative, PruneCEL cel,
            PrintStream pout, OutputStream logStream, IntermediateResultPrinter iResultPrinter) {
        System.out.println("Starting " + name);
        long time = System.currentTimeMillis();
        List<ScoredClassExpression> expressions = cel.findClassExpression(positive, negative, logStream, iResultPrinter);
        time = System.currentTimeMillis() - time;
        printClassExpressions(expressions, name, time, pout);
    }

    public static void printClassExpressions(List<ScoredClassExpression> expressions, String name, long runtime,
            PrintStream pout) {
        pout.print("Result");
        if (name != null) {
            pout.print(" for ");
            pout.print(name);
        }
        pout.print(" after ");
        pout.print(runtime);
        pout.println("ms:");
        for (ScoredClassExpression exp : expressions) {
            pout.println(exp.toString());
        }
    }
}
