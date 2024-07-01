package org.dice_research.cel.score;

public interface ScoreCalculatorFactory {

    ScoreCalculator create(int numOfPositives, int numOfNegatives);

}
