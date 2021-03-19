package org.dice_group.LPBenchGen.dl.visitors;

import org.semanticweb.owlapi.model.*;

import java.util.Random;

public class Pattern2ExprVisitor implements OWLClassExpressionVisitor {

    private final OWLOntology ontology;
    private String expr;
    private Random rand = new Random();
    //TODO walk the string if CLASS is found getClass. if RULE is Found <- check last class get assoc rule
    // if RULE some/all CLASS is found -> get CLASS
    public Pattern2ExprVisitor(OWLOntology ontology) {
        this.ontology=ontology;
    }

    public void visit(OWLObjectIntersectionOf ce) {
        this.doDefault(ce);
    }

    public void visit(OWLObjectUnionOf ce) {
        this.doDefault(ce);
    }

    public void visit(OWLObjectComplementOf ce) {
        this.doDefault(ce);
    }

    public void visit(OWLObjectSomeValuesFrom ce) {
        this.doDefault(ce);
    }

    public void visit(OWLObjectAllValuesFrom ce) {
        this.doDefault(ce);
    }

    public void visit(OWLObjectHasValue ce) {
        this.doDefault(ce);
    }

    public void visit(OWLObjectMinCardinality ce) {
        this.doDefault(ce);
    }

    public void visit(OWLObjectExactCardinality ce) {
        this.doDefault(ce);
    }

    public void visit(OWLObjectMaxCardinality ce) {
        this.doDefault(ce);
    }

    public void visit(OWLObjectHasSelf ce) {
        this.doDefault(ce);
    }

    public void visit(OWLObjectOneOf ce) {
        this.doDefault(ce);
    }

    public void visit(OWLDataSomeValuesFrom ce) {
        this.doDefault(ce);
    }

    public void visit(OWLDataAllValuesFrom ce) {
        this.doDefault(ce);
    }

    public void visit(OWLDataHasValue ce) {
        this.doDefault(ce);
    }

    public void visit(OWLDataMinCardinality ce) {
        this.doDefault(ce);
    }

    public void visit(OWLDataExactCardinality ce) {
        this.doDefault(ce);
    }

    public void visit(OWLDataMaxCardinality ce) {
        this.doDefault(ce);
    }

    public String getExprAsString() {
        return expr;
    }
}
