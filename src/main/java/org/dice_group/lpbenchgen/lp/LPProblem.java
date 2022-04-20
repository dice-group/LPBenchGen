package org.dice_group.lpbenchgen.lp;

import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

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
    public Collection<String> negatives = new HashSet<String>();
    /**
     * The Positives.
     */
    public Collection<String> positives = new HashSet<String>();
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
    public Collection<OWLDataProperty> dataRules = new HashSet<OWLDataProperty>();
    /**
     * The Negative map containing the examples 2 classExpression map.
     */
    public Map<String, OWLClassExpression> negativeMap = new HashMap<String, OWLClassExpression>();
    /**
     * Are negative examples derived from generated Class Expressions
     */
    public boolean negativeGenerated=false;

    /**
     * Returns the gold Standard concept as OWLClassExpression.
     *
     * @return the owl class expression
     */
    public OWLClassExpression goldStandardConceptAsExpr() {
        return goldStandardConceptExpr;
    }

    /**
     * Returns the gold standard concept as Manchester Syntax string in negation normal form.
     * @return Manchester Syntax string representation in nnf
     */
    public String manchesterSyntaxNNFString() {
        ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
        renderer.setShortFormProvider(new DefaultPrefixManager());
        String manchester = renderer.render(goldStandardConceptExpr.getNNF()).replace("\n", "");
        while (manchester.contains("  ")){
            manchester = manchester.replace("  ", " ");
        }
        return manchester;
    }

    /**
     * Returns the length of then manchester concept, i.e. the number of keywords and classes used.
     * @return concept length
     */
    public long NNFLength() {
        return manchesterSyntaxNNFString().chars().filter(ch -> ch == ' ').count() + 1;
    }

    /**
     * Gets the class expression which retrieved the negative Individual.
     *
     * @param nes the nes
     * @return the expr
     */
    public OWLClassExpression getExpr(String nes) {
        return negativeMap.get(nes);
    }

    /**
     * Creates a copy of this Problem
     * @return deep copy of this problem
     */
    public LPProblem getCopy() {
        LPProblem prob = new LPProblem();
        prob.negatives=new HashSet<>(negatives);
        prob.positives=new HashSet<>(positives);
        prob.goldStandardConcept=goldStandardConcept;
        prob.goldStandardConceptExpr=goldStandardConceptExpr.getNNF();
        prob.negativeGenerated=negativeGenerated;
        prob.rules=new HashSet<>(rules);
        prob.dataRules=new HashSet<>(dataRules);
        prob.negativeMap = new HashMap<>(negativeMap);
        return prob;
    }
}
