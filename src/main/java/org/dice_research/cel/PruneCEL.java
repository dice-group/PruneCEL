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
import org.dice_research.cel.io.JSONLearningProblemReader;
import org.dice_research.cel.io.LearningProblem;
import org.dice_research.cel.refine.RefinementOperator;
import org.dice_research.cel.refine.SuggestorBasedRefinementOperator;
import org.dice_research.cel.refine.suggest.ExtendedSuggestor;
import org.dice_research.cel.refine.suggest.SelectionScores;
import org.dice_research.cel.refine.suggest.SparqlBasedSuggestor;
import org.dice_research.cel.score.*;
import org.dice_research.cel.sparql.InstanceRetriever;
import org.dice_research.topicmodeling.commons.collections.TopDoubleObjectCollection;
import org.dice_research.topicmodeling.commons.collections.TopIntObjectCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PruneCEL {

    private static final Logger LOGGER = LoggerFactory.getLogger(PruneCEL.class);
    private static boolean isHeaderPrinted = false;
    private static ClassExpression currentClassExpression = null;

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
        return findClassExpression(positive, negative, null);
    }

    public List<ScoredClassExpression> findClassExpression(Collection<String> positive, Collection<String> negative,
            OutputStream logStream) {
        long startTime = System.currentTimeMillis();
        long timeToStop = startTime + maxTime;
        return findClassExpression(positive, negative, logStream, startTime, timeToStop);
    }

    public List<ScoredClassExpression> findClassExpression(Collection<String> positive, Collection<String> negative,
            OutputStream logStream, long startTime, long timeToStop) {
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
                        logStream, timeToCallRecursion, timeToStop);
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

        // "http://localhost:3030/Family/sparql"   Family
        // "http://localhost:3030/Mutagenesis/sparql"   Mutagenesis
        // "http://localhost:3030/Carcinogenesis/sparql"   Carcinogenesis
        // "http://localhost:9080/sparql"   QALD10

        int n = 2;

        // Loop through each binary combination for the first `n` bits
        for (int i = 0; i < Math.pow(2, n); i++) {
            String binaryString = String.format("%" + n + "s", Integer.toBinaryString(i)).replace(' ', '0');

            // Now loop through each possible value of the last digit (0, 1, 2)
            for (int j = 0; j < 3; j++) {

                String result = binaryString + j;

                boolean SetSkipNone = (result.charAt(0) == '1');
                boolean Recursive = (result.charAt(1) == '1');
                int accuracyfunction=0;
                if (result.charAt(2) == '0'){ accuracyfunction =0;};
                if (result.charAt(2) == '1'){ accuracyfunction =1;};
                if (result.charAt(2) == '2'){ accuracyfunction =2;};

                runPruneCEL("http://localhost:3030/Family/sparql" ,"ALC",accuracyfunction, true,
                        true, 1000000, 60000,Recursive, SetSkipNone,
                        "/home/quannian/Ontolearn_KG/LPs/Family/lps.json",
                        "/home/quannian/Ontolearn_KG/Results/Test/"+ result +".csv",false);

                runPruneCEL("http://localhost:3030/Mutagenesis/sparql" ,"ALC",accuracyfunction, true,
                        true, 1000000, 60000,Recursive, SetSkipNone,
                        "/home/quannian/Ontolearn_KG/LPs/Mutagenesis/lps.json",
                        "/home/quannian/Ontolearn_KG/Results/Mutagenesis/"+ result +".csv",false);

                runPruneCEL("http://localhost:3030/Carcinogenesis/sparql" ,"ALC",accuracyfunction, true,
                        true, 1000000, 60000,Recursive, SetSkipNone,
                        "/home/quannian/Ontolearn_KG/LPs/Carcinogenesis/lps.json",
                        "/home/quannian/Ontolearn_KG/Results/Carcinogenesis/"+ result +".csv",false);

            }

        }
        System.exit(0);
    }



    public static void runPruneCEL(String spqrql_endpoint, String description_logic, int accuracyfunction, boolean punishlongexpression,
                                   boolean AvoidPickySolutionsDecorator, int iteration, int time, boolean recursive, boolean setSkipNonImprovingStmts,
                                   String tfjson, String saveplace, boolean cluster) throws Exception {

        boolean isbascore = false;
        boolean isf1score = false;
        boolean isacscore = false;

        // XXX Set SPARQL endpoint
        String endpoint = spqrql_endpoint;

        // XXX Set description logic
        DescriptionLogic logic = DescriptionLogic.parse(description_logic);

        ScoreCalculatorFactory factory = null;

        // XXX Choose F1 or balanced accuracy or accuracy
        if(accuracyfunction == 0)
        {
            factory = new F1MeasureCalculator.Factory();
            isf1score = true;
        }
        if(accuracyfunction == 1)
        {
            factory = new BalancedAccuracyCalculator.Factory();
            isbascore = true;
        }
        if(accuracyfunction == 2)
        {
            factory = new AccuracyCalculator.Factory();
            isacscore = true;
        }

        // Punish long expressions
        if(punishlongexpression) {factory = new LengthBasedRefinementScorer.Factory(factory);}

        // XXX (Optional) avoid choosing solutions that work only for a single example
        if(AvoidPickySolutionsDecorator) {factory = new AvoidingPickySolutionsDecorator.Factory(factory);}

        try (SparqlBasedSuggestor suggestor = SparqlBasedSuggestor.create(endpoint, logic)) {
            suggestor.addToClassBlackList(OWL2.NamedIndividual.getURI());
            suggestor.addToPropertyBlackList(RDF.type.getURI());

            boolean printLogs = false;

            // recursive: find cluster by prototype itself
            PruneCEL cel = null;
            if(recursive) {
                cel = new RecursivePruneCEL(suggestor, logic, factory, suggestor);
            } else {
                 cel = new PruneCEL(suggestor, logic, factory);
            }

                // XXX Max iterations of the refinement
                cel.setMaxIterations(iteration);

                // XXX Maximum time (in ms)
                cel.setMaxTime(time);

                // XXX (Optional) try to avoid refining expressions that have not been created
                // in a promising way (i.e., just added a class to an existing expression
                // without changing the accuracy of the expression)
                if(setSkipNonImprovingStmts) { cel.setSkipNonImprovingStmts(true);}

                // XXX Keep this commented for now
                //{cel.activateRecursiveIteration(suggestor, 1.0, 0.5);

                // XXX Choose the learning problem (as JSON file)
                JSONLearningProblemReader reader = new JSONLearningProblemReader();
                Collection<LearningProblem> problems = reader.readProblems(tfjson);

                try (PrintStream pout = new PrintStream(saveplace)) {
//                for (int i = 0; i < names.size(); ++i) {
                    for (LearningProblem problem : problems) {
                        if (printLogs) {
                            try (OutputStream logStream = new BufferedOutputStream(
                                    new FileOutputStream(problem.getName() + ".log"))) {
                                runSearch(problem.getName(), problem.getPositiveExamples(), problem.getNegativeExamples(),
                                        cel, pout, logStream, isbascore, isf1score, isacscore);
                            }
                        }
                        else
                        {
                            runSearch(problem.getName(), problem.getPositiveExamples(), problem.getNegativeExamples(), cel,
                                    pout, null, isbascore, isf1score, isacscore);
                        }
                    }
                    StringBuilder statistics = new StringBuilder();
                    statistics.append("description_logic:").append(description_logic).append(", IsF1Score:").append(isf1score).append(", IsBascore:").append(isbascore).append(", IsAcscore:").append(isacscore).append(", IsPunishLongExpression:").append(punishlongexpression)
                            .append(", IsAvoidPickySolutionsDecorator:").append(AvoidPickySolutionsDecorator).append(", Isrecursive:").append(recursive)
                            .append(", IssetSkipNonImprovingStmts:").append(setSkipNonImprovingStmts).append(", usecluster:").append(cluster);
                    pout.println(statistics.toString());
                    if(cluster) {

                        Collection<LearningProblem> allproblems = reader.readProblems("/home/quannian/Tentris_Graph/QALD10/TandF/ALL_TandF/MST5/TandF_MST5.json");
                        for (LearningProblem problem : allproblems) {
                            SelectionScores scores = suggestor.scoreExpression(currentClassExpression, problem.getPositiveExamples(), problem.getNegativeExamples());
                            ScoredClassExpression a = factory.create(275, 119).score(currentClassExpression, scores.getPosCount(), scores.getNegCount(), false);
                            StringBuilder row = new StringBuilder();
                            row.append("Result").append(",").append("test").append(",").append("test").append(",").append(a.getClassificationScore()).append(",").append(a.getRefinementScore()).append(",").append(a.getPosCount()).append(",").append(a.getNegCount()).append(",").append("275").append(",").append("119").append(",").append(a.getClassExpression());
                            pout.println(row.toString());
                        }
                    }
                }
        }
        isHeaderPrinted = false;
        currentClassExpression = null;
    }



    public static void runSearch(String name, List<String> positive, List<String> negative, PruneCEL cel,
            PrintStream pout, OutputStream logStream, boolean isbascore, boolean isf1score, boolean isacscore) {
        System.out.println("Starting " + name);
        long time = System.currentTimeMillis();
        List<ScoredClassExpression> expressions = cel.findClassExpression(positive, negative, logStream);
        time = System.currentTimeMillis() - time;
        String number_of_positive = Integer.toString(positive.size());
        String number_of_negative = Integer.toString(negative.size());

        // for calculating clustering
        if (currentClassExpression == null)
            currentClassExpression =  expressions.get(0).getClassExpression();
        else
            currentClassExpression = new Junction(false, currentClassExpression,
                    expressions.get(0).getClassExpression());

        saveClassExpressionsToCsv(expressions, name, time, pout, number_of_positive, number_of_negative, isbascore, isf1score, isacscore);
        // printClassExpressions(expressions, name, time, pout);
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

    public static void saveClassExpressionsToCsv(List<ScoredClassExpression> expressions, String name, long runtime,
                                                 PrintStream pout, String number_of_positive, String number_of_negative,
                                                 boolean isbascore, boolean isf1score, boolean isacscore) {
        // Print the header only if it hasn't been printed yet
        if (!isHeaderPrinted && isf1score){
            pout.println("Result,Name,Runtime,F1-score,R-score,PosCount,NegCount,Number-of-Pos,Number-of-Neg,Expressions");
            isHeaderPrinted = true; // Set the flag to true after printing
        }
        if (!isHeaderPrinted && isbascore){
            pout.println("Result,Name,Runtime,Balanced-accuracy-score,R-score,PosCount,NegCount,Number-of-Pos,Number-of-Neg,Expressions");
            isHeaderPrinted = true; // Set the flag to true after printing
        }
        if (!isHeaderPrinted && isacscore){
            pout.println("Result,Name,Runtime,Accuracy-score,R-score,PosCount,NegCount,Number-of-Pos,Number-of-Neg,Expressions");
            isHeaderPrinted = true; // Set the flag to true after printing
        }

        // Create a CSV row for the results
        StringBuilder row = new StringBuilder();
        row.append("Result");

        // Append the name, or leave it blank if null
        if (name != null) {
            row.append(",").append(name);
        } else {
            row.append(","); // Placeholder for null name
        }

        // Append the runtime
        row.append(",").append(runtime).append("ms,");
        // Append Cscore
        String expressionsList = expressions.isEmpty() ? "" : expressions.get(0).toString();
        int cScoreStart = expressionsList.indexOf("cScore=") + "cScore=".length();
        int cScoreEnd = expressionsList.indexOf(",", cScoreStart);
        String cScore = expressionsList.substring(cScoreStart, cScoreEnd);

        int rScoreStart = expressionsList.indexOf("rScore=") + "rScore=".length();
        int rScoreEnd = expressionsList.indexOf(",", rScoreStart);
        String rScore = expressionsList.substring(rScoreStart, rScoreEnd);

        row.append(cScore).append(",").append(rScore).append(",");

        // Append positive and negative
        int posCountStart = expressionsList.indexOf("posCount=") + "posCount=".length();
        int posCountEnd = expressionsList.indexOf(",", posCountStart);
        String posCount = expressionsList.substring(posCountStart, posCountEnd);

        int negCountStart = expressionsList.indexOf("negCount=") + "negCount=".length();
        int negCountEnd = expressionsList.indexOf("]", negCountStart);
        String negCount = expressionsList.substring(negCountStart, negCountEnd);

        row.append(posCount).append(",").append(negCount).append(",");

        // Append positive and negative number from the cluster
        row.append(number_of_positive).append(",").append(number_of_negative).append(",");


        // Get the first expression only, if the list is not empty
        String expressionsString = expressions.isEmpty() ? "" : "\"" + expressions.toString() + "\"";

        // Append the first expression to the row
        row.append(expressionsString);

        // Print the CSV row
        pout.println(row.toString());
    }
}
