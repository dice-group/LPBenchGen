package org.dice_group.LPBenchGen.dl;

import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectSomeValuesFromImpl;

/**
 * The type Owl axiom visitor base.
 *
 * @author Lixi Alié Conrads
 */
public class OWLAxiomVisitorBase implements OWLAxiomVisitor {

    private OWLClassExpression expr;


    public void visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
        this.doDefault(axiom);
    }

    public void visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
        this.doDefault(axiom);
    }

    public void visit(OWLObjectPropertyRangeAxiom axiom) {
        expr = new OWLObjectSomeValuesFromImpl(axiom.getProperty(), axiom.getRange());
    }

    public void visit(OWLObjectPropertyDomainAxiom axiom) {
        //expr = new OWLObjectSomeValuesFromImpl(axiom.getProperty(), axiom.getDomain());
    }

    public void visit(OWLObjectPropertyAssertionAxiom axiom) {
        this.doDefault(axiom);
    }

    public void visit(OWLDataPropertyRangeAxiom axiom) {
        //expr=axiom.getRange().getNestedClassExpressions().iterator().next();
    }

    public void visit(OWLClassAssertionAxiom axiom) {
        expr =  axiom.getClassExpression();
    }

    public void visit(OWLDataPropertyAssertionAxiom axiom) {
        this.doDefault(axiom);
    }

    /**
     * Gets expression.
     *
     * @return the expression
     */
    public OWLClassExpression getExpression() {
        return this.expr;
    }
}
