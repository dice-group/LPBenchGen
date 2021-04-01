package org.dice_group.lpbenchgen.dl.visitors;

import org.semanticweb.owlapi.model.*;

import java.util.Random;

/**
 * The type Pattern 2 expr visitor.
 *
 * @author Lixi Alié Conrads
 */
public class Pattern2ExprVisitor implements OWLClassExpressionVisitor {

    private final OWLOntology ontology;
    private String expr;
    private Random rand = new Random();

    /**
     * Instantiates a new Pattern 2 expr visitor.
     *
     * @param ontology the ontology
     */
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

    /**
     * Gets expr as string.
     *
     * @return the expr as string
     */
    public String getExprAsString() {
        return expr;
    }
}
