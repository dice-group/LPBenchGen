package org.dice_group.lpbenchgen.dl.visitors;

import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxObjectRenderer;
import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.util.HashSet;
import java.util.Set;

/**
 * The type Concept sub classes exchanger.
 *
 * @author Lixi Alié Conrads
 */
public class ConceptSubClassesExchanger extends DLSyntaxObjectRenderer {

    /**
     * The Replace.
     */
    public String replace;
    /**
     * The Replacer.
     */
    public OWLClassExpression replacer;

    private OWLDataFactory factory = new OWLDataFactoryImpl();

    public void visit(OWLClass ce) {
        super.visit(ce);
    }

    public void visit(OWLObjectIntersectionOf ce) {
        Set<OWLClassExpression> rem = new HashSet<OWLClassExpression>();
        Set<OWLClassExpression> add = new HashSet<OWLClassExpression>();

        ce.getOperands().forEach(expr ->{
            checkExpr(expr, rem, add);
        });
        if(!rem.isEmpty()){
            ce.getOperands().removeAll(rem);
            ce.getOperands().add(replacer);
        }
        super.visit(ce);
    }

    public  void visit(OWLObjectUnionOf ce) {
        Set<OWLClassExpression> rem = new HashSet<OWLClassExpression>();
        Set<OWLClassExpression> add = new HashSet<OWLClassExpression>();

        ce.getOperands().forEach(expr ->{
            checkExpr(expr, rem, add);
        });
        if(!rem.isEmpty()){
            ce.getOperands().removeAll(rem);
            ce.getOperands().add(replacer);
        }
        super.visit(ce);

    }

    private boolean checkClass(OWLClassExpression expr){
        return expr instanceof OWLClass && ((OWLClass) expr).getIRI().toString().equals(replace);
    }

    private void checkExpr(OWLClassExpression expr, Set<OWLClassExpression> rem, Set<OWLClassExpression> add){
        if(checkClass(expr)){
            rem.add((OWLClass)expr);
            add.add(replacer);
        }
        if (expr instanceof OWLObjectComplementOf){
            OWLClassExpression expr2 =((OWLObjectComplementOf)expr).getOperand();
            if(checkClass(expr2)){
                rem.add(expr);
                add.add(factory.getOWLObjectComplementOf(replacer));
            }
        }
        else if(expr instanceof OWLObjectSomeValuesFrom){
            OWLClassExpression expr2 =((OWLObjectSomeValuesFrom)expr).getFiller();
            if(checkClass(expr2)){
                rem.add(expr);
                add.add(factory.getOWLObjectSomeValuesFrom(((OWLObjectSomeValuesFrom)expr).getProperty(), replacer));
            }
        }
        else if(expr instanceof OWLObjectAllValuesFrom){
            OWLClassExpression expr2 =((OWLObjectAllValuesFrom)expr).getFiller();
            if(checkClass(expr2)){
                rem.add(expr);
                add.add(factory.getOWLObjectAllValuesFrom(((OWLObjectAllValuesFrom)expr).getProperty(), replacer));
            }
        }
    }

    public void visit(OWLObjectComplementOf ce) {
        super.visit(ce);
    }

    public void visit(OWLObjectSomeValuesFrom ce) {
        super.visit(ce);
    }

    public  void visit(OWLObjectAllValuesFrom ce) {
        super.visit(ce);
    }

}

