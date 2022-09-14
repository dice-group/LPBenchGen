package org.dice_group.lpbenchgen.dl;

import org.jetbrains.annotations.NotNull;
import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxObjectRenderer;
import org.semanticweb.owlapi.model.*;

/**
 * <pre>
 * Calculates the Concept Length of an OWL Class Expression
 *
 * Only supports Class, Intersection, Union, ComplementOf, someValuesFrom and allValuesFrom!
 * </pre>
 *
 * @author Lixi Alié Conrads
 */
public class ConceptLengthCalculator extends DLSyntaxObjectRenderer {

    /**
     * The Concept length.
     */
    private int conceptLength = 0;

    /**
     * Get the length of the provided concept. See {@link ConceptLengthCalculator} for limitations.
     *
     * @param concept any class expression
     * @return length of concept
     */
    static public int get(OWLClassExpression concept) {
        ConceptLengthCalculator renderer = new ConceptLengthCalculator();
        renderer.render(concept);
        return renderer.conceptLength;
    }

    public void visit(@NotNull OWLClass ce) {
        conceptLength++;
        super.visit(ce);
    }

    public void visit(@NotNull OWLObjectIntersectionOf ce) {
        conceptLength++;
        super.visit(ce);
    }

    public void visit(@NotNull OWLObjectUnionOf ce) {
        conceptLength++;
        super.visit(ce);

    }

    public void visit(@NotNull OWLObjectComplementOf ce) {
        conceptLength++;
        super.visit(ce);
    }

    public void visit(@NotNull OWLObjectSomeValuesFrom ce) {
        conceptLength += 2;
        super.visit(ce);
    }

    public void visit(@NotNull OWLObjectAllValuesFrom ce) {
        conceptLength += 2;
        super.visit(ce);
    }

}
