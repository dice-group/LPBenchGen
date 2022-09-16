package org.dice_group.lpbenchgen.sparql.retriever;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.dice_group.lpbenchgen.sparql.AbstractSPARQLIndividualRetriever;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * ModelClosedWorldIndividualRetriever uses a closed world assumption and works on a simple jena model
 *
 * @author Lixi Alié Conrads
 */
public class ModelClosedWorldIndividualRetriever extends AbstractSPARQLIndividualRetriever {

    /**
     * The constant LOGGER.
     */
    private final String ontologyResource;
    /**
     * The constant DEFAULT_VARIABLE_NAME.
     */
    private final Model model = ModelFactory.createDefaultModel();

    /**
     * Creates a ModelClosedWorldIndividualRetriever using an file containing the ABox
     *
     * @param aboxFile the rdf file containing the abox
     * @throws FileNotFoundException if the abox file doesn't exists
     */
    public ModelClosedWorldIndividualRetriever(String aboxFile) throws FileNotFoundException {
        RDFDataMgr.read(model, new FileInputStream(aboxFile), RDFLanguages.filenameToLang(aboxFile));
        ResIterator resIterator = model.listSubjectsWithProperty(RDF.type, OWL.Ontology);
        ontologyResource = resIterator.nextResource().getURI();
    }

    protected Stream<String> createRequest(String sparqlQuery, int timeOut) {
        // Convert the iterator to Spliterator
        Spliterator<QuerySolution> querySolutionSpliterator = Spliterators.spliteratorUnknownSize(getResultMap(QueryFactory.create(sparqlQuery)), 0);

        return StreamSupport
                .stream(querySolutionSpliterator, false)
                .map(querySolution -> querySolution.get(DEFAULT_VARIABLE_NAME).toString())
                .filter(str -> !str.equals(ontologyResource));
    }


    @Override
    public ResultSet getResultMap(Query q) {
        QueryExecution qexec = QueryExecutionFactory.create(q, model);
        return qexec.execSelect();
    }
}
