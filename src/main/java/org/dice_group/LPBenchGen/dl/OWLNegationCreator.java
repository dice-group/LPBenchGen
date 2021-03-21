package org.dice_group.LPBenchGen.dl;


import com.google.common.collect.Lists;
import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collection;
import java.util.List;


public class OWLNegationCreator implements OWLClassExpressionVisitor, OWLEntityVisitor {

/*TODO:
   SPARQL doesn;t infere super classes
   -> some concepts not working correctly! check for allowed SUB CLASSES as well
   see Settlement  and (leaderName some Person) -> Moscow f.e. its obv correct, however doesn't have Settlement directly.


   Vt in Types -> getAllowedSubTypes ST_t, replace conceptStr(t, (\'(t or ORs in ST_t))\')
  */
    public List<OWLClassExpression> negationConcepts = new ArrayList<OWLClassExpression>();

    boolean currentNegation=false;

    public void addNeccTypes(Collection<String> types, OWLClassExpression typesExpr){
        List<OWLClassExpression> finalConcepts = new ArrayList<OWLClassExpression>();

        for(OWLClassExpression expr : negationConcepts){

            finalConcepts.add(new OWLObjectIntersectionOfImpl(Lists.newArrayList(expr, typesExpr)));
        }
        negationConcepts=finalConcepts;
    }

    private OWLClassExpression getTypeExpr(Collection<String> types) {
        List<OWLClassExpression> classes = new ArrayList<OWLClassExpression>();
        for(String type: types){
            classes.add(new OWLDataFactoryImpl().getOWLClass(IRI.create(type)));
        }
        return new OWLObjectUnionOfImpl(classes);
    }

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
        //some -> all
        if(!currentNegation) {
            //negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());
        }else{
            //TODO???
        }
    }

    public void visit(OWLObjectAllValuesFrom ce) {
        if(!currentNegation) {
            //negationConcepts.add(new OWLObjectComplementOfImpl(ce).getNNF());
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
