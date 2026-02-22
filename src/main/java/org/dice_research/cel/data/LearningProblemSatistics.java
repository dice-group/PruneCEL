package org.dice_research.cel.data;

/**
 * A simple structure carrying the basic statistical information of a learning
 * problem.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class LearningProblemSatistics {

    /**
     * The number of positive examples of a learning problem.
     */
    public final int numberOfPositives;

    /**
     * The number of negative examples of a learning problem.
     */
    public final int numberOfNegatives;

    /**
     * The number of examples of a learning problem. It is the sum of the positive
     * and negative examples.
     */
    public final int numberOfExamples;

    /**
     * Constructor.
     * 
     * @param numberOfPositives The number of positive examples of a learning
     *                          problem.
     * @param numberOfNegatives The number of negative examples of a learning
     *                          problem.
     */
    public LearningProblemSatistics(int numberOfPositives, int numberOfNegatives) {
        super();
        this.numberOfPositives = numberOfPositives;
        this.numberOfNegatives = numberOfNegatives;
        this.numberOfExamples = numberOfPositives + numberOfNegatives;
    }
}
