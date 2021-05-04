package org.dice_group.lpbenchgen.sparql;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.syntax.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The type Query triple mapping visitor.
 *
 * @author Lixi Alié Conrads
 */
public class QueryTripleMappingVisitor implements ElementVisitor {
    private Map<String, List<Object[]>> map = new HashMap<String, List<Object[]>>();
    private Map<String, List<String[]>> pattern = new HashMap<String, List<String[]>>();
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
     * Pattern to map.
     *
     * @param res the res
     */
    public void patternToMap(ResultSet res){
        while(res.hasNext()){
            QuerySolution row = res.next();
            pattern.keySet().forEach(key -> {
                String node;
                if(key.equals(start)) {
                    node=start;
                }else{
                    node = row.get(key).toString();
                }
                List<String[]> pat = pattern.get(key);
                List<Object[]> triples = new ArrayList<Object[]>();
                //for every pattern which has node as subject
                for(String[] p2 : pat){
                    Object[] add;
                    String property = p2[0];
                    if(property.startsWith("?")){
                        property=row.get(p2[0]).toString();
                    }
                    if(p2[1].equals(start)){
                        add = new Object[]{property, start};
                    }else {
                        add = new Object[]{property, row.get(p2[1])};
                    }
                    triples.add(add);
                }
                if(map.containsKey(node.toString())){
                    map.get(node.toString()).addAll(triples);
                }
                else {
                    map.put(node.toString(), triples);
                }
            });
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
                predicate=triple.getPath().toString();
            }
            String[] tr = new String[]{predicate, triple.getObject().toString()};
            if(!(predicate.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") || predicate.equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>/(<http://www.w3.org/2000/01/rdf-schema#subClassOf>)*"))) {
                if (pattern.containsKey(triple.getSubject().toString())) {
                    pattern.get(triple.getSubject().toString()).add(tr);
                } else {
                    List<String[]> coll = new ArrayList<String[]>();
                    coll.add(tr);
                    pattern.put(triple.getSubject().toString(), coll);
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
    public void visit(ElementFind elementFind) {

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
    public Map<String, List<Object[]>> getMap() {
        return map;
    }
}