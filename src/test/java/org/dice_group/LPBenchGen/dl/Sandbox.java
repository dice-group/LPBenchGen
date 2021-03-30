package org.dice_group.LPBenchGen.dl;


import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxPrefixNameShortFormProvider;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Sandbox {

    @Test
    public void test() throws OWLOntologyCreationException, IOException, OWLOntologyStorageException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        OWLOntologyLoaderConfiguration loaderConfig = new OWLOntologyLoaderConfiguration();
        loaderConfig.setStrict(false);

        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new FileDocumentSource(new File("./testbench6-ontology.owl")), loaderConfig);

        BidirectionalShortFormProviderAdapter provider = new BidirectionalShortFormProviderAdapter(Sets.newHashSet(ontology), new ManchesterOWLSyntaxPrefixNameShortFormProvider(ontology));

        OWLEntityChecker checker = new ShortFormEntityChecker(provider);

        OWLDataFactory dataFactory = new OWLDataFactoryImpl();


        Configuration conf = new Configuration();
        conf.ignoreUnsupportedDatatypes=true;
        OWLReasoner rc = new Reasoner(conf, ontology);
        ManchesterOWLSyntaxClassExpressionParser parser = new ManchesterOWLSyntaxClassExpressionParser(dataFactory, checker);
        JSONArray examples = new JSONArray(FileUtils.readFileToString(new File("./testbench6-lp.json")));
        examples.forEach(example ->{
            JSONObject exa = ((JSONObject)example);
            String concept = exa.getString("concept");
            exa.remove("concept");

            OWLClassExpression expr = parser.parse(concept);
            Set<OWLNamedIndividual> inds = rc.getInstances(expr, true).getFlattened();
            int[] vals = evaluatePosNeg(getSetFromJSON(exa, "positives"), getSetFromJSON(exa, "negatives"), inds);
            System.out.println("tp: "+vals[0]+", fp: "+vals[1]+", fn: "+vals[2]);

            if(vals[1]>0||vals[2]>0){
                System.out.println(concept);
                System.out.println(exa);
            }
        });
    }

    private Set<String> getSetFromJSON(JSONObject posNegJson, String key) {
        Set<String> ret = new HashSet<String>();
        posNegJson.getJSONArray(key).forEach(uri -> ret.add(uri.toString()));
        return ret;
    }

    private int[] evaluatePosNeg(Set<String> positiveExamples, Set<String> negativeExamples, Set<OWLNamedIndividual> received) {
        int[] evalVals= new int[]{0,0,0};
        int foundPosExa=0;
        for(OWLIndividual individual: received){
            String individualStr = individual.asOWLNamedIndividual().getIRI().toString();
            if(positiveExamples.contains(individualStr)){
                evalVals[0]++; //tp ++
                positiveExamples.remove(individualStr);
                foundPosExa++;
            }
            else if(negativeExamples.contains(individualStr)){
                evalVals[1]++; //fp ++
                System.out.println("fp: "+individualStr);
            }
        }
        if(!positiveExamples.isEmpty()){
            System.out.println(positiveExamples);
        }
        evalVals[2] = positiveExamples.size();
        //fn (if we found all positive examples, good, if we missed one, this will be accounted here)
        return evalVals;
    }
}
