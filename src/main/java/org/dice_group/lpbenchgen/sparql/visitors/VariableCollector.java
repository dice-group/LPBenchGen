package org.dice_group.lpbenchgen.sparql.visitors;

import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <pre>
 * Collect Variables occurring in a Query.
 * And removes FILTER clauses if the variable which should be ignored occurs in that clause.
 *
 * Variables in a FILTER clause will be ignored.
 *
 * Usage:
 *
 * Query q =...;
 * VariableCollector varCollector = new VariableCollector():
 * ElementWalker.walk(q.getQueryPattern(), varCollector);
 *
 * varCollector.vars #Get occurring variables.
 *
 * </pre>
 *
 * @author Lixi Alié Conrads
 */
public class VariableCollector implements ElementVisitor  {

    /**
     * The variables which occurred.
     */
    public Set<String> vars = new HashSet<String>();

    /**
     * If a variable should be ignored. (use ?v)
     */
    public String ignore="?var";

    @Override
    public void visit(ElementTriplesBlock elementTriplesBlock) {

    }

    @Override
    public void visit(ElementPathBlock elementPathBlock) {

        elementPathBlock.getPattern().forEach(triple -> {
            if(triple.getSubject().isVariable()){
                if(!triple.getSubject().toString().equals(ignore)){
                    vars.add(triple.getSubject().toString().substring(1));
                }
            }
            if(triple.getObject().isVariable()){
                if(!triple.getObject().toString().equals(ignore)){
                    vars.add(triple.getObject().toString().substring(1));
                }
            }
            if(triple.getPredicate()!=null && triple.getPredicate().isVariable()){
                if(!triple.getPredicate().toString().equals(ignore)){
                    vars.add(triple.getPredicate().toString().substring(1));
                }
            }
        });
    }

    @Override
    public void visit(ElementFilter elementFilter) {

    }

    @Override
    public void visit(ElementAssign elementAssign) {

    }

    @Override
    public void visit(ElementBind elementBind) {

    }


    @Override
    public void visit(ElementData elementData) {

    }

    @Override
    public void visit(ElementUnion elementUnion) {

    }

    @Override
    public void visit(ElementOptional elementOptional) {

    }

    @Override
    public void visit(ElementGroup elementGroup) {
        List<Element> remove = new ArrayList<Element>();
        for(Element element : elementGroup.getElements()){
                    if(element instanceof ElementFilter){
                        Set<Var> mentioned =  ((ElementFilter)element).getExpr().getVarsMentioned();
                        if(mentioned.size()==1 && mentioned.iterator().next().toString().equals(ignore)){
                            remove.add(element);
                        }
                    }
                }

        elementGroup.getElements().removeAll(remove);
    }

    @Override
    public void visit(ElementDataset elementDataset) {

    }

    @Override
    public void visit(ElementNamedGraph elementNamedGraph) {

    }

    @Override
    public void visit(ElementExists elementExists) {

    }

    @Override
    public void visit(ElementNotExists elementNotExists) {

    }

    @Override
    public void visit(ElementMinus elementMinus) {

    }

    @Override
    public void visit(ElementService elementService) {

    }

    @Override
    public void visit(ElementSubQuery el) {
        VariableCollector varC = new VariableCollector();
        varC.ignore=ignore;
        Query sub = el.getQuery();
        ElementWalker.walk(sub.getQueryPattern(), varC);
        sub.getProjectVars().clear();
        sub.getGroupBy().clear();
        varC.vars.forEach(v -> {
            sub.getProjectVars().add(Var.alloc(v));
            sub.getHavingExprs().clear();
        });
        vars.addAll(varC.vars);
    }
}
