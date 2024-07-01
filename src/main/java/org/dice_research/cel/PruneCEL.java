package org.dice_research.cel;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.dice_research.cel.expression.NamedClass;
import org.dice_research.cel.expression.ScoredCEComparatorForRefinement;
import org.dice_research.cel.expression.ScoredClassExpression;
import org.dice_research.cel.refine.RefinementOperator;
import org.dice_research.cel.refine.SuggestorBasedRefinementOperator;
import org.dice_research.cel.refine.suggest.SparqlBasedSuggestor;
import org.dice_research.cel.refine.suggest.Suggestor;
import org.dice_research.cel.score.AvoidingPickySolutionsDecorator;
import org.dice_research.cel.score.F1MeasureCalculator;
import org.dice_research.cel.score.LengthBasedRefinementScorer;
import org.dice_research.cel.score.ScoreCalculator;
import org.dice_research.cel.score.ScoreCalculatorFactory;
import org.dice_research.topicmodeling.commons.collections.TopDoubleObjectCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PruneCEL {

    private static final Logger LOGGER = LoggerFactory.getLogger(PruneCEL.class);

    protected Suggestor suggestor;
    protected DescriptionLogic logic;
    protected ScoreCalculatorFactory calculatorFactory;
    protected int maxIterations = 0;
    protected long maxTime = 0L;

    public PruneCEL(Suggestor suggestor, DescriptionLogic logic, ScoreCalculatorFactory calculatorFactory) {
        super();
        this.suggestor = suggestor;
        this.logic = logic;
        this.calculatorFactory = calculatorFactory;
    }

    public List<ScoredClassExpression> findClassExpression(Collection<String> positive, Collection<String> negative) {
        long startTime = System.currentTimeMillis();
        TopDoubleObjectCollection<ScoredClassExpression> topExpressions = new TopDoubleObjectCollection<>(10, false);
        Set<ScoredClassExpression> seenExpressions = new HashSet<>();
        Queue<ScoredClassExpression> queue = new PriorityQueue<ScoredClassExpression>(
                new ScoredCEComparatorForRefinement());
        ScoreCalculator scoreCalculator = calculatorFactory.create(positive.size(), negative.size());
        double perfectScore = scoreCalculator.getPerfectScore();

        RefinementOperator rho = new SuggestorBasedRefinementOperator(suggestor, logic, scoreCalculator, positive,
                negative);

        Collection<ScoredClassExpression> newExpressions;
        ScoredClassExpression nextBestExpression;
        // Create Top expression
        nextBestExpression = scoreCalculator.score(NamedClass.TOP, positive.size(), negative.size());
        queue.add(nextBestExpression);
        topExpressions.add(nextBestExpression.getClassificationScore(), nextBestExpression);
        int iterationCount = 0;
        // Start iterating over the queue
        while (!queue.isEmpty()) {
            nextBestExpression = queue.poll();
            LOGGER.info("Refining rScore={}, cScore={}, ce={}", nextBestExpression.getRefinementScore(),
                    nextBestExpression.getClassificationScore(), nextBestExpression.getClassExpression());
            // Refine this expression
            newExpressions = rho.refine(nextBestExpression);
            // Check the expressions
            for (ScoredClassExpression newExpression : newExpressions) {
                // If we haven't seen this before
                if (!seenExpressions.contains(newExpression)) {
                    queue.add(newExpression);
                    topExpressions.add(newExpression.getClassificationScore(), newExpression);
                    seenExpressions.add(newExpression);
                }
            }
            iterationCount++;
            // If we found an expression with 1.0, OR we have reached the max iteration
            // count, we should stop searching
            if ((topExpressions.values[0] >= perfectScore) || (maxIterations > 0 && iterationCount >= maxIterations)
                    || (maxTime > 0 && maxTime < (System.currentTimeMillis() - startTime))) {
                LOGGER.info("Stopping search. Saw {} expressions.", seenExpressions.size());
                return Stream.of(topExpressions.getObjects()).map(o -> (ScoredClassExpression) o).toList();
            }
        }
        // We tried everything we could... that's it
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

    public static void main(String[] args) throws Exception {
        String endpoint = "http://localhost:3030/exp-bench/sparql";
        // String endpoint = "http://localhost:3030/family/sparql";
        DescriptionLogic logic = DescriptionLogic.parse("ALC");

        ScoreCalculatorFactory factory = null;
        factory = new F1MeasureCalculator.Factory();
        //factory = new BalancedAccuracyCalculator.Factory();
        
        factory = new LengthBasedRefinementScorer.Factory(factory);
        factory = new AvoidingPickySolutionsDecorator.Factory(factory);

        try (SparqlBasedSuggestor suggestor = new SparqlBasedSuggestor(endpoint, logic)) {
            suggestor.addToClassBlackList(OWL2.NamedIndividual.getURI());
            suggestor.addToPropertyBlackList(RDF.type.getURI());

            PruneCEL cel = new PruneCEL(suggestor, logic, factory);
            cel.setMaxIterations(1000);
            cel.setMaxTime(60000);

//            // Country questions
//            Collection<String> positive = Arrays.asList("https://github.com/qald_10#QUE132",
//                    "https://github.com/qald_10#QUE278", "https://github.com/qald_10#QUE26",
//                    "https://github.com/qald_10#QUE299", "https://github.com/qald_10#QUE130",
//                    "https://github.com/qald_10#QUE173", "https://github.com/qald_10#QUE391",
//                    "https://github.com/qald_10#QUE392", "https://github.com/qald_10#QUE341",
//                    "https://github.com/qald_10#QUE133", "https://github.com/qald_10#QUE27",
//                    "https://github.com/qald_10#QUE127");
//            Collection<String> negative = Arrays.asList("https://github.com/qald_10#QUE284",
//                    "https://github.com/qald_10#QUE286", "https://github.com/qald_10#QUE370",
//                    "https://github.com/qald_10#QUE381", "https://github.com/qald_10#QUE2",
//                    "https://github.com/qald_10#QUE234", "https://github.com/qald_10#QUE319",
//                    "https://github.com/qald_10#QUE375", "https://github.com/qald_10#QUE0",
//                    "https://github.com/qald_10#QUE5", "https://github.com/qald_10#QUE9",
//                    "https://github.com/qald_10#QUE10", "https://github.com/qald_10#QUE13",
//                    "https://github.com/qald_10#QUE14", "https://github.com/qald_10#QUE15",
//                    "https://github.com/qald_10#QUE17", "https://github.com/qald_10#QUE18",
//                    "https://github.com/qald_10#QUE19", "https://github.com/qald_10#QUE22",
//                    "https://github.com/qald_10#QUE24", "https://github.com/qald_10#QUE28",
//                    "https://github.com/qald_10#QUE29", "https://github.com/qald_10#QUE30",
//                    "https://github.com/qald_10#QUE39", "https://github.com/qald_10#QUE46",
//                    "https://github.com/qald_10#QUE47", "https://github.com/qald_10#QUE52",
//                    "https://github.com/qald_10#QUE62", "https://github.com/qald_10#QUE66",
//                    "https://github.com/qald_10#QUE71", "https://github.com/qald_10#QUE73",
//                    "https://github.com/qald_10#QUE74", "https://github.com/qald_10#QUE82",
//                    "https://github.com/qald_10#QUE83", "https://github.com/qald_10#QUE84",
//                    "https://github.com/qald_10#QUE97", "https://github.com/qald_10#QUE99",
//                    "https://github.com/qald_10#QUE108", "https://github.com/qald_10#QUE111",
//                    "https://github.com/qald_10#QUE113", "https://github.com/qald_10#QUE118",
//                    "https://github.com/qald_10#QUE120", "https://github.com/qald_10#QUE121",
//                    "https://github.com/qald_10#QUE122", "https://github.com/qald_10#QUE126",
//                    "https://github.com/qald_10#QUE128", "https://github.com/qald_10#QUE140",
//                    "https://github.com/qald_10#QUE143", "https://github.com/qald_10#QUE144",
//                    "https://github.com/qald_10#QUE148", "https://github.com/qald_10#QUE149",
//                    "https://github.com/qald_10#QUE150", "https://github.com/qald_10#QUE151",
//                    "https://github.com/qald_10#QUE152", "https://github.com/qald_10#QUE154",
//                    "https://github.com/qald_10#QUE155", "https://github.com/qald_10#QUE158",
//                    "https://github.com/qald_10#QUE159", "https://github.com/qald_10#QUE163",
//                    "https://github.com/qald_10#QUE168", "https://github.com/qald_10#QUE169",
//                    "https://github.com/qald_10#QUE172", "https://github.com/qald_10#QUE175",
//                    "https://github.com/qald_10#QUE177", "https://github.com/qald_10#QUE180",
//                    "https://github.com/qald_10#QUE181", "https://github.com/qald_10#QUE185",
//                    "https://github.com/qald_10#QUE189", "https://github.com/qald_10#QUE198",
//                    "https://github.com/qald_10#QUE201", "https://github.com/qald_10#QUE214",
//                    "https://github.com/qald_10#QUE215", "https://github.com/qald_10#QUE216",
//                    "https://github.com/qald_10#QUE219", "https://github.com/qald_10#QUE221",
//                    "https://github.com/qald_10#QUE230", "https://github.com/qald_10#QUE231",
//                    "https://github.com/qald_10#QUE233", "https://github.com/qald_10#QUE238",
//                    "https://github.com/qald_10#QUE243", "https://github.com/qald_10#QUE250",
//                    "https://github.com/qald_10#QUE251", "https://github.com/qald_10#QUE255",
//                    "https://github.com/qald_10#QUE259", "https://github.com/qald_10#QUE261",
//                    "https://github.com/qald_10#QUE265", "https://github.com/qald_10#QUE271",
//                    "https://github.com/qald_10#QUE273", "https://github.com/qald_10#QUE274",
//                    "https://github.com/qald_10#QUE276", "https://github.com/qald_10#QUE277",
//                    "https://github.com/qald_10#QUE281", "https://github.com/qald_10#QUE282",
//                    "https://github.com/qald_10#QUE283", "https://github.com/qald_10#QUE285",
//                    "https://github.com/qald_10#QUE292", "https://github.com/qald_10#QUE294",
//                    "https://github.com/qald_10#QUE301", "https://github.com/qald_10#QUE303",
//                    "https://github.com/qald_10#QUE307", "https://github.com/qald_10#QUE314",
//                    "https://github.com/qald_10#QUE324", "https://github.com/qald_10#QUE325",
//                    "https://github.com/qald_10#QUE327", "https://github.com/qald_10#QUE328",
//                    "https://github.com/qald_10#QUE331", "https://github.com/qald_10#QUE335",
//                    "https://github.com/qald_10#QUE339", "https://github.com/qald_10#QUE346",
//                    "https://github.com/qald_10#QUE347", "https://github.com/qald_10#QUE348",
//                    "https://github.com/qald_10#QUE349", "https://github.com/qald_10#QUE350",
//                    "https://github.com/qald_10#QUE351", "https://github.com/qald_10#QUE353",
//                    "https://github.com/qald_10#QUE354", "https://github.com/qald_10#QUE355",
//                    "https://github.com/qald_10#QUE368", "https://github.com/qald_10#QUE371",
//                    "https://github.com/qald_10#QUE385", "https://github.com/qald_10#QUE386",
//                    "https://github.com/qald_10#QUE388", "https://github.com/qald_10#QUE390",
//                    "https://github.com/qald_10#QUE1", "https://github.com/qald_10#QUE3",
//                    "https://github.com/qald_10#QUE4", "https://github.com/qald_10#QUE6",
//                    "https://github.com/qald_10#QUE7", "https://github.com/qald_10#QUE8",
//                    "https://github.com/qald_10#QUE11", "https://github.com/qald_10#QUE12",
//                    "https://github.com/qald_10#QUE16", "https://github.com/qald_10#QUE20",
//                    "https://github.com/qald_10#QUE21", "https://github.com/qald_10#QUE23",
//                    "https://github.com/qald_10#QUE25", "https://github.com/qald_10#QUE31",
//                    "https://github.com/qald_10#QUE32", "https://github.com/qald_10#QUE33",
//                    "https://github.com/qald_10#QUE34", "https://github.com/qald_10#QUE35",
//                    "https://github.com/qald_10#QUE36", "https://github.com/qald_10#QUE37",
//                    "https://github.com/qald_10#QUE38", "https://github.com/qald_10#QUE40",
//                    "https://github.com/qald_10#QUE41", "https://github.com/qald_10#QUE42",
//                    "https://github.com/qald_10#QUE43", "https://github.com/qald_10#QUE44",
//                    "https://github.com/qald_10#QUE45", "https://github.com/qald_10#QUE48",
//                    "https://github.com/qald_10#QUE49", "https://github.com/qald_10#QUE50",
//                    "https://github.com/qald_10#QUE51", "https://github.com/qald_10#QUE53",
//                    "https://github.com/qald_10#QUE54", "https://github.com/qald_10#QUE55",
//                    "https://github.com/qald_10#QUE56", "https://github.com/qald_10#QUE57",
//                    "https://github.com/qald_10#QUE58", "https://github.com/qald_10#QUE59",
//                    "https://github.com/qald_10#QUE60", "https://github.com/qald_10#QUE61",
//                    "https://github.com/qald_10#QUE63", "https://github.com/qald_10#QUE64",
//                    "https://github.com/qald_10#QUE65", "https://github.com/qald_10#QUE67",
//                    "https://github.com/qald_10#QUE68", "https://github.com/qald_10#QUE69",
//                    "https://github.com/qald_10#QUE70", "https://github.com/qald_10#QUE72",
//                    "https://github.com/qald_10#QUE75", "https://github.com/qald_10#QUE76",
//                    "https://github.com/qald_10#QUE77", "https://github.com/qald_10#QUE78",
//                    "https://github.com/qald_10#QUE79", "https://github.com/qald_10#QUE80",
//                    "https://github.com/qald_10#QUE81", "https://github.com/qald_10#QUE85",
//                    "https://github.com/qald_10#QUE86", "https://github.com/qald_10#QUE87",
//                    "https://github.com/qald_10#QUE88", "https://github.com/qald_10#QUE89",
//                    "https://github.com/qald_10#QUE90", "https://github.com/qald_10#QUE91",
//                    "https://github.com/qald_10#QUE92", "https://github.com/qald_10#QUE93",
//                    "https://github.com/qald_10#QUE94", "https://github.com/qald_10#QUE95",
//                    "https://github.com/qald_10#QUE96", "https://github.com/qald_10#QUE98",
//                    "https://github.com/qald_10#QUE100", "https://github.com/qald_10#QUE101",
//                    "https://github.com/qald_10#QUE102", "https://github.com/qald_10#QUE103",
//                    "https://github.com/qald_10#QUE104", "https://github.com/qald_10#QUE105",
//                    "https://github.com/qald_10#QUE106", "https://github.com/qald_10#QUE107",
//                    "https://github.com/qald_10#QUE109", "https://github.com/qald_10#QUE110",
//                    "https://github.com/qald_10#QUE112", "https://github.com/qald_10#QUE114",
//                    "https://github.com/qald_10#QUE115", "https://github.com/qald_10#QUE116",
//                    "https://github.com/qald_10#QUE117", "https://github.com/qald_10#QUE119",
//                    "https://github.com/qald_10#QUE123", "https://github.com/qald_10#QUE124",
//                    "https://github.com/qald_10#QUE125", "https://github.com/qald_10#QUE129",
//                    "https://github.com/qald_10#QUE131", "https://github.com/qald_10#QUE134",
//                    "https://github.com/qald_10#QUE135", "https://github.com/qald_10#QUE136",
//                    "https://github.com/qald_10#QUE137", "https://github.com/qald_10#QUE138",
//                    "https://github.com/qald_10#QUE139", "https://github.com/qald_10#QUE141",
//                    "https://github.com/qald_10#QUE142", "https://github.com/qald_10#QUE145",
//                    "https://github.com/qald_10#QUE146", "https://github.com/qald_10#QUE147",
//                    "https://github.com/qald_10#QUE153", "https://github.com/qald_10#QUE156",
//                    "https://github.com/qald_10#QUE157", "https://github.com/qald_10#QUE160",
//                    "https://github.com/qald_10#QUE161", "https://github.com/qald_10#QUE162",
//                    "https://github.com/qald_10#QUE164", "https://github.com/qald_10#QUE165",
//                    "https://github.com/qald_10#QUE166", "https://github.com/qald_10#QUE167",
//                    "https://github.com/qald_10#QUE170", "https://github.com/qald_10#QUE171",
//                    "https://github.com/qald_10#QUE174", "https://github.com/qald_10#QUE176",
//                    "https://github.com/qald_10#QUE178", "https://github.com/qald_10#QUE179",
//                    "https://github.com/qald_10#QUE182", "https://github.com/qald_10#QUE183",
//                    "https://github.com/qald_10#QUE184", "https://github.com/qald_10#QUE186",
//                    "https://github.com/qald_10#QUE187", "https://github.com/qald_10#QUE188",
//                    "https://github.com/qald_10#QUE190", "https://github.com/qald_10#QUE191",
//                    "https://github.com/qald_10#QUE192", "https://github.com/qald_10#QUE193",
//                    "https://github.com/qald_10#QUE194", "https://github.com/qald_10#QUE195",
//                    "https://github.com/qald_10#QUE196", "https://github.com/qald_10#QUE197",
//                    "https://github.com/qald_10#QUE199", "https://github.com/qald_10#QUE200",
//                    "https://github.com/qald_10#QUE202", "https://github.com/qald_10#QUE203",
//                    "https://github.com/qald_10#QUE204", "https://github.com/qald_10#QUE205",
//                    "https://github.com/qald_10#QUE206", "https://github.com/qald_10#QUE207",
//                    "https://github.com/qald_10#QUE208", "https://github.com/qald_10#QUE209",
//                    "https://github.com/qald_10#QUE210", "https://github.com/qald_10#QUE211",
//                    "https://github.com/qald_10#QUE212", "https://github.com/qald_10#QUE213",
//                    "https://github.com/qald_10#QUE217", "https://github.com/qald_10#QUE218",
//                    "https://github.com/qald_10#QUE220", "https://github.com/qald_10#QUE222",
//                    "https://github.com/qald_10#QUE223", "https://github.com/qald_10#QUE224",
//                    "https://github.com/qald_10#QUE225", "https://github.com/qald_10#QUE226",
//                    "https://github.com/qald_10#QUE227", "https://github.com/qald_10#QUE228",
//                    "https://github.com/qald_10#QUE229", "https://github.com/qald_10#QUE232",
//                    "https://github.com/qald_10#QUE235", "https://github.com/qald_10#QUE236",
//                    "https://github.com/qald_10#QUE237", "https://github.com/qald_10#QUE239",
//                    "https://github.com/qald_10#QUE240", "https://github.com/qald_10#QUE241",
//                    "https://github.com/qald_10#QUE242", "https://github.com/qald_10#QUE244",
//                    "https://github.com/qald_10#QUE245", "https://github.com/qald_10#QUE246",
//                    "https://github.com/qald_10#QUE247", "https://github.com/qald_10#QUE248",
//                    "https://github.com/qald_10#QUE249", "https://github.com/qald_10#QUE252",
//                    "https://github.com/qald_10#QUE253", "https://github.com/qald_10#QUE254",
//                    "https://github.com/qald_10#QUE256", "https://github.com/qald_10#QUE257",
//                    "https://github.com/qald_10#QUE258", "https://github.com/qald_10#QUE260",
//                    "https://github.com/qald_10#QUE262", "https://github.com/qald_10#QUE263",
//                    "https://github.com/qald_10#QUE264", "https://github.com/qald_10#QUE266",
//                    "https://github.com/qald_10#QUE267", "https://github.com/qald_10#QUE268",
//                    "https://github.com/qald_10#QUE269", "https://github.com/qald_10#QUE270",
//                    "https://github.com/qald_10#QUE272", "https://github.com/qald_10#QUE275",
//                    "https://github.com/qald_10#QUE279", "https://github.com/qald_10#QUE280",
//                    "https://github.com/qald_10#QUE287", "https://github.com/qald_10#QUE288",
//                    "https://github.com/qald_10#QUE289", "https://github.com/qald_10#QUE290",
//                    "https://github.com/qald_10#QUE291", "https://github.com/qald_10#QUE293",
//                    "https://github.com/qald_10#QUE295", "https://github.com/qald_10#QUE296",
//                    "https://github.com/qald_10#QUE297", "https://github.com/qald_10#QUE298",
//                    "https://github.com/qald_10#QUE300", "https://github.com/qald_10#QUE302",
//                    "https://github.com/qald_10#QUE304", "https://github.com/qald_10#QUE305",
//                    "https://github.com/qald_10#QUE306", "https://github.com/qald_10#QUE308",
//                    "https://github.com/qald_10#QUE309", "https://github.com/qald_10#QUE310",
//                    "https://github.com/qald_10#QUE311", "https://github.com/qald_10#QUE312",
//                    "https://github.com/qald_10#QUE313", "https://github.com/qald_10#QUE315",
//                    "https://github.com/qald_10#QUE316", "https://github.com/qald_10#QUE317",
//                    "https://github.com/qald_10#QUE318", "https://github.com/qald_10#QUE320",
//                    "https://github.com/qald_10#QUE321", "https://github.com/qald_10#QUE322",
//                    "https://github.com/qald_10#QUE323", "https://github.com/qald_10#QUE326",
//                    "https://github.com/qald_10#QUE329", "https://github.com/qald_10#QUE330",
//                    "https://github.com/qald_10#QUE332", "https://github.com/qald_10#QUE333",
//                    "https://github.com/qald_10#QUE334", "https://github.com/qald_10#QUE336",
//                    "https://github.com/qald_10#QUE337", "https://github.com/qald_10#QUE338",
//                    "https://github.com/qald_10#QUE340", "https://github.com/qald_10#QUE342",
//                    "https://github.com/qald_10#QUE343", "https://github.com/qald_10#QUE344",
//                    "https://github.com/qald_10#QUE345", "https://github.com/qald_10#QUE352",
//                    "https://github.com/qald_10#QUE356", "https://github.com/qald_10#QUE357",
//                    "https://github.com/qald_10#QUE358", "https://github.com/qald_10#QUE359",
//                    "https://github.com/qald_10#QUE360", "https://github.com/qald_10#QUE361",
//                    "https://github.com/qald_10#QUE362", "https://github.com/qald_10#QUE363",
//                    "https://github.com/qald_10#QUE364", "https://github.com/qald_10#QUE365",
//                    "https://github.com/qald_10#QUE366", "https://github.com/qald_10#QUE367",
//                    "https://github.com/qald_10#QUE369", "https://github.com/qald_10#QUE372",
//                    "https://github.com/qald_10#QUE373", "https://github.com/qald_10#QUE374",
//                    "https://github.com/qald_10#QUE376", "https://github.com/qald_10#QUE377",
//                    "https://github.com/qald_10#QUE378", "https://github.com/qald_10#QUE379",
//                    "https://github.com/qald_10#QUE380", "https://github.com/qald_10#QUE382",
//                    "https://github.com/qald_10#QUE383", "https://github.com/qald_10#QUE384",
//                    "https://github.com/qald_10#QUE387", "https://github.com/qald_10#QUE389",
//                    "https://github.com/qald_10#QUE393", "http://github.com/qald_10/QUERY#QUERY225",
//                    "http://github.com/qald_10/QUERY#QUERY270", "http://github.com/qald_10/QUERY#QUERY310",
//                    "http://github.com/qald_10/QUERY#QUERY323", "http://github.com/qald_10/QUERY#QUERY357",
//                    "http://github.com/qald_10/QUERY#QUERY366", "http://github.com/qald_10/QUERY#QUERY378",
//                    "http://github.com/qald_10/QUERY#QUERY44", "http://github.com/qald_10/QUERY#QUERY200",
//                    "http://github.com/qald_10/QUERY#QUERY219", "http://github.com/qald_10/NLP#NLP1",
//                    "http://github.com/qald_10/NLP#NLP77", "http://github.com/qald_10/NLP#NLP89",
//                    "http://github.com/qald_10/NLP#NLP151", "http://github.com/qald_10/NLP#NLP183",
//                    "http://github.com/qald_10/NLP#NLP217", "http://github.com/qald_10/NLP#NLP286",
//                    "http://github.com/qald_10/NLP#NLP310", "http://github.com/qald_10/NLP#NLP320",
//                    "http://github.com/qald_10/NLP#NLP340", "http://www.wikidata.org/entity/Q30304723",
//                    "http://www.wikidata.org/entity/Q40719727", "http://www.wikidata.org/entity/Q833",
//                    "http://www.wikidata.org/entity/Q16713577", "http://www.wikidata.org/entity/Q14017079",
//                    "http://www.wikidata.org/entity/Q14596169", "http://www.wikidata.org/entity/Q69523839",
//                    "http://www.wikidata.org/entity/Q111118353", "http://www.wikidata.org/entity/Q24034552",
//                    "http://www.wikidata.org/entity/Q44464550", "http://www.wikidata.org/entity/Q162297",
//                    "http://www.wikidata.org/entity/Q37067257", "http://www.wikidata.org/entity/Q112544626",
//                    "http://www.wikidata.org/entity/Q118676347", "http://www.wikidata.org/entity/Q2039789",
//                    "http://www.wikidata.org/entity/Q26370006", "http://www.wikidata.org/entity/Q11766277",
//                    "http://www.wikidata.org/entity/Q3425601", "http://www.wikidata.org/entity/Q833232",
//                    "http://www.wikidata.org/entity/Q111122528", "http://www.wikidata.org/entity/Q41710975",
//                    "http://www.wikidata.org/entity/P1401", "http://www.wikidata.org/entity/Q9690678",
//                    "http://www.wikidata.org/entity/Q98503808", "http://www.wikidata.org/entity/Q19229340",
//                    "http://www.wikidata.org/entity/Q36053591", "http://www.wikidata.org/entity/Q7797851",
//                    "http://www.wikidata.org/entity/Q316", "http://www.wikidata.org/entity/Q6736049",
//                    "http://www.wikidata.org/entity/Q33129660", "http://www.wikidata.org/entity/Q205575",
//                    "http://www.wikidata.org/entity/Q116212246", "http://www.wikidata.org/entity/Q916617",
//                    "http://www.wikidata.org/entity/Q598658", "http://www.wikidata.org/entity/Q82248",
//                    "http://www.wikidata.org/entity/Q1948007", "http://www.wikidata.org/entity/Q212960",
//                    "http://www.wikidata.org/entity/Q6525002", "http://www.wikidata.org/entity/Q871549",
//                    "http://www.wikidata.org/entity/Q7121289", "http://www.wikidata.org/entity/Q26257948",
//                    "http://www.wikidata.org/entity/Q98810351", "http://www.wikidata.org/entity/Q102285387",
//                    "http://www.wikidata.org/entity/Q775805", "http://www.wikidata.org/entity/Q7576374",
//                    "http://www.wikidata.org/entity/Q1250464", "http://www.wikidata.org/entity/Q4890957",
//                    "http://www.wikidata.org/entity/Q104665263", "http://www.wikidata.org/entity/Q58029430",
//                    "http://www.wikidata.org/entity/Q102483", "http://www.wikidata.org/entity/Q2743",
//                    "http://www.wikidata.org/entity/Q531425", "http://www.wikidata.org/entity/Q3074431",
//                    "http://www.wikidata.org/entity/Q11254", "http://www.wikidata.org/entity/Q96793309",
//                    "http://www.wikidata.org/entity/Q189336", "http://www.wikidata.org/entity/Q111976692",
//                    "http://www.wikidata.org/entity/Q2525881", "http://www.wikidata.org/entity/Q717119",
//                    "http://www.wikidata.org/entity/Q3930527", "http://www.wikidata.org/entity/Q118529009",
//                    "http://www.wikidata.org/entity/Q30247719", "http://www.wikidata.org/entity/Q68177712",
//                    "http://www.wikidata.org/entity/Q535979", "http://www.wikidata.org/entity/Q6469132",
//                    "http://www.wikidata.org/entity/Q116192398", "http://www.wikidata.org/entity/Q8209976",
//                    "http://www.wikidata.org/entity/Q7875112", "http://www.wikidata.org/entity/Q3489340",
//                    "http://www.wikidata.org/entity/Q368091", "http://www.wikidata.org/entity/Q106677271",
//                    "http://www.wikidata.org/entity/P4895", "http://www.wikidata.org/entity/Q232384",
//                    "http://www.wikidata.org/entity/Q2350211", "http://www.wikidata.org/entity/Q2339602",
//                    "http://www.wikidata.org/entity/Q560694", "http://www.wikidata.org/entity/Q1299240",
//                    "http://www.wikidata.org/entity/Q6519853");
// MST5 positives
            List<String> names = new ArrayList<>();
            List<List<String>> positives = new ArrayList<>();
            List<List<String>> negatives = new ArrayList<>();

            List<String> mst5Wrong = Arrays.asList("https://github.com/qald_10#QUE1", "https://github.com/qald_10#QUE2",
                    "https://github.com/qald_10#QUE3", "https://github.com/qald_10#QUE4",
                    "https://github.com/qald_10#QUE6", "https://github.com/qald_10#QUE7",
                    "https://github.com/qald_10#QUE8", "https://github.com/qald_10#QUE11",
                    "https://github.com/qald_10#QUE12", "https://github.com/qald_10#QUE16",
                    "https://github.com/qald_10#QUE20", "https://github.com/qald_10#QUE21",
                    "https://github.com/qald_10#QUE23", "https://github.com/qald_10#QUE25",
                    "https://github.com/qald_10#QUE26", "https://github.com/qald_10#QUE27",
                    "https://github.com/qald_10#QUE31", "https://github.com/qald_10#QUE32",
                    "https://github.com/qald_10#QUE33", "https://github.com/qald_10#QUE34",
                    "https://github.com/qald_10#QUE35", "https://github.com/qald_10#QUE36",
                    "https://github.com/qald_10#QUE37", "https://github.com/qald_10#QUE38",
                    "https://github.com/qald_10#QUE40", "https://github.com/qald_10#QUE41",
                    "https://github.com/qald_10#QUE42", "https://github.com/qald_10#QUE43",
                    "https://github.com/qald_10#QUE44", "https://github.com/qald_10#QUE45",
                    "https://github.com/qald_10#QUE48", "https://github.com/qald_10#QUE49",
                    "https://github.com/qald_10#QUE50", "https://github.com/qald_10#QUE51",
                    "https://github.com/qald_10#QUE53", "https://github.com/qald_10#QUE54",
                    "https://github.com/qald_10#QUE55", "https://github.com/qald_10#QUE56",
                    "https://github.com/qald_10#QUE57", "https://github.com/qald_10#QUE58",
                    "https://github.com/qald_10#QUE59", "https://github.com/qald_10#QUE60",
                    "https://github.com/qald_10#QUE61", "https://github.com/qald_10#QUE63",
                    "https://github.com/qald_10#QUE64", "https://github.com/qald_10#QUE65",
                    "https://github.com/qald_10#QUE67", "https://github.com/qald_10#QUE68",
                    "https://github.com/qald_10#QUE69", "https://github.com/qald_10#QUE70",
                    "https://github.com/qald_10#QUE72", "https://github.com/qald_10#QUE75",
                    "https://github.com/qald_10#QUE76", "https://github.com/qald_10#QUE77",
                    "https://github.com/qald_10#QUE78", "https://github.com/qald_10#QUE79",
                    "https://github.com/qald_10#QUE80", "https://github.com/qald_10#QUE81",
                    "https://github.com/qald_10#QUE85", "https://github.com/qald_10#QUE86",
                    "https://github.com/qald_10#QUE87", "https://github.com/qald_10#QUE88",
                    "https://github.com/qald_10#QUE89", "https://github.com/qald_10#QUE90",
                    "https://github.com/qald_10#QUE91", "https://github.com/qald_10#QUE92",
                    "https://github.com/qald_10#QUE93", "https://github.com/qald_10#QUE94",
                    "https://github.com/qald_10#QUE95", "https://github.com/qald_10#QUE96",
                    "https://github.com/qald_10#QUE98", "https://github.com/qald_10#QUE100",
                    "https://github.com/qald_10#QUE101", "https://github.com/qald_10#QUE102",
                    "https://github.com/qald_10#QUE103", "https://github.com/qald_10#QUE104",
                    "https://github.com/qald_10#QUE105", "https://github.com/qald_10#QUE106",
                    "https://github.com/qald_10#QUE107", "https://github.com/qald_10#QUE109",
                    "https://github.com/qald_10#QUE110", "https://github.com/qald_10#QUE112",
                    "https://github.com/qald_10#QUE114", "https://github.com/qald_10#QUE115",
                    "https://github.com/qald_10#QUE116", "https://github.com/qald_10#QUE117",
                    "https://github.com/qald_10#QUE119", "https://github.com/qald_10#QUE123",
                    "https://github.com/qald_10#QUE124", "https://github.com/qald_10#QUE125",
                    "https://github.com/qald_10#QUE129", "https://github.com/qald_10#QUE130",
                    "https://github.com/qald_10#QUE131", "https://github.com/qald_10#QUE132",
                    "https://github.com/qald_10#QUE134", "https://github.com/qald_10#QUE135",
                    "https://github.com/qald_10#QUE136", "https://github.com/qald_10#QUE137",
                    "https://github.com/qald_10#QUE138", "https://github.com/qald_10#QUE139",
                    "https://github.com/qald_10#QUE141", "https://github.com/qald_10#QUE142",
                    "https://github.com/qald_10#QUE145", "https://github.com/qald_10#QUE146",
                    "https://github.com/qald_10#QUE147", "https://github.com/qald_10#QUE153",
                    "https://github.com/qald_10#QUE156", "https://github.com/qald_10#QUE157",
                    "https://github.com/qald_10#QUE160", "https://github.com/qald_10#QUE161",
                    "https://github.com/qald_10#QUE162", "https://github.com/qald_10#QUE164",
                    "https://github.com/qald_10#QUE165", "https://github.com/qald_10#QUE166",
                    "https://github.com/qald_10#QUE167", "https://github.com/qald_10#QUE170",
                    "https://github.com/qald_10#QUE171", "https://github.com/qald_10#QUE174",
                    "https://github.com/qald_10#QUE176", "https://github.com/qald_10#QUE178",
                    "https://github.com/qald_10#QUE179", "https://github.com/qald_10#QUE182",
                    "https://github.com/qald_10#QUE183", "https://github.com/qald_10#QUE184",
                    "https://github.com/qald_10#QUE186", "https://github.com/qald_10#QUE187",
                    "https://github.com/qald_10#QUE188", "https://github.com/qald_10#QUE190",
                    "https://github.com/qald_10#QUE191", "https://github.com/qald_10#QUE192",
                    "https://github.com/qald_10#QUE193", "https://github.com/qald_10#QUE194",
                    "https://github.com/qald_10#QUE195", "https://github.com/qald_10#QUE196",
                    "https://github.com/qald_10#QUE197", "https://github.com/qald_10#QUE199",
                    "https://github.com/qald_10#QUE200", "https://github.com/qald_10#QUE202",
                    "https://github.com/qald_10#QUE203", "https://github.com/qald_10#QUE204",
                    "https://github.com/qald_10#QUE205", "https://github.com/qald_10#QUE206",
                    "https://github.com/qald_10#QUE207", "https://github.com/qald_10#QUE208",
                    "https://github.com/qald_10#QUE209", "https://github.com/qald_10#QUE210",
                    "https://github.com/qald_10#QUE211", "https://github.com/qald_10#QUE212",
                    "https://github.com/qald_10#QUE213", "https://github.com/qald_10#QUE217",
                    "https://github.com/qald_10#QUE218", "https://github.com/qald_10#QUE220",
                    "https://github.com/qald_10#QUE222", "https://github.com/qald_10#QUE223",
                    "https://github.com/qald_10#QUE224", "https://github.com/qald_10#QUE225",
                    "https://github.com/qald_10#QUE226", "https://github.com/qald_10#QUE227",
                    "https://github.com/qald_10#QUE228", "https://github.com/qald_10#QUE229",
                    "https://github.com/qald_10#QUE232", "https://github.com/qald_10#QUE234",
                    "https://github.com/qald_10#QUE235", "https://github.com/qald_10#QUE236",
                    "https://github.com/qald_10#QUE237", "https://github.com/qald_10#QUE239",
                    "https://github.com/qald_10#QUE240", "https://github.com/qald_10#QUE241",
                    "https://github.com/qald_10#QUE242", "https://github.com/qald_10#QUE244",
                    "https://github.com/qald_10#QUE245", "https://github.com/qald_10#QUE246",
                    "https://github.com/qald_10#QUE247", "https://github.com/qald_10#QUE248",
                    "https://github.com/qald_10#QUE249", "https://github.com/qald_10#QUE252",
                    "https://github.com/qald_10#QUE253", "https://github.com/qald_10#QUE254",
                    "https://github.com/qald_10#QUE256", "https://github.com/qald_10#QUE257",
                    "https://github.com/qald_10#QUE258", "https://github.com/qald_10#QUE260",
                    "https://github.com/qald_10#QUE262", "https://github.com/qald_10#QUE263",
                    "https://github.com/qald_10#QUE264", "https://github.com/qald_10#QUE266",
                    "https://github.com/qald_10#QUE267", "https://github.com/qald_10#QUE268",
                    "https://github.com/qald_10#QUE269", "https://github.com/qald_10#QUE270",
                    "https://github.com/qald_10#QUE272", "https://github.com/qald_10#QUE275",
                    "https://github.com/qald_10#QUE278", "https://github.com/qald_10#QUE279",
                    "https://github.com/qald_10#QUE280", "https://github.com/qald_10#QUE284",
                    "https://github.com/qald_10#QUE286", "https://github.com/qald_10#QUE287",
                    "https://github.com/qald_10#QUE288", "https://github.com/qald_10#QUE289",
                    "https://github.com/qald_10#QUE290", "https://github.com/qald_10#QUE291",
                    "https://github.com/qald_10#QUE293", "https://github.com/qald_10#QUE295",
                    "https://github.com/qald_10#QUE296", "https://github.com/qald_10#QUE297",
                    "https://github.com/qald_10#QUE298", "https://github.com/qald_10#QUE299",
                    "https://github.com/qald_10#QUE300", "https://github.com/qald_10#QUE302",
                    "https://github.com/qald_10#QUE304", "https://github.com/qald_10#QUE305",
                    "https://github.com/qald_10#QUE306", "https://github.com/qald_10#QUE308",
                    "https://github.com/qald_10#QUE309", "https://github.com/qald_10#QUE310",
                    "https://github.com/qald_10#QUE311", "https://github.com/qald_10#QUE312",
                    "https://github.com/qald_10#QUE313", "https://github.com/qald_10#QUE315",
                    "https://github.com/qald_10#QUE316", "https://github.com/qald_10#QUE317",
                    "https://github.com/qald_10#QUE318", "https://github.com/qald_10#QUE319",
                    "https://github.com/qald_10#QUE320", "https://github.com/qald_10#QUE321",
                    "https://github.com/qald_10#QUE322", "https://github.com/qald_10#QUE323",
                    "https://github.com/qald_10#QUE326", "https://github.com/qald_10#QUE329",
                    "https://github.com/qald_10#QUE330", "https://github.com/qald_10#QUE332",
                    "https://github.com/qald_10#QUE333", "https://github.com/qald_10#QUE334",
                    "https://github.com/qald_10#QUE336", "https://github.com/qald_10#QUE337",
                    "https://github.com/qald_10#QUE338", "https://github.com/qald_10#QUE340",
                    "https://github.com/qald_10#QUE342", "https://github.com/qald_10#QUE343",
                    "https://github.com/qald_10#QUE344", "https://github.com/qald_10#QUE345",
                    "https://github.com/qald_10#QUE352", "https://github.com/qald_10#QUE356",
                    "https://github.com/qald_10#QUE357", "https://github.com/qald_10#QUE358",
                    "https://github.com/qald_10#QUE359", "https://github.com/qald_10#QUE360",
                    "https://github.com/qald_10#QUE361", "https://github.com/qald_10#QUE362",
                    "https://github.com/qald_10#QUE363", "https://github.com/qald_10#QUE364",
                    "https://github.com/qald_10#QUE365", "https://github.com/qald_10#QUE366",
                    "https://github.com/qald_10#QUE367", "https://github.com/qald_10#QUE369",
                    "https://github.com/qald_10#QUE370", "https://github.com/qald_10#QUE372",
                    "https://github.com/qald_10#QUE373", "https://github.com/qald_10#QUE374",
                    "https://github.com/qald_10#QUE375", "https://github.com/qald_10#QUE376",
                    "https://github.com/qald_10#QUE377", "https://github.com/qald_10#QUE378",
                    "https://github.com/qald_10#QUE379", "https://github.com/qald_10#QUE380",
                    "https://github.com/qald_10#QUE381", "https://github.com/qald_10#QUE382",
                    "https://github.com/qald_10#QUE383", "https://github.com/qald_10#QUE384",
                    "https://github.com/qald_10#QUE387", "https://github.com/qald_10#QUE389",
                    "https://github.com/qald_10#QUE391", "https://github.com/qald_10#QUE392",
                    "https://github.com/qald_10#QUE393");
            List<String> mst5Correct = Arrays.asList("https://github.com/qald_10#QUE0",
                    "https://github.com/qald_10#QUE5", "https://github.com/qald_10#QUE9",
                    "https://github.com/qald_10#QUE10", "https://github.com/qald_10#QUE13",
                    "https://github.com/qald_10#QUE14", "https://github.com/qald_10#QUE15",
                    "https://github.com/qald_10#QUE17", "https://github.com/qald_10#QUE18",
                    "https://github.com/qald_10#QUE19", "https://github.com/qald_10#QUE22",
                    "https://github.com/qald_10#QUE24", "https://github.com/qald_10#QUE28",
                    "https://github.com/qald_10#QUE29", "https://github.com/qald_10#QUE30",
                    "https://github.com/qald_10#QUE39", "https://github.com/qald_10#QUE46",
                    "https://github.com/qald_10#QUE47", "https://github.com/qald_10#QUE52",
                    "https://github.com/qald_10#QUE62", "https://github.com/qald_10#QUE66",
                    "https://github.com/qald_10#QUE71", "https://github.com/qald_10#QUE73",
                    "https://github.com/qald_10#QUE74", "https://github.com/qald_10#QUE82",
                    "https://github.com/qald_10#QUE83", "https://github.com/qald_10#QUE84",
                    "https://github.com/qald_10#QUE97", "https://github.com/qald_10#QUE99",
                    "https://github.com/qald_10#QUE108", "https://github.com/qald_10#QUE111",
                    "https://github.com/qald_10#QUE113", "https://github.com/qald_10#QUE118",
                    "https://github.com/qald_10#QUE120", "https://github.com/qald_10#QUE121",
                    "https://github.com/qald_10#QUE122", "https://github.com/qald_10#QUE126",
                    "https://github.com/qald_10#QUE127", "https://github.com/qald_10#QUE128",
                    "https://github.com/qald_10#QUE133", "https://github.com/qald_10#QUE140",
                    "https://github.com/qald_10#QUE143", "https://github.com/qald_10#QUE144",
                    "https://github.com/qald_10#QUE148", "https://github.com/qald_10#QUE149",
                    "https://github.com/qald_10#QUE150", "https://github.com/qald_10#QUE151",
                    "https://github.com/qald_10#QUE152", "https://github.com/qald_10#QUE154",
                    "https://github.com/qald_10#QUE155", "https://github.com/qald_10#QUE158",
                    "https://github.com/qald_10#QUE159", "https://github.com/qald_10#QUE163",
                    "https://github.com/qald_10#QUE168", "https://github.com/qald_10#QUE169",
                    "https://github.com/qald_10#QUE172", "https://github.com/qald_10#QUE173",
                    "https://github.com/qald_10#QUE175", "https://github.com/qald_10#QUE177",
                    "https://github.com/qald_10#QUE180", "https://github.com/qald_10#QUE181",
                    "https://github.com/qald_10#QUE185", "https://github.com/qald_10#QUE189",
                    "https://github.com/qald_10#QUE198", "https://github.com/qald_10#QUE201",
                    "https://github.com/qald_10#QUE214", "https://github.com/qald_10#QUE215",
                    "https://github.com/qald_10#QUE216", "https://github.com/qald_10#QUE219",
                    "https://github.com/qald_10#QUE221", "https://github.com/qald_10#QUE230",
                    "https://github.com/qald_10#QUE231", "https://github.com/qald_10#QUE233",
                    "https://github.com/qald_10#QUE238", "https://github.com/qald_10#QUE243",
                    "https://github.com/qald_10#QUE250", "https://github.com/qald_10#QUE251",
                    "https://github.com/qald_10#QUE255", "https://github.com/qald_10#QUE259",
                    "https://github.com/qald_10#QUE261", "https://github.com/qald_10#QUE265",
                    "https://github.com/qald_10#QUE271", "https://github.com/qald_10#QUE273",
                    "https://github.com/qald_10#QUE274", "https://github.com/qald_10#QUE276",
                    "https://github.com/qald_10#QUE277", "https://github.com/qald_10#QUE281",
                    "https://github.com/qald_10#QUE282", "https://github.com/qald_10#QUE283",
                    "https://github.com/qald_10#QUE285", "https://github.com/qald_10#QUE292",
                    "https://github.com/qald_10#QUE294", "https://github.com/qald_10#QUE301",
                    "https://github.com/qald_10#QUE303", "https://github.com/qald_10#QUE307",
                    "https://github.com/qald_10#QUE314", "https://github.com/qald_10#QUE324",
                    "https://github.com/qald_10#QUE325", "https://github.com/qald_10#QUE327",
                    "https://github.com/qald_10#QUE328", "https://github.com/qald_10#QUE331",
                    "https://github.com/qald_10#QUE335", "https://github.com/qald_10#QUE339",
                    "https://github.com/qald_10#QUE341", "https://github.com/qald_10#QUE346",
                    "https://github.com/qald_10#QUE347", "https://github.com/qald_10#QUE348",
                    "https://github.com/qald_10#QUE349", "https://github.com/qald_10#QUE350",
                    "https://github.com/qald_10#QUE351", "https://github.com/qald_10#QUE353",
                    "https://github.com/qald_10#QUE354", "https://github.com/qald_10#QUE355",
                    "https://github.com/qald_10#QUE368", "https://github.com/qald_10#QUE371",
                    "https://github.com/qald_10#QUE385", "https://github.com/qald_10#QUE386",
                    "https://github.com/qald_10#QUE388", "https://github.com/qald_10#QUE390");

//            names.add("MST5 wrong answers vs. correct answers");
//            positives.add(mst5Wrong);
//            negatives.add(mst5Correct);

            names.add("MST5 correct answers vs. wrong answers");
            positives.add(mst5Correct);
            negatives.add(mst5Wrong);
//
//            names.add("MST5 wrong vs. correct cluster 1");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE332", "https://github.com/qald_10#QUE296",
//                    "https://github.com/qald_10#QUE174", "https://github.com/qald_10#QUE228",
//                    "https://github.com/qald_10#QUE70", "https://github.com/qald_10#QUE373"));
//
//            names.add("MST5 wrong vs. correct cluster 3");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE377", "https://github.com/qald_10#QUE142"));
//            names.add("MST5 wrong vs. correct cluster 4");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE362", "https://github.com/qald_10#QUE227",
//                    "https://github.com/qald_10#QUE359", "https://github.com/qald_10#QUE199",
//                    "https://github.com/qald_10#QUE179", "https://github.com/qald_10#QUE170",
//                    "https://github.com/qald_10#QUE57", "https://github.com/qald_10#QUE104",
//                    "https://github.com/qald_10#QUE90", "https://github.com/qald_10#QUE75",
//                    "https://github.com/qald_10#QUE232", "https://github.com/qald_10#QUE342",
//                    "https://github.com/qald_10#QUE340", "https://github.com/qald_10#QUE134",
//                    "https://github.com/qald_10#QUE313", "https://github.com/qald_10#QUE93",
//                    "https://github.com/qald_10#QUE264", "https://github.com/qald_10#QUE116",
//                    "https://github.com/qald_10#QUE262", "https://github.com/qald_10#QUE48",
//                    "https://github.com/qald_10#QUE222", "https://github.com/qald_10#QUE225",
//                    "https://github.com/qald_10#QUE165", "https://github.com/qald_10#QUE364",
//                    "https://github.com/qald_10#QUE360", "https://github.com/qald_10#QUE40",
//                    "https://github.com/qald_10#QUE161", "https://github.com/qald_10#QUE135",
//                    "https://github.com/qald_10#QUE258", "https://github.com/qald_10#QUE76",
//                    "https://github.com/qald_10#QUE382", "https://github.com/qald_10#QUE88",
//                    "https://github.com/qald_10#QUE304", "https://github.com/qald_10#QUE312",
//                    "https://github.com/qald_10#QUE376", "https://github.com/qald_10#QUE139",
//                    "https://github.com/qald_10#QUE137"));
//            names.add("MST5 wrong vs. correct cluster 5");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE156", "https://github.com/qald_10#QUE182",
//                    "https://github.com/qald_10#QUE193", "https://github.com/qald_10#QUE141",
//                    "https://github.com/qald_10#QUE53", "https://github.com/qald_10#QUE130",
//                    "https://github.com/qald_10#QUE200", "https://github.com/qald_10#QUE384"));
//            names.add("MST5 wrong vs. correct cluster 6");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE190", "https://github.com/qald_10#QUE49",
//                    "https://github.com/qald_10#QUE205", "https://github.com/qald_10#QUE27",
//                    "https://github.com/qald_10#QUE184", "https://github.com/qald_10#QUE330",
//                    "https://github.com/qald_10#QUE110", "https://github.com/qald_10#QUE280",
//                    "https://github.com/qald_10#QUE191", "https://github.com/qald_10#QUE213",
//                    "https://github.com/qald_10#QUE1", "https://github.com/qald_10#QUE107",
//                    "https://github.com/qald_10#QUE31", "https://github.com/qald_10#QUE112"));
//            names.add("MST5 wrong vs. correct cluster 7");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE234", "https://github.com/qald_10#QUE171",
//                    "https://github.com/qald_10#QUE249", "https://github.com/qald_10#QUE284"));
//            names.add("MST5 wrong vs. correct cluster 8");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE145", "https://github.com/qald_10#QUE333",
//                    "https://github.com/qald_10#QUE208", "https://github.com/qald_10#QUE206",
//                    "https://github.com/qald_10#QUE217", "https://github.com/qald_10#QUE363",
//                    "https://github.com/qald_10#QUE61", "https://github.com/qald_10#QUE374",
//                    "https://github.com/qald_10#QUE183", "https://github.com/qald_10#QUE309"));
//            names.add("MST5 wrong vs. correct cluster 10");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE334", "https://github.com/qald_10#QUE315",
//                    "https://github.com/qald_10#QUE308", "https://github.com/qald_10#QUE212",
//                    "https://github.com/qald_10#QUE115", "https://github.com/qald_10#QUE345",
//                    "https://github.com/qald_10#QUE210", "https://github.com/qald_10#QUE69",
//                    "https://github.com/qald_10#QUE370", "https://github.com/qald_10#QUE245",
//                    "https://github.com/qald_10#QUE12", "https://github.com/qald_10#QUE272",
//                    "https://github.com/qald_10#QUE43", "https://github.com/qald_10#QUE78",
//                    "https://github.com/qald_10#QUE102", "https://github.com/qald_10#QUE42"));
//            names.add("MST5 wrong vs. correct cluster 12");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE391", "https://github.com/qald_10#QUE3",
//                    "https://github.com/qald_10#QUE50", "https://github.com/qald_10#QUE248",
//                    "https://github.com/qald_10#QUE369", "https://github.com/qald_10#QUE64",
//                    "https://github.com/qald_10#QUE389", "https://github.com/qald_10#QUE105",
//                    "https://github.com/qald_10#QUE310", "https://github.com/qald_10#QUE81",
//                    "https://github.com/qald_10#QUE79", "https://github.com/qald_10#QUE119",
//                    "https://github.com/qald_10#QUE85", "https://github.com/qald_10#QUE7",
//                    "https://github.com/qald_10#QUE157"));
//            names.add("MST5 wrong vs. correct cluster 13");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE103", "https://github.com/qald_10#QUE4",
//                    "https://github.com/qald_10#QUE77", "https://github.com/qald_10#QUE224",
//                    "https://github.com/qald_10#QUE37", "https://github.com/qald_10#QUE32",
//                    "https://github.com/qald_10#QUE344", "https://github.com/qald_10#QUE235",
//                    "https://github.com/qald_10#QUE375", "https://github.com/qald_10#QUE20",
//                    "https://github.com/qald_10#QUE186", "https://github.com/qald_10#QUE379",
//                    "https://github.com/qald_10#QUE100", "https://github.com/qald_10#QUE393",
//                    "https://github.com/qald_10#QUE202"));
//            names.add("MST5 wrong vs. correct cluster 14");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE146", "https://github.com/qald_10#QUE338",
//                    "https://github.com/qald_10#QUE123", "https://github.com/qald_10#QUE242",
//                    "https://github.com/qald_10#QUE204", "https://github.com/qald_10#QUE253",
//                    "https://github.com/qald_10#QUE320", "https://github.com/qald_10#QUE203",
//                    "https://github.com/qald_10#QUE195", "https://github.com/qald_10#QUE357",
//                    "https://github.com/qald_10#QUE289", "https://github.com/qald_10#QUE124",
//                    "https://github.com/qald_10#QUE226", "https://github.com/qald_10#QUE194",
//                    "https://github.com/qald_10#QUE311", "https://github.com/qald_10#QUE352",
//                    "https://github.com/qald_10#QUE268", "https://github.com/qald_10#QUE252",
//                    "https://github.com/qald_10#QUE240", "https://github.com/qald_10#QUE94",
//                    "https://github.com/qald_10#QUE237", "https://github.com/qald_10#QUE45",
//                    "https://github.com/qald_10#QUE131", "https://github.com/qald_10#QUE132",
//                    "https://github.com/qald_10#QUE6", "https://github.com/qald_10#QUE167",
//                    "https://github.com/qald_10#QUE34"));
//            names.add("MST5 wrong vs. correct cluster 15");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE211", "https://github.com/qald_10#QUE2",
//                    "https://github.com/qald_10#QUE11", "https://github.com/qald_10#QUE367",
//                    "https://github.com/qald_10#QUE220", "https://github.com/qald_10#QUE207",
//                    "https://github.com/qald_10#QUE254"));
//            names.add("MST5 wrong vs. correct cluster 16");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE365", "https://github.com/qald_10#QUE278",
//                    "https://github.com/qald_10#QUE361", "https://github.com/qald_10#QUE117",
//                    "https://github.com/qald_10#QUE187", "https://github.com/qald_10#QUE72",
//                    "https://github.com/qald_10#QUE247", "https://github.com/qald_10#QUE125",
//                    "https://github.com/qald_10#QUE241"));
//            names.add("MST5 wrong vs. correct cluster 17");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE329", "https://github.com/qald_10#QUE358",
//                    "https://github.com/qald_10#QUE267", "https://github.com/qald_10#QUE98",
//                    "https://github.com/qald_10#QUE63", "https://github.com/qald_10#QUE59",
//                    "https://github.com/qald_10#QUE298", "https://github.com/qald_10#QUE21",
//                    "https://github.com/qald_10#QUE166", "https://github.com/qald_10#QUE322",
//                    "https://github.com/qald_10#QUE58"));
//            names.add("MST5 wrong vs. correct cluster 18");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE164", "https://github.com/qald_10#QUE192",
//                    "https://github.com/qald_10#QUE383", "https://github.com/qald_10#QUE239",
//                    "https://github.com/qald_10#QUE89", "https://github.com/qald_10#QUE80",
//                    "https://github.com/qald_10#QUE138", "https://github.com/qald_10#QUE176",
//                    "https://github.com/qald_10#QUE316", "https://github.com/qald_10#QUE290",
//                    "https://github.com/qald_10#QUE244", "https://github.com/qald_10#QUE92",
//                    "https://github.com/qald_10#QUE136", "https://github.com/qald_10#QUE33",
//                    "https://github.com/qald_10#QUE162", "https://github.com/qald_10#QUE321",
//                    "https://github.com/qald_10#QUE196", "https://github.com/qald_10#QUE223",
//                    "https://github.com/qald_10#QUE392", "https://github.com/qald_10#QUE67",
//                    "https://github.com/qald_10#QUE209", "https://github.com/qald_10#QUE55",
//                    "https://github.com/qald_10#QUE44", "https://github.com/qald_10#QUE147",
//                    "https://github.com/qald_10#QUE269", "https://github.com/qald_10#QUE26",
//                    "https://github.com/qald_10#QUE229"));
//            names.add("MST5 wrong vs. correct cluster 19");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE23", "https://github.com/qald_10#QUE8",
//                    "https://github.com/qald_10#QUE109", "https://github.com/qald_10#QUE260",
//                    "https://github.com/qald_10#QUE297", "https://github.com/qald_10#QUE129",
//                    "https://github.com/qald_10#QUE306"));
//            names.add("MST5 wrong vs. correct cluster 20");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE256", "https://github.com/qald_10#QUE87",
//                    "https://github.com/qald_10#QUE188", "https://github.com/qald_10#QUE378",
//                    "https://github.com/qald_10#QUE356", "https://github.com/qald_10#QUE106",
//                    "https://github.com/qald_10#QUE302", "https://github.com/qald_10#QUE291",
//                    "https://github.com/qald_10#QUE60", "https://github.com/qald_10#QUE41",
//                    "https://github.com/qald_10#QUE286", "https://github.com/qald_10#QUE270",
//                    "https://github.com/qald_10#QUE275"));
//            names.add("MST5 wrong vs. correct cluster 21");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE54", "https://github.com/qald_10#QUE263",
//                    "https://github.com/qald_10#QUE257", "https://github.com/qald_10#QUE25",
//                    "https://github.com/qald_10#QUE114"));
//            names.add("MST5 wrong vs. correct cluster 22");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE65", "https://github.com/qald_10#QUE38",
//                    "https://github.com/qald_10#QUE305", "https://github.com/qald_10#QUE218",
//                    "https://github.com/qald_10#QUE96", "https://github.com/qald_10#QUE56",
//                    "https://github.com/qald_10#QUE16"));
//            names.add("MST5 wrong vs. correct cluster 23");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE319", "https://github.com/qald_10#QUE387",
//                    "https://github.com/qald_10#QUE343", "https://github.com/qald_10#QUE197",
//                    "https://github.com/qald_10#QUE317"));
//            names.add("MST5 wrong vs. correct cluster 24");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE323", "https://github.com/qald_10#QUE366",
//                    "https://github.com/qald_10#QUE293", "https://github.com/qald_10#QUE95"));
//            names.add("MST5 wrong vs. correct cluster 25");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE372", "https://github.com/qald_10#QUE36",
//                    "https://github.com/qald_10#QUE326", "https://github.com/qald_10#QUE160",
//                    "https://github.com/qald_10#QUE101"));
//            names.add("MST5 wrong vs. correct cluster 26");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE287", "https://github.com/qald_10#QUE91",
//                    "https://github.com/qald_10#QUE381", "https://github.com/qald_10#QUE279",
//                    "https://github.com/qald_10#QUE295", "https://github.com/qald_10#QUE246",
//                    "https://github.com/qald_10#QUE51", "https://github.com/qald_10#QUE86"));
//            names.add("MST5 wrong vs. correct cluster 27");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE380", "https://github.com/qald_10#QUE300",
//                    "https://github.com/qald_10#QUE336"));
//            names.add("MST5 wrong vs. correct cluster 28");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE299", "https://github.com/qald_10#QUE178"));
//            names.add("MST5 wrong vs. correct cluster 29");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE35", "https://github.com/qald_10#QUE236",
//                    "https://github.com/qald_10#QUE288", "https://github.com/qald_10#QUE153",
//                    "https://github.com/qald_10#QUE318"));
//            names.add("MST5 wrong vs. correct cluster 30");
//            negatives.add(mst5Correct);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE337", "https://github.com/qald_10#QUE266"));

//            names.add("MST5 correct vs. wrong cluster 2");
//            negatives.add(mst5Wrong);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE335", "https://github.com/qald_10#QUE83",
//                    "https://github.com/qald_10#QUE347", "https://github.com/qald_10#QUE9",
//                    "https://github.com/qald_10#QUE276", "https://github.com/qald_10#QUE230",
//                    "https://github.com/qald_10#QUE127", "https://github.com/qald_10#QUE0",
//                    "https://github.com/qald_10#QUE152", "https://github.com/qald_10#QUE251",
//                    "https://github.com/qald_10#QUE331", "https://github.com/qald_10#QUE154",
//                    "https://github.com/qald_10#QUE155", "https://github.com/qald_10#QUE158",
//                    "https://github.com/qald_10#QUE15", "https://github.com/qald_10#QUE99",
//                    "https://github.com/qald_10#QUE388", "https://github.com/qald_10#QUE221",
//                    "https://github.com/qald_10#QUE303", "https://github.com/qald_10#QUE277",
//                    "https://github.com/qald_10#QUE324", "https://github.com/qald_10#QUE140",
//                    "https://github.com/qald_10#QUE219", "https://github.com/qald_10#QUE71",
//                    "https://github.com/qald_10#QUE273", "https://github.com/qald_10#QUE180",
//                    "https://github.com/qald_10#QUE339", "https://github.com/qald_10#QUE74",
//                    "https://github.com/qald_10#QUE238", "https://github.com/qald_10#QUE133",
//                    "https://github.com/qald_10#QUE121", "https://github.com/qald_10#QUE113",
//                    "https://github.com/qald_10#QUE231", "https://github.com/qald_10#QUE173",
//                    "https://github.com/qald_10#QUE169", "https://github.com/qald_10#QUE386",
//                    "https://github.com/qald_10#QUE126", "https://github.com/qald_10#QUE215",
//                    "https://github.com/qald_10#QUE271", "https://github.com/qald_10#QUE13",
//                    "https://github.com/qald_10#QUE307", "https://github.com/qald_10#QUE350",
//                    "https://github.com/qald_10#QUE349", "https://github.com/qald_10#QUE261",
//                    "https://github.com/qald_10#QUE189", "https://github.com/qald_10#QUE351",
//                    "https://github.com/qald_10#QUE314", "https://github.com/qald_10#QUE10",
//                    "https://github.com/qald_10#QUE198", "https://github.com/qald_10#QUE274",
//                    "https://github.com/qald_10#QUE172", "https://github.com/qald_10#QUE14",
//                    "https://github.com/qald_10#QUE52", "https://github.com/qald_10#QUE368",
//                    "https://github.com/qald_10#QUE355", "https://github.com/qald_10#QUE82",
//                    "https://github.com/qald_10#QUE29", "https://github.com/qald_10#QUE348",
//                    "https://github.com/qald_10#QUE111", "https://github.com/qald_10#QUE294",
//                    "https://github.com/qald_10#QUE292", "https://github.com/qald_10#QUE385",
//                    "https://github.com/qald_10#QUE84", "https://github.com/qald_10#QUE28",
//                    "https://github.com/qald_10#QUE73", "https://github.com/qald_10#QUE18",
//                    "https://github.com/qald_10#QUE144", "https://github.com/qald_10#QUE175",
//                    "https://github.com/qald_10#QUE325", "https://github.com/qald_10#QUE128",
//                    "https://github.com/qald_10#QUE181", "https://github.com/qald_10#QUE243",
//                    "https://github.com/qald_10#QUE250", "https://github.com/qald_10#QUE149",
//                    "https://github.com/qald_10#QUE282"));
//            names.add("MST5 correct vs. wrong cluster 9");
//            negatives.add(mst5Wrong);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE22", "https://github.com/qald_10#QUE159",
//                    "https://github.com/qald_10#QUE163", "https://github.com/qald_10#QUE327",
//                    "https://github.com/qald_10#QUE30", "https://github.com/qald_10#QUE255",
//                    "https://github.com/qald_10#QUE216", "https://github.com/qald_10#QUE17",
//                    "https://github.com/qald_10#QUE122", "https://github.com/qald_10#QUE108",
//                    "https://github.com/qald_10#QUE233"));
//            names.add("MST5 correct vs. wrong cluster 11");
//            negatives.add(mst5Wrong);
//            positives.add(Arrays.asList("https://github.com/qald_10#QUE97", "https://github.com/qald_10#QUE120",
//                    "https://github.com/qald_10#QUE346", "https://github.com/qald_10#QUE168",
//                    "https://github.com/qald_10#QUE66", "https://github.com/qald_10#QUE5",
//                    "https://github.com/qald_10#QUE265", "https://github.com/qald_10#QUE201",
//                    "https://github.com/qald_10#QUE301", "https://github.com/qald_10#QUE371",
//                    "https://github.com/qald_10#QUE214", "https://github.com/qald_10#QUE24",
//                    "https://github.com/qald_10#QUE19", "https://github.com/qald_10#QUE62",
//                    "https://github.com/qald_10#QUE283", "https://github.com/qald_10#QUE143",
//                    "https://github.com/qald_10#QUE259", "https://github.com/qald_10#QUE281",
//                    "https://github.com/qald_10#QUE118", "https://github.com/qald_10#QUE390",
//                    "https://github.com/qald_10#QUE47", "https://github.com/qald_10#QUE150",
//                    "https://github.com/qald_10#QUE341", "https://github.com/qald_10#QUE353",
//                    "https://github.com/qald_10#QUE151", "https://github.com/qald_10#QUE354",
//                    "https://github.com/qald_10#QUE177", "https://github.com/qald_10#QUE185"));

            // Aunt problem
//            Collection<String> positive = Arrays.asList("http://www.benchmark.org/family#F2F14",
//                    "http://www.benchmark.org/family#F2F12", "http://www.benchmark.org/family#F2F19",
//                    "http://www.benchmark.org/family#F2F26", "http://www.benchmark.org/family#F2F28",
//                    "http://www.benchmark.org/family#F2F36", "http://www.benchmark.org/family#F3F52",
//                    "http://www.benchmark.org/family#F3F53", "http://www.benchmark.org/family#F5F62",
//                    "http://www.benchmark.org/family#F6F72", "http://www.benchmark.org/family#F6F79",
//                    "http://www.benchmark.org/family#F6F77", "http://www.benchmark.org/family#F6F86",
//                    "http://www.benchmark.org/family#F6F91", "http://www.benchmark.org/family#F6F84",
//                    "http://www.benchmark.org/family#F6F96", "http://www.benchmark.org/family#F6F101",
//                    "http://www.benchmark.org/family#F6F93", "http://www.benchmark.org/family#F7F114",
//                    "http://www.benchmark.org/family#F7F106", "http://www.benchmark.org/family#F7F116",
//                    "http://www.benchmark.org/family#F7F119", "http://www.benchmark.org/family#F7F126",
//                    "http://www.benchmark.org/family#F7F121", "http://www.benchmark.org/family#F9F148",
//                    "http://www.benchmark.org/family#F9F150", "http://www.benchmark.org/family#F9F143",
//                    "http://www.benchmark.org/family#F9F152", "http://www.benchmark.org/family#F9F154",
//                    "http://www.benchmark.org/family#F9F141", "http://www.benchmark.org/family#F9F160",
//                    "http://www.benchmark.org/family#F9F163", "http://www.benchmark.org/family#F9F158",
//                    "http://www.benchmark.org/family#F9F168", "http://www.benchmark.org/family#F10F174",
//                    "http://www.benchmark.org/family#F10F179", "http://www.benchmark.org/family#F10F181",
//                    "http://www.benchmark.org/family#F10F192", "http://www.benchmark.org/family#F10F193",
//                    "http://www.benchmark.org/family#F10F186", "http://www.benchmark.org/family#F10F195");
//            Collection<String> negative = Arrays.asList("http://www.benchmark.org/family#F6M99",
//                    "http://www.benchmark.org/family#F10F200", "http://www.benchmark.org/family#F9F156",
//                    "http://www.benchmark.org/family#F6M69", "http://www.benchmark.org/family#F2F15",
//                    "http://www.benchmark.org/family#F6M100", "http://www.benchmark.org/family#F8F133",
//                    "http://www.benchmark.org/family#F3F48", "http://www.benchmark.org/family#F2F30",
//                    "http://www.benchmark.org/family#F4F55", "http://www.benchmark.org/family#F6F74",
//                    "http://www.benchmark.org/family#F10M199", "http://www.benchmark.org/family#F7M104",
//                    "http://www.benchmark.org/family#F9M146", "http://www.benchmark.org/family#F6M71",
//                    "http://www.benchmark.org/family#F2F22", "http://www.benchmark.org/family#F2M13",
//                    "http://www.benchmark.org/family#F9F169", "http://www.benchmark.org/family#F5F65",
//                    "http://www.benchmark.org/family#F6M81", "http://www.benchmark.org/family#F7M131",
//                    "http://www.benchmark.org/family#F7F129", "http://www.benchmark.org/family#F7M107",
//                    "http://www.benchmark.org/family#F10F189", "http://www.benchmark.org/family#F8F135",
//                    "http://www.benchmark.org/family#F8M136", "http://www.benchmark.org/family#F10M188",
//                    "http://www.benchmark.org/family#F9F164", "http://www.benchmark.org/family#F7F118",
//                    "http://www.benchmark.org/family#F2F10", "http://www.benchmark.org/family#F6F97",
//                    "http://www.benchmark.org/family#F7F111", "http://www.benchmark.org/family#F9M151",
//                    "http://www.benchmark.org/family#F4M59", "http://www.benchmark.org/family#F2M37",
//                    "http://www.benchmark.org/family#F1M1", "http://www.benchmark.org/family#F9M142",
//                    "http://www.benchmark.org/family#F4M57", "http://www.benchmark.org/family#F9M170",
//                    "http://www.benchmark.org/family#F5M66", "http://www.benchmark.org/family#F9F145");

            // DEBUG CODE!!!
//            ClassExpression ce = new Junction(true,
//                    new SimpleQuantifiedRole(true, "http://quans-namespace.org/#HasQuery", false,
//                            new Junction(true, Suggestor.CONTEXT_POSITION_MARKER,
//                                    new SimpleQuantifiedRole(true, "http://lsq.aksw.org/vocab#usesFeature", false,
//                                            NamedClass.TOP))),
//                    new SimpleQuantifiedRole(true, "http://quans-namespace.org/#HasIRIAns", false, NamedClass.TOP),
//                    new SimpleQuantifiedRole(true,
//                            "http://quans-namespace.org/DependencyParseTree/HasNLPDependencyParseTree", false,
//                            new SimpleQuantifiedRole(true,
//                                    "http://quans-namespace.org/DependencyParseTree/NLPDependencyParseTreeRelation#obj",
//                                    false, NamedClass.TOP)));
//            suggestor.suggestClass(positive, negative, ce);
            // DEBUG CODE END!!!
            try (PrintStream pout = new PrintStream("mst5-results.txt")) {
                for (int i = 0; i < names.size(); ++i) {
                    runSearch(names.get(i), positives.get(i), negatives.get(i), cel, pout);
                }
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
            PrintStream pout) {
        System.out.println("Starting " + name);
        long time = System.currentTimeMillis();
        List<ScoredClassExpression> expressions = cel.findClassExpression(positive, negative);
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
