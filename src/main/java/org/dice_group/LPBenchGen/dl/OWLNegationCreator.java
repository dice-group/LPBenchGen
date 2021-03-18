package org.dice_group.LPBenchGen.dl;


import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectComplementOfImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectIntersectionOfImpl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collection;
import java.util.List;


public class OWLNegationCreator implements OWLClassExpressionVisitor, OWLEntityVisitor {

    //TODO
    public List<OWLClassExpression> negationConcepts = new ArrayList<OWLClassExpression>();

    boolean currentNegation=false;

    public void prune(){
        List<OWLClassExpression> unions = new ArrayList<OWLClassExpression>();
        List<OWLClassExpression> remove = new ArrayList<OWLClassExpression>();

        for(OWLClassExpression expression : negationConcepts){
            if(expression instanceof OWLObjectUnionOf){
                unions.add(expression);
            }
        }
        for(OWLClassExpression union: unions){
            for(OWLClassExpression expr : negationConcepts){
                if(contains((OWLObjectUnionOf) union, expr)){
                    remove.add(expr);
                }
            }
        }
        negationConcepts.removeAll(remove);
        if(negationConcepts.size()> unions.size()){
            negationConcepts.removeAll(unions);
        }
    }

    public boolean contains(OWLObjectUnionOf union, OWLClassExpression expr){
            if(union == expr) {
                return false;
            }
            for(OWLClassExpression unionExpr : ((OWLObjectUnionOf)union).getOperands()) {
                if(unionExpr.toString().equals(expr.toString())){
                    return true;
                }
            }
            return false;
    }


    public void visit(OWLClass ce) {
        if(!currentNegation) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());
        }else{
            //TODO???
        }

    }

    public void visit(OWLObjectIntersectionOf ce) {
        if(!currentNegation) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());
        }else{
            //TODO???
        }

        for(OWLClassExpression entity : ce.getOperands()){
            entity.accept(this);
            List<OWLClassExpression> ops = new ArrayList<OWLClassExpression>(ce.getOperands());
            ops.remove(entity);
            ops.add(new OWLObjectComplementOfImpl(entity));
            negationConcepts.add(new OWLObjectIntersectionOfImpl(ops));
        }
    }

    public void visit(OWLObjectUnionOf ce) {
        if(!currentNegation) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());
        }else{
            //TODO???
        }
        for(OWLEntity entity : ce.getSignature()){
            entity.accept(this);
        }
    }

    public void visit(OWLObjectComplementOf ce) {
        if(!currentNegation) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());
        }else{
            //TODO???
        }
    }

    public void visit(OWLObjectSomeValuesFrom ce) {
        if(!currentNegation) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());
        }else{
            //TODO???
        }
    }

    public void visit(OWLObjectAllValuesFrom ce) {
        if(!currentNegation) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());
        }else{
            //TODO???
        }
    }

    public void visit(OWLObjectHasValue ce) {
        if(!currentNegation) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());
        }else{
            //TODO???
        }
    }

    public void visit(OWLObjectMinCardinality ce) {
        if(!currentNegation) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());
        }else{
            //TODO???
        }
    }

    public void visit(OWLObjectExactCardinality ce) {
        if(!currentNegation) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());
        }else{
            //TODO???
        }
    }

    public void visit(OWLObjectMaxCardinality ce) {
        if(!currentNegation) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());
        }else{
            //TODO???
        }
    }

    public void visit(OWLObjectHasSelf ce) {
        if(!currentNegation) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());
        }else{
            //TODO???
        }
    }

    public void visit(OWLObjectOneOf ce) {
        if(!currentNegation) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());
        }else{
            //TODO???
        }
    }

    public void visit(OWLDataSomeValuesFrom ce) {
        if(!currentNegation) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());
        }else{
            //TODO???
        }
    }

    public void visit(OWLDataAllValuesFrom ce) {
        if(!currentNegation) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());
        }else{
            //TODO???
        }
    }

    public void visit(OWLDataHasValue ce) {
        if(!currentNegation) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());
        }else{
            //TODO???
        }
    }

    public void visit(OWLDataMinCardinality ce) {
        if(!currentNegation) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());
        }else{
            //TODO???
        }
    }

    public void visit(OWLDataExactCardinality ce) {
        if(!currentNegation) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());
        }else{
            //TODO???
        }
    }

    public void visit(OWLDataMaxCardinality ce) {
        if(!currentNegation) {
            negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());
        }else{
            //TODO???
        }
    }
}
