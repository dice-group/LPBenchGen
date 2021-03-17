package org.dice_group.obscene.dl;


import org.semanticweb.owlapi.model.*;

public class OWL2SPARQLVisitor implements OWLClassExpressionVisitor {

    private StringBuilder whereClause=new StringBuilder();

    private String var="?s";

    public void visit(OWLClass ce) {

        whereClause.append(var).append(" rdf:type <").append(ce.getIRI().toString()).append(">");
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
}
