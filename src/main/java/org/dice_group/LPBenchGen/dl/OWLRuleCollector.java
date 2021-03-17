package org.dice_group.LPBenchGen.dl;


import org.semanticweb.owlapi.model.*;

import java.util.HashSet;
import java.util.Collection;


public class OWLRuleCollector implements OWLClassExpressionVisitor {

    public Collection<String> rules = new HashSet<String>();

    public void visit(OWLClass ce) {

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
        ce.getObjectPropertiesInSignature().forEach(prop -> {
            rules.add(prop.getIRI().toString());
        });
    }

    public void visit(OWLDataSomeValuesFrom ce) {
        ce.getObjectPropertiesInSignature().forEach(prop -> {
            rules.add(prop.getIRI().toString());
        });
    }

    public void visit(OWLDataAllValuesFrom ce) {
        ce.getObjectPropertiesInSignature().forEach(prop -> {
            rules.add(prop.getIRI().toString());
        });
    }

    public void visit(OWLDataHasValue ce) {
        ce.getObjectPropertiesInSignature().forEach(prop -> {
            rules.add(prop.getIRI().toString());
        });
    }

    public void visit(OWLDataMinCardinality ce) {
        ce.getObjectPropertiesInSignature().forEach(prop -> {
            rules.add(prop.getIRI().toString());
        });

    }

    public void visit(OWLDataExactCardinality ce) {
        ce.getObjectPropertiesInSignature().forEach(prop -> {
            rules.add(prop.getIRI().toString());
        });
    }

    public void visit(OWLDataMaxCardinality ce) {
        ce.getObjectPropertiesInSignature().forEach(prop -> {
            rules.add(prop.getIRI().toString());
        });
    }
}
