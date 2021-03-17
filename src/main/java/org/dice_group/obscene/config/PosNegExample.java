package org.dice_group.obscene.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class PosNegExample {

    @JsonProperty(required = true)
    private String positive;
    @JsonProperty(required = true)
    private List<String> negatives;

    public String getPositive() {
        return positive;
    }

    public void setPositive(String positive) {
        this.positive = positive;
    }

    public List<String> getNegatives() {
        return negatives;
    }

    public void setNegatives(List<String> negatives) {
        this.negatives = negatives;
    }
}
