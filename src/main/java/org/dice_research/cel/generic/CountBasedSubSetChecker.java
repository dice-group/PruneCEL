package org.dice_research.cel.generic;

/**
 * A simple class that implements checking a given subsets size based on a given
 * absolute or relative threshold. The latter is measured in comparison to the
 * size of the complete set the given set is a subset of.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class CountBasedSubSetChecker {

    /**
     * Minimum absolute size a sub set should have.
     */
    protected int minAbsCardinality = 2;
    /**
     * Relative size of the minimum size of a sub set measured based on the size of
     * the complete set, i.e., [0,1].
     */
    protected double minRelCardinality = 0.0;

    /**
     * Constructor.
     * 
     * @param minAbsCardinality Minimum absolute size a sub set should have. If it
     *                          is 0 or smaller, checks based on the absolute size
     *                          are ignored.
     * @param minRelCardinality Relative size of the minimum cardinality of a set
     *                          measured based on the size of the complete set,
     *                          i.e., [0,1]. If it is 0 or smaller, checks based on
     *                          this relative cardinality are ignored.
     */
    public CountBasedSubSetChecker(int minAbsCardinality, double minRelCardinality) {
        super();
        this.minAbsCardinality = minAbsCardinality;
        this.minRelCardinality = minRelCardinality;
    }

    /**
     * Checks whether the given set fulfills the minimum size.
     * 
     * @param subsetSize      the size of the subset
     * @param completeSetSize the size of the complete set the given set is a subset
     *                        of
     * @return {@code true} if the given set fulfills the pre-defined minimum size
     *         constraints.
     */
    public boolean isSetGood(int subsetSize, int completeSetSize) {
        return // Check absolute size
        ((minAbsCardinality <= 0) || (subsetSize >= minAbsCardinality))
                // check relative size
                && ((minRelCardinality <= 0) || (subsetSize >= (minRelCardinality * completeSetSize)));
    }
}
