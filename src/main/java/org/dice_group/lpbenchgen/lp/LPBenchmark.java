package org.dice_group.lpbenchgen.lp;

import org.semanticweb.owlapi.model.OWLOntology;

import java.util.ArrayList;
import java.util.List;

public class LPBenchmark {

    private List<LPProblem> train = new ArrayList<LPProblem>();
    private List<LPProblem> gold = new ArrayList<LPProblem>();
    private List<LPProblem> test = new ArrayList<LPProblem>();
    private OWLOntology abox;

    public List<LPProblem> getTrain() {
        return train;
    }

    public void setTrain(List<LPProblem> train) {
        this.train = train;
    }

    public List<LPProblem> getGold() {
        return gold;
    }

    public void setGold(List<LPProblem> gold) {
        this.gold = gold;
    }

    public List<LPProblem> getTest() {
        return test;
    }

    public void setTest(List<LPProblem> test) {
        this.test = test;
    }

    public OWLOntology getAbox() {
        return abox;
    }

    public void setABox(OWLOntology abox) {
        this.abox = abox;
    }
}
