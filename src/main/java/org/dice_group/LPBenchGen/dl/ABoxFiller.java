package org.dice_group.LPBenchGen.dl;

import com.google.common.collect.Lists;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.algebra.optimize.VariableUsageVisitor;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.ElementWalker;
import org.dice_group.LPBenchGen.sparql.IndividualRetriever;
import org.dice_group.LPBenchGen.sparql.QueryTripleMappingVisitor;
import org.dice_group.LPBenchGen.sparql.VariableCollector;
import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ABoxFiller {

    private OWLDataFactory factory = new OWLDataFactoryImpl();
    private List<String> allowedTypes;
    private IndividualRetriever retriever;

    public static void main(String[] args) throws OWLOntologyCreationException {
        IndividualRetriever retriever = new IndividualRetriever("http://dbpedia.org/sparql");
        ABoxFiller filler = new ABoxFiller(retriever, Lists.newArrayList("http://dbpedia.org/ontology/Person", "http://dbpedia.org/ontology/City", "http://dbpedia.org/ontology/Place"));
        Parser parser = new Parser("./dbpedia_2016-10.owl");
        OWLClassExpression ce = parser.parseManchesterConcept("Person and (birthPlace some (City and location some Place))");

        String startIndvidual  = retriever.retrieveIndividualsForConcept(ce).iterator().next();
        filler.addIndividualsFromConcept(ce, startIndvidual, parser.getOntology());
    }

    public ABoxFiller(IndividualRetriever retriever, List<String> allowedTypes){
        this.retriever=retriever;
        this.allowedTypes=allowedTypes;
    }

    public void addIndividualsFromConcept(OWLClassExpression concept, String startIndividual, OWLOntology ontology){
        //walk concept from startIndividual -> For all rules recursively add Individuals who have the connection
        // e.g. MusicalArtist some prop1 (Politician and some prop2 XYZ)
        // -> startIndividual prop1 ?o1 . ?o1 rdf:type Politician ; prop2 ?o2
        // -> add ?o1 and ?o2 and add o1 type Policitian; prop2 o2
        //    add MusicalArtist1
        // add/or (should be straigth forward as well)
        // get SPARQL query from OWL2SPARQL -> but instead fill startIndividual with VAR and get all other variables
        //TODO subqueries check

        OWL2SPARQL sparql = new OWL2SPARQL();
        Query q = sparql.asQuery(concept, "?var");
        VariableCollector varC= new VariableCollector();
        ElementWalker.walk(q.getQueryPattern(), varC);
        q.getProjectVars().clear();
        q.getGroupBy().clear();
        varC.vars.forEach(v -> {
            q.getProjectVars().add(Var.alloc(v));
        });


        String queryStr = q.serialize().replace("?var", "<"+startIndividual+">");
        try {
            if(varC.vars.isEmpty()){
                //only a type query
                OWLNamedIndividual subject = factory.getOWLNamedIndividual(startIndividual);
                ontology.addAxioms(getTypeAxiomsForIndividual(subject));
            }else {
                Query query = QueryFactory.create(queryStr);

                query.setLimit(100);
                ResultSet res = retriever.getResultMap(query);
                QueryTripleMappingVisitor visitor = new QueryTripleMappingVisitor(startIndividual);
                ElementWalker.walk(query.getQueryPattern(), visitor);
                visitor.patternToMap(res);
                List<OWLAxiom> axioms = createAxiomsFromMap(visitor.getMap());
                ontology.addAxioms(axioms);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private List<OWLAxiom> createAxiomsFromMap(Map<String, List<String[]>> map) {
        List<OWLAxiom> axioms = new ArrayList<OWLAxiom>();
        map.keySet().forEach(individual ->{
            OWLNamedIndividual subject = factory.getOWLNamedIndividual(individual);
            axioms.addAll(getTypeAxiomsForIndividual(subject));
            map.get(individual).forEach(triple -> {
                OWLObjectPropertyExpression property = factory.getOWLObjectProperty(triple[0]);
                OWLNamedIndividual object = factory.getOWLNamedIndividual(triple[1]);
                OWLAxiom axiom = factory.getOWLObjectPropertyAssertionAxiom(property, subject, object);
                axioms.add(axiom);
            });
        });
        return axioms;
    }

    private Collection<OWLAxiom> getTypeAxiomsForIndividual(OWLNamedIndividual subject) {
        List<OWLAxiom> axioms = new ArrayList<OWLAxiom>();
        retriever.retrieveTypesForIndividual(subject.getIRI().toString()).forEach(type ->{
            if(allowedTypes.contains(type)){
                OWLClassExpression expr = factory.getOWLClass(IRI.create(type));
                OWLAxiom axiom = factory.getOWLClassAssertionAxiom(expr, subject);
                axioms.add(axiom);
            }
        });
        return axioms;
    }
}
