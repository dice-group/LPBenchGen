package org.dice_group.lpbenchgen.dl.creator;


import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.*;

import java.util.ArrayList;
import java.util.List;


/**
 * Creates Class Expression which are complements of an original class Expression
 *
 * @author Lixi Alié Conrads
 */
public class OWLNegationCreator implements OWLClassExpressionVisitor, OWLEntityVisitor {


    /**
     * The complement concepts.
     */
    public List<OWLClassExpression> negationConcepts = new ArrayList<OWLClassExpression>();


    public void visit(OWLClass ce) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());


    }

    public void visit(OWLObjectIntersectionOf ce) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());


        for(OWLClassExpression entity : ce.getOperands()){
            entity.accept(this);
            List<OWLClassExpression> ops = new ArrayList<OWLClassExpression>(ce.getOperands());

                ops.remove(entity);
                ops.add(new OWLObjectComplementOfImpl(entity));
                negationConcepts.add(new OWLObjectIntersectionOfImpl(ops).getNNF());

        }
    }

    public void visit(OWLObjectUnionOf ce) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());
    }

    public void visit(OWLObjectComplementOf ce) {
        negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());

    }

    public void visit(OWLObjectSomeValuesFrom ce) {
        //some -> all
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());

    }

    public void visit(OWLObjectAllValuesFrom ce) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());

    }

    public void visit(OWLObjectHasValue ce) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());

    }

    public void visit(OWLObjectMinCardinality ce) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());

    }

    public void visit(OWLObjectExactCardinality ce) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());

    }

    public void visit(OWLObjectMaxCardinality ce) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());

    }

    public void visit(OWLObjectHasSelf ce) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());

    }

    public void visit(OWLObjectOneOf ce) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());
    }

    public void visit(OWLDataSomeValuesFrom ce) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());
    }

    public void visit(OWLDataAllValuesFrom ce) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());

    }

    public void visit(OWLDataHasValue ce) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());

    }

    public void visit(OWLDataMinCardinality ce) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());

    }

    public void visit(OWLDataExactCardinality ce) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());

    }

    public void visit(OWLDataMaxCardinality ce) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());

    }
}
