package org.dice_group.lpbenchgen.sparql.retriever;

import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.jena.query.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.riot.WebContent;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.dice_group.lpbenchgen.sparql.AbstractSPARQLIndividualRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
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
        QueryExecution qexec = QueryExecutionFactory.sparqlService(endpoint, "SELECT ?s WHERE { ?s <" + RDF.type + "> <" + OWL.Ontology + "> } LIMIT 1");
        ResultSet res = qexec.execSelect();
        if (res.hasNext()) {
            ontologyResource = res.next().get("s").toString();
        } else {
            ontologyResource = "";
        }
    }


    protected Stream<String> createRequest(String sparqlQuery, int timeOut) {
        // TODO: this needs heavy cleanup

        try {
            int timeout = timeOut * 1000;
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(timeout)
                    .setConnectionRequestTimeout(timeout)
                    .setSocketTimeout(timeout).build();
            CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
            //HttpClient client = HttpClients.custom().setConnectionManager(new BasicHttpClientConnectionManager()).build();
            HttpGet get = new HttpGet(endpoint + "?query=" + URLEncoder.encode(sparqlQuery, StandardCharsets.UTF_8));

            get.addHeader(HttpHeaders.ACCEPT, QueryEngineHTTP.defaultSelectHeader());
            HttpResponse resp = client.execute(get);
            String actualContentType = resp.getEntity().getContentType().getValue().replace("; charset=utf-8", "");
            if (actualContentType.isEmpty()) {
                actualContentType = QueryEngineHTTP.defaultSelectHeader();
            }
            Lang lang = WebContent.contentTypeToLangResultSet(actualContentType);
            String test = read(resp.getEntity().getContent());
            InputStream is = new ByteArrayInputStream(test.getBytes(StandardCharsets.UTF_8));
            Spliterator<QuerySolution> querySolutionSpliterator = Spliterators.spliteratorUnknownSize(ResultSetMgr.read(is, lang), 0);

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
            public InputStream openStream() throws IOException {
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
            HttpClient client = HttpClients.custom().setConnectionManager(new BasicHttpClientConnectionManager()).build();
            HttpGet get = new HttpGet(endpoint + "?query=" + URLEncoder.encode(q.serialize(), StandardCharsets.UTF_8));
            get.addHeader(HttpHeaders.ACCEPT, QueryEngineHTTP.defaultSelectHeader());
            HttpResponse resp = client.execute(get);
            String actualContentType = resp.getEntity().getContentType().getValue();
            if (actualContentType == null || actualContentType.isEmpty()) {
                actualContentType = QueryEngineHTTP.defaultSelectHeader();
            }
            Lang lang = WebContent.contentTypeToLangResultSet(actualContentType.replace("; charset=utf-8", ""));
            String test = read(resp.getEntity().getContent());
            InputStream is = new ByteArrayInputStream(test.getBytes(StandardCharsets.UTF_8));
            return ResultSetMgr.read(is, lang);
        } catch (Exception e) {
            LOGGER.error("Could not retrieve result map due to ", e);
        }
        return null;
    }
}
