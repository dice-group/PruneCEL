package org.dice_research.cel.tree;

import javolution.util.FastBitSet;

public interface Feature {

    FastBitSet getSelectedPositives();

    FastBitSet getSelectedNegatives();
}
