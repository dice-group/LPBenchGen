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
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.*;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.dice_group.lpbenchgen.sparql.AbstractSPARQLIndividualRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ModelClosedWorldIndividualRetriever uses a closed world assumption and works on a simple jena model
 *
 * @author Lixi Alié Conrads
 */
public class ModelClosedWorldIndividualRetriever extends AbstractSPARQLIndividualRetriever {

    /**
     * The constant LOGGER.
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(ModelClosedWorldIndividualRetriever.class.getName());
    /**
     * The constant DEFAULT_VARIABLE_NAME.
     */
    private Model model = ModelFactory.createDefaultModel();

    /**
     * Creates a ModelClosedWorldIndividualRetriever using an file containing the ABox
     *
     * @param aboxFile the rdf file containing the abox
     * @throws FileNotFoundException if the abox file doesn't exists
     */
    public ModelClosedWorldIndividualRetriever(String aboxFile) throws FileNotFoundException {
        RDFDataMgr.read(model, new FileInputStream(aboxFile), RDFLanguages.filenameToLang(aboxFile));
    }


    protected List<String> createRequest(String sparqlQuery, int timeOut){
        List<String> ret =  new ArrayList<String>();

        Query q = QueryFactory.create(sparqlQuery);
        ResultSet res = getResultMap(q);
        while (res.hasNext()) {
            ret.add(res.next().get(DEFAULT_VARIABLE_NAME).toString());
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
        QueryExecution qexec = QueryExecutionFactory.create(q, model);

        ResultSet res = qexec.execSelect();
        return res;
    }
}
