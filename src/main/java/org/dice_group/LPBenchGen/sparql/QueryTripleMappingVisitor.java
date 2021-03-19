package org.dice_group.LPBenchGen.sparql;

import com.google.common.collect.Lists;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QueryVisitor;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.syntax.*;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryTripleMappingVisitor implements ElementVisitor {
    private Map<String, List<String[]>> map = new HashMap<String, List<String[]>>();
    private Map<String, List<String[]>> pattern = new HashMap<String, List<String[]>>();
    private String start;
    // walk the query and map Triple to rules e.g.
    // START prop1 ?s0 ; prop2 ?s1 . ?s1 prop3 ?s3!
    // -> START prop1 S0#1, START prop2 S1#1, S1#1 prop3 s3, S0#1 a TYPES, S1#1 a TYPES
    // MAP<String, List<String[]>> -> S1#1 -> [prop3, s3], [...]

    public QueryTripleMappingVisitor(String start){
        this.start=start;
    }

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
                List<String[]> triples = new ArrayList<String[]>();
                //for every pattern which has node as subject
                for(String[] p2 : pat){
                    String[] add;
                    if(p2[1].equals(start)){
                        add = new String[]{p2[0], start};
                    }else {
                        add = new String[]{p2[0], row.get(p2[1]).toString()};
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
            String[] tr = new String[]{triple.getPredicate().toString(), triple.getObject().toString()};
            if(!triple.getPredicate().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
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

    public Map<String, List<String[]>> getMap() {
        return map;
    }
}
