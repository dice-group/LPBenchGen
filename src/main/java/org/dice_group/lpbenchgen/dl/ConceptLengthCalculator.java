package org.dice_group.lpbenchgen.dl;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.AbstractCollector;

/**
 * <pre>
 * Calculates the Concept Length of an OWL Class Expression
 *
 * Only supports Class, Intersection, Union, ComplementOf, someValuesFrom and allValuesFrom!
 * </pre>
 *
 * @author Lixi Alié Conrads
 * @author Alexander Bigerl
 */
public class ConceptLengthCalculator {
    /**
     * Get the length of the provided concept. See {@link ConceptLengthCalculator} for limitations.
     *
     * @param concept any class expression
     * @return length of concept
     */
    static public int get(OWLClassExpression concept) {
        ConceptLengthCalculatorImpl lengthCalculator = new ConceptLengthCalculatorImpl();
        concept.accept(lengthCalculator);
        return lengthCalculator.conceptLength;
    }

    static private class ConceptLengthCalculatorImpl extends AbstractCollector {
        /**
         * The Concept length.
         */
        private int conceptLength = 0;

        public void visit(OWLClass ce) {
            conceptLength++;
            super.visit(ce);
        }

        public void visit(OWLObjectIntersectionOf ce) {
            conceptLength++;
            super.visit(ce);
        }

        public void visit(OWLObjectUnionOf ce) {
            conceptLength++;
            super.visit(ce);

        }

        public void visit(OWLObjectComplementOf ce) {
            conceptLength++;
            super.visit(ce);
        }

        public void visit(OWLObjectSomeValuesFrom ce) {
            conceptLength += 2;
            super.visit(ce);
        }

        public void visit(OWLObjectAllValuesFrom ce) {
            conceptLength += 2;
            super.visit(ce);
        }
    }
}
