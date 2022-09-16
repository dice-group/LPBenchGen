package org.dice_group.lpbenchgen.lp;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * The Learning Problem Class
 *
 * @author Lixi Alié Conrads
 */
public class LPProblem {
    /**
     * The Negatives.
     */
    public Collection<OWLNamedIndividual> negatives = new HashSet<>();
    /**
     * The Positives.
     */
    public Collection<OWLNamedIndividual> positives = new HashSet<>();
    /**
     * The Gold standard concept.
     */
    public String goldStandardConcept = "";
    /**
     * The Gold standard concept expr.
     */
    public OWLClassExpression goldStandardConceptExpr;

    /**
     * The Rules.
     */
    public Collection<String> rules;
    /**
     * The Data rules.
     */
    public Collection<OWLDataProperty> dataRules = new HashSet<>();
    /**
     * The Negative map containing the examples 2 classExpression map.
     */
    public Map<OWLNamedIndividual, OWLClassExpression> negativeMap = new HashMap<>();
    /**
     * Are negative examples derived from generated Class Expressions
     */
    public boolean negativeGenerated = false;

    /**
     * Returns the gold Standard concept as OWLClassExpression.
     *
     * @return the owl class expression
     */
    public OWLClassExpression goldStandardConceptAsExpr() {
        return goldStandardConceptExpr;
    }

    /**
     * Gets the class expression which retrieved the negative Individual.
     *
     * @param nes the nes
     * @return the expr
     */
    public OWLClassExpression getExpr(OWLNamedIndividual nes) {
        return negativeMap.get(nes);
    }

    /**
     * Creates a copy of this Problem
     *
     * @return deep copy of this problem
     */
    public LPProblem getCopy() {
        LPProblem prob = new LPProblem();
        prob.negatives = new HashSet<>(negatives);
        prob.positives = new HashSet<>(positives);
        prob.goldStandardConcept = goldStandardConcept;
        prob.goldStandardConceptExpr = goldStandardConceptExpr.getNNF();
        prob.negativeGenerated = negativeGenerated;
        prob.rules = new HashSet<>(rules);
        prob.dataRules = new HashSet<>(dataRules);
        prob.negativeMap = new HashMap<>(negativeMap);
        return prob;
    }
}
