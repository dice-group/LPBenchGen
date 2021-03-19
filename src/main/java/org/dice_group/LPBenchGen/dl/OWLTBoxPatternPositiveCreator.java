package org.dice_group.LPBenchGen.dl;

import org.dice_group.LPBenchGen.dl.visitors.Pattern2ExprVisitor;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;

public class OWLTBoxPatternPositiveCreator implements  OWLTBoxConceptCreator{

    private OWLOntology ontology;
    private OWLClassExpression[] patterns = new OWLClassExpression[]{};
    private int maxFails=10;
    private int maxFailsPerPattern=10;


    @Override
    public Collection<String> createDistinctConcepts(int noOfConcepts) {
        Collection<String> ret = new HashSet<String>();
        int currentPattern=0;
        int fails=0;
        int failsPerPattern=0;
        do{
            if(maxFailsPerPattern<=failsPerPattern){
                fails++;
                failsPerPattern=0;
            }
            else{
                String expr = convertPattern(patterns[currentPattern]);
                if(hasSolution(expr)){
                    ret.add(expr);
                }else{
                    failsPerPattern++;
                    continue;
                }
            }
            currentPattern++;
            if(currentPattern>=patterns.length){
                currentPattern=0;
            }
        }while(ret.size()<noOfConcepts || fails>=maxFails);
        return null;
    }

    public String convertPattern(OWLClassExpression pattern){
        Pattern2ExprVisitor visitor = new Pattern2ExprVisitor(ontology);
        pattern.accept(visitor);
        return visitor.getExprAsString();
    }

    public boolean hasSolution(String concept){
        return true;
    }
}
