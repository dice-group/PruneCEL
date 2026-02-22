package org.dice_research.cel.tree;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javolution.util.FastBitSet;

/**
 * 
 * Very simple class that records "patterns" of features, i.e., whether a
 * feature with the same bitsets has been seen before.
 *
 */
public class SimplePatternRecorder {
    protected Set<FeatureKey> seenPatterns = new HashSet<>();

    /**
     * Adds the given {@link Feature} to the internal list of feature patterns and
     * returns {@code true} if there was no other feature with the same Bitsets as
     * this feature. Else {@code false} is returned.
     * 
     * @param feature
     * @return
     */
    public boolean add(Feature feature) {
        return seenPatterns.add(new FeatureKey(feature.getSelectedPositives(), feature.getSelectedNegatives()));
    }

    protected static class FeatureKey implements Feature {
        protected FastBitSet pos;
        protected FastBitSet neg;

        public FeatureKey(FastBitSet pos, FastBitSet neg) {
            super();
            this.pos = pos;
            this.neg = neg;
        }

        @Override
        public FastBitSet getSelectedPositives() {
            return pos;
        }

        @Override
        public FastBitSet getSelectedNegatives() {
            return neg;
        }

        @Override
        public int hashCode() {
            return Objects.hash(neg, pos);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            FeatureKey other = (FeatureKey) obj;
            return Objects.equals(neg, other.neg) && Objects.equals(pos, other.pos);
        }
    }
}
