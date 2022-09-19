package org.dice_group.lpbenchgen.sparql.retriever;

import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.dice_group.lpbenchgen.sparql.AbstractSPARQLIndividualRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Uses an Closed World Assumption and a SPARQL endpoint to retrieve Individuals
 *
 * @author Lixi Alié Conrads
 */
public class SPARQLClosedWorldIndividualRetriever extends AbstractSPARQLIndividualRetriever {

    /**
     * The constant LOGGER.
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(SPARQLClosedWorldIndividualRetriever.class.getName());
    /**
     * The constant DEFAULT_VARIABLE_NAME.
     */
    private final String endpoint;

    private final String ontologyResource;


    /**
     * Creates a retriever using endpoint as the HTTP SPARQL endpoint
     *
     * @param endpoint the endpoint
     */
    public SPARQLClosedWorldIndividualRetriever(String endpoint) {
        this.endpoint = endpoint;
        RDFConnection connect = RDFConnection.connect(endpoint);
        QueryExecution query = connect.query("SELECT ?s WHERE { ?s <" + RDF.type + "> <" + OWL.Ontology + "> } LIMIT 1");
        ResultSet res = query.execSelect();
        if (res.hasNext()) {
            ontologyResource = res.next().get("s").toString();
        } else {
            ontologyResource = "";
        }
    }


    protected Stream<String> createRequest(String sparqlQuery, int timeOut) {
        try {
            int timeout = timeOut * 1000;
            RDFConnection connect = RDFConnection.connect(endpoint);
            QueryExecution query = connect.query(sparqlQuery);
            query.setTimeout(timeout);
            Spliterator<QuerySolution> querySolutionSpliterator = Spliterators.spliteratorUnknownSize(query.execSelect(), 0);

            return StreamSupport
                    .stream(querySolutionSpliterator, false)
                    .map(querySolution -> querySolution.get(DEFAULT_VARIABLE_NAME).toString())
                    .filter(str -> !str.equals(ontologyResource));

        } catch (Exception e) {
            String id = UUID.randomUUID().toString();
            LOGGER.debug("Could not execute request due to {}, see debug id:{}", e.getMessage(), id);
            LOGGER.debug(id + ": ", e);
            return Stream.empty();
        }
    }

    private String read(InputStream content) {
        ByteSource byteSource = new ByteSource() {
            @Override
            public InputStream openStream() {
                return content;
            }
        };

        try {
            return byteSource.asCharSource(Charsets.UTF_8).read();
        } catch (IOException e) {
            LOGGER.error("Could not read stream due to ", e);
        }
        return "";

    }

    @Override
    public ResultSet getResultMap(Query q) {
        try {
            RDFConnection connect = RDFConnection.connect(endpoint);
            QueryExecution query = connect.query(q);
            return query.execSelect();
        } catch (Exception e) {
            LOGGER.error("Could not retrieve result map due to ", e);
        }
        return null;
    }
}
