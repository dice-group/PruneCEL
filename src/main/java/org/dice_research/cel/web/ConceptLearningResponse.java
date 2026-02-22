package org.dice_research.cel.web;

public class ConceptLearningResponse {

    public String concept;

    public ConceptLearningResponse(String concept) {
        super();
        this.concept = concept;
    }

    /**
     * @return the concept
     */
    public String getConcept() {
        return concept;
    }

    /**
     * @param concept the concept to set
     */
    public void setConcept(String concept) {
        this.concept = concept;
    }
    
    
}
