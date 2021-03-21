package org.dice_group.LPBenchGen.sparql;

import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;
import org.aksw.owl2sparql.OWLClassExpressionToSPARQLConverter;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.riot.WebContent;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.dice_group.LPBenchGen.dl.OWL2SPARQL;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLLiteral;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLLiteralImplString;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class IndividualRetriever {

    public static final String DEFAULT_VARIABLE_NAME = "var";
    private String endpoint;
    public boolean useCSV=false;

    public IndividualRetriever(String endpoint){
        this.endpoint=endpoint;
    }

    public List<String> retrieveIndividualsForType(String uriType){
        String sparqlQuery = createTypeQuery(uriType);

        return createRequest(sparqlQuery, 180);
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

    private List<String> createRequest(String sparqlQuery, int timeOut){
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
            actualContentType = resp.getEntity().getContentType().getValue();
            if(actualContentType==null || actualContentType.isEmpty()){
                actualContentType=QueryEngineHTTP.defaultSelectHeader();
            }
            Lang lang = WebContent.contentTypeToLangResultSet(actualContentType);
            test = read(resp.getEntity().getContent());
            InputStream is = new ByteArrayInputStream(test.getBytes(StandardCharsets.UTF_8));
            ResultSet res= ResultSetMgr.read(is, lang);

            //Query q = QueryFactory.create(sparqlQuery);


            //QueryEngineHTTP qexec = QueryExecutionFactory.createServiceRequest(endpoint, q);
            //ResultSet res = qexec.execSelect();
            while (res.hasNext()) {
                ret.add(res.next().get(DEFAULT_VARIABLE_NAME).toString());
            }
        }catch(Exception e){
            //System.out.println("Code: "+code+", CT: "+actualContentType);
            //e.printStackTrace();
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
            e.printStackTrace();
        }
        return "";

    }

    private String createTypeQuery(String uriType) {
        return "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?"+DEFAULT_VARIABLE_NAME+" { ?"+DEFAULT_VARIABLE_NAME+" rdf:type <"+uriType+">} LIMIT 2000";
    }

    private String createTypesQuery(String uriIndividual) {
        return "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?"+DEFAULT_VARIABLE_NAME+" { <"+uriIndividual+"> rdf:type ?"+DEFAULT_VARIABLE_NAME+"}";
    }

    public List<String> retrieveIndividualsForConcept(OWLClassExpression concept, int limit, int timeOut){
        String sparqlQuery = createQuery(concept);
        Query q = QueryFactory.create(sparqlQuery);
        q.setLimit(limit);
        return createRequest(q.serialize(), timeOut );
    }

    public List<String> retrieveIndividualsForConcept(OWLClassExpression concept){
        String sparqlQuery = createQuery(concept);
        return createRequest(sparqlQuery, 180);
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
        Query q  = converter.asQuery(concept, "?var");
        q.setLimit(100);
        return q.serialize();
    }

    public Collection<String> retrieveTypesForIndividual(String uri) {
        return createRequest(createTypesQuery(uri), 180);
    }

    public Collection<String[]> retrieveIndividualsForRule(String uri, Collection<String> rules) {
        Collection<String[]> ret = new HashSet<String[]>();
        for(String rule: rules){
            for(String r : createRequest(createRuleQuery(uri, rule), 180)){
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

    public ResultSet getResultMap(Query q) {
        try {
            //QueryExecution qexec = QueryExecutionFactory.sparqlService(endpoint, q);
            //ResultSet res = qexec.execSelect();
            HttpClient client = HttpClients.custom().setConnectionManager(new BasicHttpClientConnectionManager()).build();
            HttpGet get = new HttpGet(endpoint+"?query="+ URLEncoder.encode(q.serialize(), "UTF-8"));
            get.addHeader(HttpHeaders.ACCEPT, QueryEngineHTTP.defaultSelectHeader());
            HttpResponse resp = client.execute(get);
            String actualContentType = resp.getEntity().getContentType().getValue();
            if(actualContentType==null || actualContentType.isEmpty()){
                actualContentType=QueryEngineHTTP.defaultSelectHeader();
            }
            Lang lang = WebContent.contentTypeToLangResultSet(actualContentType);
            String test = read(resp.getEntity().getContent());
            InputStream is = new ByteArrayInputStream(test.getBytes(StandardCharsets.UTF_8));
            ResultSet res= ResultSetMgr.read(is, lang);
            return res;
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
