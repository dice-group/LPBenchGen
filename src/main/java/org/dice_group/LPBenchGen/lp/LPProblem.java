package org.dice_group.LPBenchGen.lp;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class LPProblem {
    public Collection<String> negatives = new HashSet<String>();
    public Collection<String> positives = new HashSet<String>();
    public String goldStandardConcept = "";
    public OWLClassExpression goldStandardConceptExpr;

    public Collection<String> rules;
    public Collection<OWLDataProperty> dataRules = new HashSet<OWLDataProperty>();
    public Map<String, OWLClassExpression> negativeMap = new HashMap<String, OWLClassExpression>();
    public boolean negativeGenerated=false;

    public OWLClassExpression goldStandardConceptAsExpr() {
        return goldStandardConceptExpr;
    }

    public OWLClassExpression getExpr(String nes) {
        return negativeMap.get(nes);
    }
}
