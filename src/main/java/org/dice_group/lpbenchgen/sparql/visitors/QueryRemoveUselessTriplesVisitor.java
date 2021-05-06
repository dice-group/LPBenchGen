package org.dice_group.lpbenchgen.sparql.visitors;

import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.*;
import org.dice_group.lpbenchgen.sparql.Triple;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Removes Useless Triples (e.g. triples which have no variable) from a query and stores them.
 *
 * @author Lixi Alié Conrads
 */
public class QueryRemoveUselessTriplesVisitor implements ElementVisitor {
    /**
     * The Found empty bg ps.
     */
    boolean foundEmptyBGPs=false;

    /**
     * The removed triples
     */
    public Set<Triple> triples = new HashSet<>();

    @Override
    public void visit(ElementTriplesBlock el) {
        if(foundEmptyBGPs) {
            System.out.println("test");
        }
    }

    @Override
    public void visit(ElementPathBlock el) {
        List<TriplePath> rem = new ArrayList<TriplePath>();
        for(TriplePath triple : el.getPattern().getList()){
            if(!(triple.getSubject().isVariable() || (triple.getPredicate()!=null&&triple.getPredicate().isVariable()) || triple.getObject().isVariable())){
                rem.add(triple);
                addTriple(triple);
            }
        }
        el.getPattern().getList().removeAll(rem);
        if(el.getPattern().getList().isEmpty()){
            foundEmptyBGPs=true;
        }
    }

    private void addTriple(TriplePath triple) {

        String subject = triple.getSubject().toString();
        String predicate;
        if(triple.getPredicate()!=null){
            predicate = triple.getPredicate().toString();
        }else{
            predicate="http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
        }
        Object object = triple.getObject().toString();
        if(triple.getObject().isURI()){
            object = ResourceFactory.createResource(triple.getObject().getURI());
        }
        Triple tr = new Triple(triple.getSubject().toString(), predicate, object);
        triples.add(tr);
    }

    @Override
    public void visit(ElementFilter el) {

    }

    @Override
    public void visit(ElementAssign el) {

    }

    @Override
    public void visit(ElementBind el) {

    }


    @Override
    public void visit(ElementData el) {

    }

    @Override
    public void visit(ElementUnion el) {
        if(foundEmptyBGPs) {
            List<Element> remove = new ArrayList<Element>();
            for(Element el2 : el.getElements()){
                if(el2 instanceof  ElementPathBlock){
                    if(((ElementPathBlock)el2).getPattern().getList().isEmpty()){
                        remove.add(el2);
                    }
                }
                if(el2 instanceof  ElementGroup){
                    if(((ElementGroup)el2).getElements().isEmpty()){
                        remove.add(el2);
                    }
                }
                if(el2 instanceof  ElementUnion){
                    if(((ElementUnion)el2).getElements().isEmpty()){
                        remove.add(el2);
                    }
                }
                if(el2 instanceof ElementOptional){
                    if(checkOptional(((ElementOptional) el2).getOptionalElement(), remove)){
                        remove.add(el2);
                    }
                }
            }

            el.getElements().removeAll(remove);
        }
    }

    @Override
    public void visit(ElementOptional el) {

    }

    @Override
    public void visit(ElementGroup el) {

        if(foundEmptyBGPs) {
            List<Element> remove = new ArrayList<Element>();
            for(Element el2 : el.getElements()){
                if(el2 instanceof  ElementPathBlock){
                    if(((ElementPathBlock)el2).getPattern().getList().isEmpty()){
                        remove.add(el2);
                    }
                }
                if(el2 instanceof  ElementGroup){
                    if(((ElementGroup)el2).getElements().isEmpty()){
                        remove.add(el2);
                    }
                }
                if(el2 instanceof  ElementUnion){
                    if(((ElementUnion)el2).getElements().isEmpty()){
                        remove.add(el2);
                    }
                }
                if(el2 instanceof ElementOptional){
                    if(checkOptional(((ElementOptional) el2).getOptionalElement(), remove)){
                        remove.add(el2);
                    }
                }
            }

            el.getElements().removeAll(remove);
        }
    }

    private boolean checkOptional(Element el2, List<Element> remove){
        if(el2 instanceof  ElementPathBlock){
            if(((ElementPathBlock)el2).getPattern().getList().isEmpty()){
                remove.add(el2);
            }
            return true;
        }
        if(el2 instanceof  ElementGroup){
            if(((ElementGroup)el2).getElements().isEmpty()){
                remove.add(el2);
            }
            return true;
        }
        if(el2 instanceof  ElementUnion){
            if(((ElementUnion)el2).getElements().isEmpty()){
                remove.add(el2);
            }
            return true;
        }
        if(el2 instanceof ElementOptional){
            return checkOptional(((ElementOptional) el2).getOptionalElement(), remove);
        }
        return false;
    }

    @Override
    public void visit(ElementDataset el) {

    }

    @Override
    public void visit(ElementNamedGraph el) {

    }

    @Override
    public void visit(ElementExists el) {

    }

    @Override
    public void visit(ElementNotExists el) {

    }

    @Override
    public void visit(ElementMinus el) {

    }

    @Override
    public void visit(ElementService el) {

    }

    @Override
    public void visit(ElementSubQuery el) {
        QueryRemoveUselessTriplesVisitor vis = new QueryRemoveUselessTriplesVisitor();
        ElementWalker.walk(el.getQuery().getQueryPattern(), vis);
    }
}
