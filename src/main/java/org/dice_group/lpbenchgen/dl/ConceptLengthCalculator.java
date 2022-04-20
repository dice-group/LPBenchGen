package org.dice_group.lpbenchgen.dl;

import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxObjectRenderer;
import org.semanticweb.owlapi.model.*;

/**
 * <pre>
 * Calculates the Concept Length of an OWL Class Expression
 *
 * Only supports Class, Intersection, Union, ComplementOf, someValuesFrom and allValuesFrom!
 * </pre>
 * @author Lixi Alié Conrads
 */
public class ConceptLengthCalculator extends DLSyntaxObjectRenderer {

    /**
     * The Concept length.
     */
    private int conceptLength=0;

    public static int calc(OWLClassExpression ce) {
        ConceptLengthCalculator clc = new ConceptLengthCalculator();
        clc.render(ce);
        return clc.conceptLength;
    }

    public static int calcNNF(OWLClassExpression ce) {
        return calc(ce.getNNF());
    }

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
        super.visit(ce);

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
