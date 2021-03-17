package org.dice_group.OWLBenchGen.sparql;

import org.aksw.owl2sparql.OWLClassExpressionToSPARQLConverter;
import org.apache.jena.query.*;
import org.semanticweb.owlapi.model.OWLClassExpression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class IndividualRetriever {

    public static final String DEFAULT_VARIABLE_NAME = "var";
    private String endpoint;

    public IndividualRetriever(String endpoint){
        this.endpoint=endpoint;
    }

    public List<String> retrieveIndividualsForType(String uriType){
        String sparqlQuery = createTypeQuery(uriType);

        return createRequest(sparqlQuery);
    }

    private List<String> createRequest(String sparqlQuery){
        List<String> ret =  new ArrayList<String>();

        Query q = QueryFactory.create(sparqlQuery);
        QueryExecution qexec = QueryExecutionFactory.sparqlService(endpoint, q);
        ResultSet res = qexec.execSelect();
        while(res.hasNext()){
            ret.add(res.next().getResource(DEFAULT_VARIABLE_NAME).toString());
        }
        return ret;
    }

    private String createTypeQuery(String uriType) {
        return "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?"+DEFAULT_VARIABLE_NAME+" { ?"+DEFAULT_VARIABLE_NAME+" rdf:type <"+uriType+">} LIMIT 2000";
    }

    private String createTypesQuery(String uriIndividual) {
        return "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?"+DEFAULT_VARIABLE_NAME+" { <"+uriIndividual+"> rdf:type ?"+DEFAULT_VARIABLE_NAME+"}";
    }

    public List<String> retrieveIndividualsForConcept(OWLClassExpression concept){
        String sparqlQuery = createQuery(concept);
        return createRequest(sparqlQuery);
    }

    private String createQuery(OWLClassExpression concept) {
        // Atomic C -> ?s type C
        // rC -> ?s r ?o . ?o type C
        // rD -> ?s r ?o . D
        // C or D  {C} UNION {D} // a little bit more problematic
        // C and D -> C . D .
        // not C -> ?? thats also possible, FILTER (?s )
        // female and president -> {?s type female; type president}
        // female or president -> {?s type female} union {?s type president}
        // female and (not (isR President or artist)) -> {?s type female . FILTER (?s not in {{?s isR ?o . ?o type president} UNION { ?s type artist }})
        // female and (President or artist)) -> {?s type female . {{?s type president} UNION { ?s type artist }}} ???
        //                                      {?s type female . FILTER ( ?s in {?s type artist} OR ?s in {?s type president})}
        OWLClassExpressionToSPARQLConverter converter = new OWLClassExpressionToSPARQLConverter();
        return converter.asQuery(concept, "?var").serialize();
    }

    public Collection<String> retrieveTypesForIndividual(String uri) {
        return createRequest(createTypesQuery(uri));
    }
}
