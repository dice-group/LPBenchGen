package org.dice_group.LPBenchGen.lp;

import org.semanticweb.owlapi.model.OWLDataProperty;

import java.util.Collection;
import java.util.HashSet;

public class LPProblem {
    public Collection<String> negatives = new HashSet<String>();
    public Collection<String> positives = new HashSet<String>();
    public String goldStandardConcept = "";
    public Collection<String> rules;
    public Collection<OWLDataProperty> dataRules = new HashSet<OWLDataProperty>();

}
