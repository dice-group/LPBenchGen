package org.dice_group.lpbenchgen.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.semanticweb.owlapi.model.OWLClassExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * The Positive Negative Concept template
 *
 * @author Lixi Alié Conrads
 */
public class PosNegExample {

    @JsonProperty(required = true)
    private String positive;
    @JsonProperty(required = false)
    private List<String> negatives = new ArrayList<String>();

    private List<OWLClassExpression> negativesExpr = new ArrayList<OWLClassExpression>();
    private boolean negativeGenerated=false;

    /**
     * If negative concepts were generated
     * @return if the negative concepts were generated
     */
    public boolean isNegativeGenerated() {
        return negativeGenerated;
    }

    /**
     * If negative concepts were generated
     * @param negativeGenerated sets if the negative concepts were/are generated
     */
    public void setNegativeGenerated(boolean negativeGenerated) {
        this.negativeGenerated = negativeGenerated;
    }

    /**
     * Gets positive concept.
     *
     * @return the positive
     */
    public String getPositive() {
        return positive;
    }

    /**
     * Sets positive concept.
     *
     * @param positive the positive
     */
    public void setPositive(String positive) {
        this.positive = positive;
    }

    /**
     * Gets negatives concepts.
     *
     * @return the negatives
     */
    public List<OWLClassExpression> getNegativesExpr() {
        return negativesExpr;
    }

    /**
     * Sets negatives concepts.
     *
     * @param negativesExpr the negatives
     */
    public void setNegativesExpr(List<OWLClassExpression> negativesExpr) {
        this.negativesExpr = negativesExpr;
    }

    @Override
    public String toString(){
        return positive;
    }

    @Override
    public int hashCode(){
        return positive.hashCode();
    }

    @Override
    public boolean equals(Object anotherObject){
        if(anotherObject instanceof PosNegExample){
            anotherObject.toString().equals(positive);
        }
        return false;
    }

    /**
     * Gets the negative concepts as strings - only set if loaded from config
     *
     * @return negative concepts as string
     */
    public List<String> getNegatives() {
        return negatives;
    }
}
