package org.dice_research.cel.tree.select;

import org.junit.Assert;
import org.junit.Test;

public class InformationGainBasedComparatorTest {

    @Test
    public void test() {
        /*
         * Example from Wikipedia
         * (https://en.wikipedia.org/wiki/Decision_tree_learning#Information_gain)
         * 
         * Consider an example data set with four attributes: outlook (sunny, overcast,
         * rainy), temperature (hot, mild, cool), humidity (high, normal), and windy
         * (true, false), with a binary (yes or no) target variable, play, and 14 data
         * points. To construct a decision tree on this data, we need to compare the
         * information gain of each of four trees, each split on one of the four
         * features. The split with the highest information gain will be taken as the
         * first split and the process will continue until all children nodes each have
         * consistent data, or until the information gain is 0.
         * 
         * To find the information gain of the split using windy, we must first
         * calculate the information in the data before the split. The original data
         * contained nine yes's and five no's.
         * 
         * {\displaystyle I_{E}([9,5])=-{\frac {9}{14}}\log _{2}{\frac {9}{14}}-{\frac
         * {5}{14}}\log _{2}{\frac {5}{14}}=0.94}
         */
        Assert.assertEquals(0.94, InformationGainBasedComparator.calculateEntropy(9, 5), 0.005);
        /*
         * The split using the feature windy results in two children nodes, one for a
         * windy value of true and one for a windy value of false. In this data set,
         * there are six data points with a true windy value, three of which have a play
         * (where play is the target variable) value of yes and three with a play value
         * of no. The eight remaining data points with a windy value of false contain
         * two no's and six yes's. The information of the windy=true node is calculated
         * using the entropy equation above. Since there is an equal number of yes's and
         * no's in this node, we have
         * 
         * I E ( [ 3 , 3 ] ) = − 3 6 log 2 ⁡ 3 6 − 3 6 log 2 ⁡ 3 6 = − 1 2 log 2 ⁡ 1 2 −
         * 1 2 log 2 ⁡ 1 2 = 1 {\displaystyle I_{E}([3,3])=-{\frac {3}{6}}\log
         * _{2}{\frac {3}{6}}-{\frac {3}{6}}\log _{2}{\frac {3}{6}}=-{\frac {1}{2}}\log
         * _{2}{\frac {1}{2}}-{\frac {1}{2}}\log _{2}{\frac {1}{2}}=1}
         */
        Assert.assertEquals(1.0, InformationGainBasedComparator.calculateEntropy(3, 3), 0.005);
        /*
         * For the node where windy=false there were eight data points, six yes's and
         * two no's. Thus we have
         * 
         * I E ( [ 6 , 2 ] ) = − 6 8 log 2 ⁡ 6 8 − 2 8 log 2 ⁡ 2 8 = − 3 4 log 2 ⁡ 3 4 −
         * 1 4 log 2 ⁡ 1 4 = 0.81 {\displaystyle I_{E}([6,2])=-{\frac {6}{8}}\log
         * _{2}{\frac {6}{8}}-{\frac {2}{8}}\log _{2}{\frac {2}{8}}=-{\frac {3}{4}}\log
         * _{2}{\frac {3}{4}}-{\frac {1}{4}}\log _{2}{\frac {1}{4}}=0.81}
         */
        Assert.assertEquals(0.81, InformationGainBasedComparator.calculateEntropy(6, 2), 0.005);
        /*
         * To find the information of the split, we take the weighted average of these
         * two numbers based on how many observations fell into which node.
         * 
         * I E ( [ 3 , 3 ] , [ 6 , 2 ] ) = I E ( windy or not ) = 6 14 ⋅ 1 + 8 14 ⋅ 0.81
         * = 0.89 {\displaystyle I_{E}([3,3],[6,2])=I_{E}({\text{windy or not}})={\frac
         * {6}{14}}\cdot 1+{\frac {8}{14}}\cdot 0.81=0.89}
         * 
         * Now we can calculate the information gain achieved by splitting on the windy
         * feature.
         * 
         * IG ⁡ ( windy ) = I E ( [ 9 , 5 ] ) − I E ( [ 3 , 3 ] , [ 6 , 2 ] ) = 0.94 −
         * 0.89 = 0.05 {\displaystyle \operatorname {IG}
         * ({\text{windy}})=I_{E}([9,5])-I_{E}([3,3],[6,2])=0.94-0.89=0.05}
         */
        Assert.assertEquals(0.05, InformationGainBasedComparator.calculateInformationGain(9, 5, 6, 2), 0.005);
        Assert.assertEquals(0.05, InformationGainBasedComparator.calculateInformationGain(9, 5, 3, 3), 0.005);
        /*
         * 
         * To build the tree, the information gain of each possible first split would
         * need to be calculated. The best first split is the one that provides the most
         * information gain.
         */
    }
}
