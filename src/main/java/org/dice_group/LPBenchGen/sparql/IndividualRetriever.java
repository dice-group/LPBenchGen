package org.dice_group.LPBenchGen.sparql;

import org.aksw.owl2sparql.OWLClassExpressionToSPARQLConverter;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.dice_group.LPBenchGen.dl.OWL2SPARQL;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLLiteral;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
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

    private List<OWLLiteral> createDataRequest(String sparqlQuery){
        List<OWLLiteral> ret =  new ArrayList<OWLLiteral>();

        Query q = QueryFactory.create(sparqlQuery);
        QueryExecution qexec = QueryExecutionFactory.sparqlService(endpoint, q);
        ResultSet res = qexec.execSelect();
        while(res.hasNext()){
            ret.add(transformLiteral(res.next().getLiteral(DEFAULT_VARIABLE_NAME)));
        }
        return ret;
    }

    private OWLLiteral transformLiteral(Literal literal) {
        return new OWLDataFactoryImpl().getOWLLiteral(literal.getValue().toString(), literal.getDatatypeURI());

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
        OWL2SPARQL converter = new OWL2SPARQL();

        return converter.asQuery(concept, "?var").serialize();
    }

    public Collection<String> retrieveTypesForIndividual(String uri) {
        return createRequest(createTypesQuery(uri));
    }

    public Collection<String[]> retrieveIndividualsForRule(String uri, Collection<String> rules) {
        Collection<String[]> ret = new HashSet<String[]>();
        for(String rule: rules){
            for(String r : createRequest(createRuleQuery(uri, rule))){
                ret.add(new String[]{rule, r});
            }
        }
        return ret;
    }

    public Collection<Object[]> retrieveIndividualsForDataRule(String uri, Collection<OWLDataProperty> rules) {
        Collection<Object[]> ret = new ArrayList<Object[]>();
        for(OWLDataProperty rule: rules){
            for(OWLLiteral r : createDataRequest(createRuleQuery(uri, rule.getIRI().toString()))){
                ret.add(new Object[]{rule, r});
            }
        }
        return ret;
    }


    private String createRuleQuery(String uri, String rule) {
        return "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?"+DEFAULT_VARIABLE_NAME+" { <"+uri+"> <"+rule+"> ?"+DEFAULT_VARIABLE_NAME+"}";
    }
}
