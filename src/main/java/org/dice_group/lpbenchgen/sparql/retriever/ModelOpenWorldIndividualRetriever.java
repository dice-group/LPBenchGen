package org.dice_group.lpbenchgen.sparql.retriever;

import openllet.owlapi.OpenlletReasonerFactory;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.dice_group.lpbenchgen.dl.Parser;
import org.dice_group.lpbenchgen.sparql.IndividualRetriever;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ModelOpenWorldIndividualRetriever uses an OpenWorld Assumption, a simple Jena Model containing the ABox and
 * an OWLReasoner (Openllet) to retrieve Individuals.
 */
public class ModelOpenWorldIndividualRetriever  implements IndividualRetriever {


    private final Model model = ModelFactory.createDefaultModel();
    private final OWLReasoner reasoner;
    private final Parser parser;


    /**
     * Creates a ModelOpenWorldIndividualRetriever using an file containing the ABox
     *
     * @param aboxFile the rdf file containing the abox
     * @throws FileNotFoundException if the abox file doesn't exist
     * @throws OWLOntologyCreationException if the ontology cannot be created
     */
    public ModelOpenWorldIndividualRetriever(String aboxFile) throws FileNotFoundException, OWLOntologyCreationException {
        RDFDataMgr.read(model, new FileInputStream(aboxFile), RDFLanguages.filenameToLang(aboxFile));
        parser = new Parser(aboxFile);
        this.reasoner = OpenlletReasonerFactory.getInstance().createReasoner(parser.getOntology());
    }

    /**
     * Returns the Reasoner used.
     * @return the OWLReasoner used
     */
    public OWLReasoner getReasoner(){
        return reasoner;
    }


    @Override
    public List<String> retrieveIndividualsForConcept(OWLClassExpression concept, int limit, int timeOut, boolean reasoning) {
        List<String> ret = retrieveIndividualsForConcept(concept, reasoning);
        if(ret.size()>limit && limit!=0){
            return ret.subList(0, limit);
        }
        return ret;
    }

    @Override
    public List<String> retrieveIndividualsForConcept(OWLClassExpression concept, boolean reasoning) {
        return this.reasoner.getInstances(concept, false).getFlattened().stream().map(x -> x.getIRI().toString()).collect(Collectors.toList());

    }

    @Override
    public Collection<String> retrieveTypesForIndividual(String uri) {
        OWLNamedIndividual individual = new OWLNamedIndividualImpl(IRI.create(uri));
        return this.reasoner.getTypes(individual).getFlattened().stream().map(x -> x.getIRI().toString()).collect(Collectors.toSet());
    }

    @Override
    public ResultSet getResultMap(Query q) {
        QueryExecution qexec = QueryExecutionFactory.create(q, model);
        return qexec.execSelect();
    }
}