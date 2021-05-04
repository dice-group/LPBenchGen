package org.dice_group.lpbenchgen.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.semanticweb.owlapi.model.OWLClassExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * The type Pos neg example.
 *
 * @author Lixi Alié Conrads
 */
public class PosNegExample {

    @JsonProperty(required = true)
    private String positive;
    @JsonProperty(required = false)
    private List<OWLClassExpression> negatives = new ArrayList<OWLClassExpression>();
    private boolean negativeGenerated=false;

    public boolean isNegativeGenerated() {
        return negativeGenerated;
    }

    public void setNegativeGenerated(boolean negativeGenerated) {
        this.negativeGenerated = negativeGenerated;
    }

    /**
     * Gets positive.
     *
     * @return the positive
     */
    public String getPositive() {
        return positive;
    }

    /**
     * Sets positive.
     *
     * @param positive the positive
     */
    public void setPositive(String positive) {
        this.positive = positive;
    }

    /**
     * Gets negatives.
     *
     * @return the negatives
     */
    public List<OWLClassExpression> getNegatives() {
        return negatives;
    }

    /**
     * Sets negatives.
     *
     * @param negatives the negatives
     */
    public void setNegatives(List<OWLClassExpression> negatives) {
        this.negatives = negatives;
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
}