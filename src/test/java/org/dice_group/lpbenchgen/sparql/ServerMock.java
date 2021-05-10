package org.dice_group.lpbenchgen.sparql;

import org.apache.commons.io.FileUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.*;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status;
import org.simpleframework.http.core.Container;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ServerMock implements Container {

    private Model model;


    public ServerMock(String rdfFile) throws FileNotFoundException {
        model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model, new FileInputStream(rdfFile), RDFLanguages.filenameToLang(rdfFile));
    }

    @Override
    public void handle(Request request, Response resp) {
        String content=null;
        try {
            content = request.getParameter("query");
            Query q = QueryFactory.create(content);
            QueryExecution qexec = QueryExecutionFactory.create(q, model);
            ResultSet res = qexec.execSelect();
            resp.setCode(200);
            resp.setContentType(WebContent.contentTypeResultsJSON);
            String contentStr = ResultSetMgr.asString(res, ResultSetLang.RS_JSON);
            resp.setContentType(WebContent.contentTypeResultsJSON);
            resp.getOutputStream().write(contentStr.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
            resp.setCode(500);

        } finally {
            try {
                resp.getOutputStream().close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }




    }


}