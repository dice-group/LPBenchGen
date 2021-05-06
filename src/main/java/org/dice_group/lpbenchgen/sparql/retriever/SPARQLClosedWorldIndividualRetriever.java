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
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.riot.WebContent;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.dice_group.lpbenchgen.dl.OWL2SPARQL;
import org.dice_group.lpbenchgen.sparql.AbstractSPARQLIndividualRetriever;
import org.dice_group.lpbenchgen.sparql.IndividualRetriever;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Uses an Closed World Assumption and a SPARQL endpoint to retrieve Individuals
 *
 * @author Lixi Alié Conrads
 */
public class SPARQLClosedWorldIndividualRetriever  extends AbstractSPARQLIndividualRetriever {

    /**
     * The constant LOGGER.
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(SPARQLClosedWorldIndividualRetriever.class.getName());
    /**
     * The constant DEFAULT_VARIABLE_NAME.
     */
    private String endpoint;


    /**
     * Creates a retriever using endpoint as the HTTP SPARQL endpoint
     *
     * @param endpoint the endpoint
     */
    public SPARQLClosedWorldIndividualRetriever(String endpoint){
        this.endpoint=endpoint;
    }


    protected List<String> createRequest(String sparqlQuery, int timeOut){
        List<String> ret =  new ArrayList<String>();
        int code=0;
        String test;
        String actualContentType="";
        try {
            int timeout = timeOut;
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(timeout * 1000)
                    .setConnectionRequestTimeout(timeout * 1000)
                    .setSocketTimeout(timeout * 1000).build();
            CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
            //HttpClient client = HttpClients.custom().setConnectionManager(new BasicHttpClientConnectionManager()).build();
            HttpGet get = new HttpGet(endpoint+"?query="+ URLEncoder.encode(sparqlQuery, "UTF-8"));

            get.addHeader(HttpHeaders.ACCEPT, QueryEngineHTTP.defaultSelectHeader());
            HttpResponse resp = client.execute(get);
            code = resp.getStatusLine().getStatusCode();
            actualContentType = resp.getEntity().getContentType().getValue().replace("; charset=utf-8","");
            if(actualContentType==null || actualContentType.isEmpty()){
                actualContentType=QueryEngineHTTP.defaultSelectHeader();
            }
            Lang lang = WebContent.contentTypeToLangResultSet(actualContentType);
            test = read(resp.getEntity().getContent());
            InputStream is = new ByteArrayInputStream(test.getBytes(StandardCharsets.UTF_8));
            ResultSet res= ResultSetMgr.read(is, lang);

            while (res.hasNext()) {
                ret.add(res.next().get(DEFAULT_VARIABLE_NAME).toString());
            }
        }catch(Exception e){
            String id = UUID.randomUUID().toString();
            LOGGER.debug("Could not execute request due to {}, see debug id:{}", e.getMessage(), id);
            LOGGER.debug(id+": ", e);
        }
        return ret;
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
            LOGGER.error("Could not read stream due to ",e);
        }
        return "";

    }

    @Override
    public ResultSet getResultMap(Query q) {
        try {
            HttpClient client = HttpClients.custom().setConnectionManager(new BasicHttpClientConnectionManager()).build();
            HttpGet get = new HttpGet(endpoint+"?query="+ URLEncoder.encode(q.serialize(), "UTF-8"));
            get.addHeader(HttpHeaders.ACCEPT, QueryEngineHTTP.defaultSelectHeader());
            HttpResponse resp = client.execute(get);
            String actualContentType = resp.getEntity().getContentType().getValue();
            if(actualContentType==null || actualContentType.isEmpty()){
                actualContentType=QueryEngineHTTP.defaultSelectHeader();
            }
            Lang lang = WebContent.contentTypeToLangResultSet(actualContentType.replace("; charset=utf-8",""));
            String test = read(resp.getEntity().getContent());
            InputStream is = new ByteArrayInputStream(test.getBytes(StandardCharsets.UTF_8));
            ResultSet res= ResultSetMgr.read(is, lang);
            return res;
        }catch(Exception e){
            LOGGER.error("Could not retrieve result map due to " ,e);
        }
        return null;
    }
}
