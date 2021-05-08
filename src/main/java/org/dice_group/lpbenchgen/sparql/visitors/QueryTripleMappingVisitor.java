package org.dice_group.lpbenchgen.sparql.visitors;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.syntax.*;
import org.dice_group.lpbenchgen.sparql.Triple;

import java.util.*;

/**
 * Creates a Triples from a Query and the corresponding ResultSet
 *
 * @author Lixi Alié Conrads
 */
public class QueryTripleMappingVisitor implements ElementVisitor {
    private Set<Triple> mapping = new HashSet<>();
    private Set<Triple> map = new HashSet<Triple>();
    private String start;
    // walk the query and map Triple to rules e.g.
    // START prop1 ?s0 ; prop2 ?s1 . ?s1 prop3 ?s3!
    // -> START prop1 S0#1, START prop2 S1#1, S1#1 prop3 s3, S0#1 a TYPES, S1#1 a TYPES
    // MAP<String, List<String[]>> -> S1#1 -> [prop3, s3], [...]

    /**
     * Instantiates a new Query triple mapping visitor.
     *
     * @param start the start
     */
    public QueryTripleMappingVisitor(String start){
        this.start=start;
    }

    /**
     * Creates the actual triples from the previously created mapping to actual Triples.
     *
     * @param res the res
     */
    public void patternToMap(ResultSet res){
        while(res.hasNext()){
            QuerySolution row = res.next();
            for(Triple tripleMap :mapping){
                //fill triple
                String subject=tripleMap.subject, predicate=tripleMap.predicate;
                Object object=tripleMap.object;
                if(tripleMap.subject.startsWith("?")){
                    if(row.contains(tripleMap.subject)){
                        subject = row.get(tripleMap.subject).toString();
                    }else{
                        //UNION
                        continue;
                    }
                }
                if(tripleMap.predicate.startsWith("?")){
                    if(row.contains(tripleMap.predicate)){
                        predicate = row.get(tripleMap.predicate).toString();
                    }else{
                        //UNION
                        continue;
                    }
                }
                if(tripleMap.object.toString().startsWith("?")){
                    if(row.contains(tripleMap.object.toString())){
                        object = row.get(tripleMap.object.toString());
                    }else{
                        //UNION
                        continue;
                    }
                }
                map.add(new Triple(subject, predicate, object));
            }
        }
    }

    @Override
    public void visit(ElementTriplesBlock el) {

    }

    @Override
    public void visit(ElementPathBlock el) {
        el.getPattern().forEach(triple ->{
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
            mapping.add(tr);
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
        ElementWalker.walk(el.getQuery().getQueryPattern(), this);
    }

    /**
     * Gets map.
     *
     * @return the map
     */
    public Set<Triple> getMap() {
        return map;
    }
}
