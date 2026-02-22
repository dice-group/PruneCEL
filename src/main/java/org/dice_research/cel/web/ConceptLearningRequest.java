package org.dice_research.cel.web;

public class ConceptLearningRequest {

    protected String[] pos;
    protected String[] neg;
    protected String model;
    protected String endpoint;
    protected long max_runtime = 0;
    protected int iter_bound = 0;

//    public ConceptLearningRequest(String[] pos, String[] neg, String model, String model, long max_runtime, int iter_bound) {
//        super();
//        this.positives = pos;
//        this.negatives = neg;
//        this.model = model;
//        this.maxRunime = max_runtime;
//        this.iterBound = iter_bound;
//    }
//
//    public ConceptLearningRequest(String[] pos, String[] neg, String model, String model, long max_runtime) {
//        this(pos, neg, model, max_runtime, 0);
//    }
//
//    public ConceptLearningRequest(String[] pos, String[] neg, String model, String model, int iter_bound) {
//        this(pos, neg, model, 0, iter_bound);
//    }

    /**
     * @return the model
     */
    public String getModel() {
        return model;
    }

    /**
     * @return the pos
     */
    public String[] getPos() {
        return pos;
    }

    /**
     * @param pos the pos to set
     */
    public void setPos(String[] pos) {
        this.pos = pos;
    }

    /**
     * @return the neg
     */
    public String[] getNeg() {
        return neg;
    }

    /**
     * @param neg the neg to set
     */
    public void setNeg(String[] neg) {
        this.neg = neg;
    }

    /**
     * @param model the model to set
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * @return the endpoint
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * @param endpoint the endpoint to set
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * @return the max_runtime
     */
    public long getMax_runtime() {
        return max_runtime;
    }

    /**
     * @param max_runtime the max_runtime to set
     */
    public void setMax_runtime(long max_runtime) {
        this.max_runtime = max_runtime;
    }

    /**
     * @return the iter_bound
     */
    public int getIter_bound() {
        return iter_bound;
    }

    /**
     * @param iter_bound the iter_bound to set
     */
    public void setIter_bound(int iter_bound) {
        this.iter_bound = iter_bound;
    }
}
