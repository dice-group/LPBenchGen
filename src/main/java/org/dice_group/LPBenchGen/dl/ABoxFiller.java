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
import org.dice_group.LPBenchGen.sparql.visitors.QueryRemoveUselessTriplesVisitor;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.util.*;


/**
 * The type A box filler.
 *
 * @author Lixi Alié Conrads
 */
public class ABoxFiller {

    private static final Logger LOGGER = LoggerFactory.getLogger(ABoxFiller.class.getName());

    private OWLDataFactory factory = new OWLDataFactoryImpl();
    private List<String> allowedTypes;
    private IndividualRetriever retriever;

    /**
     * Instantiates a new A box filler.
     *
     * @param retriever    the retriever
     * @param allowedTypes the allowed types
     */
    public ABoxFiller(IndividualRetriever retriever, List<String> allowedTypes){
        this.retriever=retriever;
        this.allowedTypes=allowedTypes;
    }

    /**
     * Add individuals from concept boolean.
     *
     * @param concept         the concept
     * @param startIndividual the start individual
     * @param ontology        the ontology
     * @return the boolean
     */
    public boolean addIndividualsFromConcept(OWLClassExpression concept, String startIndividual, OWLOntology ontology){

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
                QueryRemoveUselessTriplesVisitor visitor2 = new QueryRemoveUselessTriplesVisitor();
                ElementWalker.walk(query.getQueryPattern(), visitor2);

                query.setLimit(100);
                ResultSet res = retriever.getResultMap(query);
                if(res==null){
                    return false;
                }
                QueryTripleMappingVisitor visitor = new QueryTripleMappingVisitor(startIndividual);
                ElementWalker.walk(query.getQueryPattern(), visitor);
                visitor.patternToMap(res);
                List<OWLAxiom> axioms = createAxiomsFromMap(visitor.getMap());
                ontology.addAxioms(axioms);
            }
        }catch(Exception e){
            String id = UUID.randomUUID().toString();
            LOGGER.error("{}:, Could not add Individuals for concept {} with Individual {}", id, concept, startIndividual);
            LOGGER.error(id+": Due to: ", e);
            return false;
        }
        return true;
    }

    private List<OWLAxiom> createAxiomsFromMap(Map<String, List<String[]>> map) {
        List<OWLAxiom> axioms = new ArrayList<OWLAxiom>();
        map.keySet().forEach(individual ->{
            OWLNamedIndividual subject = factory.getOWLNamedIndividual(individual);
            axioms.addAll(getTypeAxiomsForIndividual(subject));
            map.get(individual).forEach(triple -> {
                OWLObjectPropertyExpression property = factory.getOWLObjectProperty(triple[0]);
                if(triple[1].startsWith(":no prefix")){
                    triple[1]=individual+"#"+triple[1].substring(10);
                }
                OWLNamedIndividual object = factory.getOWLNamedIndividual(triple[1]);
                axioms.addAll(getTypeAxiomsForIndividual(object));
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
