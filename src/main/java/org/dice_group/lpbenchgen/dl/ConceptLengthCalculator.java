package org.dice_group.lpbenchgen.dl;

import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxObjectRenderer;
import org.semanticweb.owlapi.model.*;

/**
 * Calculates the Concept Length of an OWL Class Expression
 *
 * @author Lixi Alié Conrads
 */
public class ConceptLengthCalculator extends DLSyntaxObjectRenderer {

    /**
     * The Concept length.
     */
    public int conceptLength=0;

    public void visit(OWLClass ce) {
        conceptLength++;
        super.visit(ce);
    }

    public void visit(OWLObjectIntersectionOf ce) {
        conceptLength++;
        super.visit(ce);
    }

    public  void visit(OWLObjectUnionOf ce) {
        conceptLength++;
        boolean types=true;
        for(OWLClassExpression expr : ce.getOperands()){
            if(!(expr instanceof OWLClass)){
                types=false;
            }
        }
        if(!types) {
            super.visit(ce);
        }
    }

    public void visit(OWLObjectComplementOf ce) {
        conceptLength++;
        super.visit(ce);
    }

    public void visit(OWLObjectSomeValuesFrom ce) {
        conceptLength+=2;
        super.visit(ce);
    }

    public  void visit(OWLObjectAllValuesFrom ce) {
        conceptLength+=2;
        super.visit(ce);
    }

}
