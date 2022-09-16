package org.dice_group.lpbenchgen.dl;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.ElementWalker;
import org.apache.jena.vocabulary.OWL2;
import org.dice_group.lpbenchgen.sparql.*;
import org.dice_group.lpbenchgen.sparql.visitors.QueryRemoveUselessTriplesVisitor;
import org.dice_group.lpbenchgen.sparql.visitors.QueryTripleMappingVisitor;
import org.dice_group.lpbenchgen.sparql.visitors.VariableCollector;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Will fill the ABox using Individual retrievals.
 *
 * @author Lixi Alié Conrads
 */
public class ABoxFiller {

    private static final Logger LOGGER = LoggerFactory.getLogger(ABoxFiller.class.getName());

    private final OWLDataFactory factory = new OWLDataFactoryImpl();
    private final List<OWLClass> allowedTypes;
    private final IndividualRetriever retriever;
    private final int limit;

    /**
     * Instantiates a new A box filler.
     *
     * @param retriever    the retriever
     * @param allowedTypes the allowed types
     * @param limit        the internal query limit
     */
    public ABoxFiller(IndividualRetriever retriever, List<OWLClass> allowedTypes, int limit) {
        this.retriever = retriever;
        this.allowedTypes = allowedTypes;
        this.limit = limit;
    }

    /**
     * Gets Individuals from a concept and a startIndividual and will convert the concept
     * to a SPARQL query, using that start Individual.
     * The result will be mapped to triples and added to the ABox
     *
     * @param concept         the concept
     * @param startIndividual the start individual
     * @param ontology        the ontology
     * @return true if addition was successful, false otherwise
     */
    public boolean addIndividualsFromConcept(OWLClassExpression concept, OWLNamedIndividual startIndividual, OWLOntology ontology) {
        OWL2SPARQL sparql = new OWL2SPARQL();
        sparql.setUseReasoning(true);
        Query q = sparql.asQuery(concept, "?var");
        VariableCollector varC = new VariableCollector();
        ElementWalker.walk(q.getQueryPattern(), varC);
        q.getProjectVars().clear();
        q.getGroupBy().clear();
        varC.vars.forEach(v -> q.getProjectVars().add(Var.alloc(v)));


        String queryStr = q.serialize().replace("?var", startIndividual.toString());

        try {
            if (varC.vars.isEmpty()) {
                //only a type query
                OWLNamedIndividual subject = factory.getOWLNamedIndividual(startIndividual);
                ontology.addAxioms(getTypeAxiomsForIndividual(subject));
            } else {
                Query query = QueryFactory.create(queryStr);
                QueryRemoveUselessTriplesVisitor visitor2 = new QueryRemoveUselessTriplesVisitor();
                ElementWalker.walk(query.getQueryPattern(), visitor2);

                query.setLimit(limit);
                ResultSet res = retriever.getResultMap(query);
                if (res == null) {
                    return false;
                }
                QueryTripleMappingVisitor visitor = new QueryTripleMappingVisitor();
                ElementWalker.walk(query.getQueryPattern(), visitor);
                visitor.patternToMap(res);
                Set<OWLAxiom> axioms = createAxiomsFromMap(visitor.getMap());
                ontology.addAxioms(axioms);
                Set<OWLAxiom> axioms2 = createAxiomsFromMap(visitor2.triples);
                ontology.addAxioms(axioms2);
            }
        } catch (Exception e) {
            String id = UUID.randomUUID().toString();
            LOGGER.error("{}:, Could not add Individuals for concept {} with Individual {}", id, concept, startIndividual);
            LOGGER.error(id + ": Due to: ", e);
            return false;
        }
        return true;
    }

    private Set<OWLAxiom> createAxiomsFromMap(Set<Triple> map) {
        Set<OWLAxiom> axioms = new HashSet<>();
        for (Triple triple : map) {
            String individual = triple.subject;
            OWLNamedIndividual subject = factory.getOWLNamedIndividual(individual);
            axioms.addAll(getTypeAxiomsForIndividual(subject));


            if (((RDFNode) triple.object).isLiteral()) {
                OWLDataPropertyExpression property = factory.getOWLDataProperty(triple.predicate);
                Literal lit = ((RDFNode) triple.object).asLiteral();
                OWLDatatype type = factory.getOWLDatatype(IRI.create(lit.getDatatypeURI()));
                OWLLiteral literal = factory.getOWLLiteral(lit.getValue().toString(), type);
                OWLAxiom axiom = factory.getOWLDataPropertyAssertionAxiom(property, subject, literal);
                axioms.add(axiom);
            } else {
                OWLObjectPropertyExpression property = factory.getOWLObjectProperty(triple.predicate);
                if (triple.object.toString().startsWith(":no prefix")) {
                    triple.object = individual + "#" + triple.object.toString().substring(10);
                }
                if (triple.predicate.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
//                    OWLAxiom axiom = factory.getOWLClassAssertionAxiom(factory.getOWLClass(triple.object.toString()), subject);
                    //axioms.add(axiom);
                } else {
                    OWLNamedIndividual object = factory.getOWLNamedIndividual(triple.object.toString());
                    axioms.addAll(getTypeAxiomsForIndividual(object));
                    OWLAxiom axiom = factory.getOWLObjectPropertyAssertionAxiom(property, subject, object);
                    axioms.add(axiom);
                }
            }
        }
        return axioms;
    }

    private Collection<OWLAxiom> getTypeAxiomsForIndividual(OWLNamedIndividual subject) {
        return retriever.retrieveTypesForIndividual(subject)
                .stream()
                .filter(type -> allowedTypes.contains(type) && !OWL2.NamedIndividual.equals(type))
                .map(type -> factory.getOWLClassAssertionAxiom(type, subject))
                .collect(Collectors.toList());
    }
}
