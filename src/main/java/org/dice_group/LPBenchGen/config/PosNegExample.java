package org.dice_group.LPBenchGen.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.semanticweb.owlapi.model.OWLClassExpression;

import java.util.ArrayList;
import java.util.List;

public class PosNegExample {

    @JsonProperty(required = true)
    private String positive;
    @JsonProperty(required = false)
    private List<OWLClassExpression> negatives = new ArrayList<OWLClassExpression>();

    public String getPositive() {
        return positive;
    }

    public void setPositive(String positive) {
        this.positive = positive;
    }

    public List<OWLClassExpression> getNegatives() {
        return negatives;
    }

    public void setNegatives(List<OWLClassExpression> negatives) {
        this.negatives = negatives;
    }

    @Override
    public String toString(){
        return positive;
    }
}
