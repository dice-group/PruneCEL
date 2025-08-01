package org.dice_research.cel.refine.instances;

import java.util.Map;
import java.util.Set;

import org.dice_research.cel.refine.suggest.ScoredIRI;
import org.dice_research.cel.tree.Feature;

import javolution.util.FastBitSet;

public class ScoredIRIWithInstances extends ScoredIRI implements Feature {

    protected FastBitSet selectedPos;
    protected FastBitSet selectedNeg;

    public ScoredIRIWithInstances(String iri, FastBitSet selectedPos, FastBitSet selectedNeg) {
        this(iri, selectedPos, selectedNeg, selectedPos.cardinality(), selectedNeg.cardinality());
    }

    public ScoredIRIWithInstances(String iri, FastBitSet selectedPos, FastBitSet selectedNeg, int posCount, int negCount) {
        super(iri, posCount, negCount);
        this.selectedPos = selectedPos;
        this.selectedNeg = selectedNeg;
    }

    public static ScoredIRIWithInstances create(String iri, Set<String> examples, Map<String, Integer> positiveMaps,
            Map<String, Integer> negativeMaps) {
        FastBitSet selectedPos = FastBitSet.newInstance();
        selectedPos.set(0, positiveMaps.size());
        FastBitSet selectedNeg = FastBitSet.newInstance();
        selectedNeg.set(0, negativeMaps.size());
        Integer id = null;
        for (String example : examples) {
            id = positiveMaps.get(example);
            if (id != null) {
                selectedPos.set(id);
            } else {
                id = negativeMaps.get(example);
                if (id != null) {
                    selectedNeg.set(id);
                }
            }
        }
        return new ScoredIRIWithInstances(iri, selectedPos, selectedNeg);
    }

    @Override
    public FastBitSet getSelectedPositives() {
        return selectedPos;
    }

    @Override
    public FastBitSet getSelectedNegatives() {
        return selectedNeg;
    }

    public void add(FastBitSet selectedPos, FastBitSet selectedNeg) {
        this.selectedPos.and(selectedPos);
        this.posCount = this.selectedPos.cardinality();
        this.selectedNeg.and(selectedPos);
        this.negCount = this.selectedPos.cardinality();
    }

}