package org.dice_group.lpbenchgen.eval;

import com.google.common.collect.Sets;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.impl.LiteralLabelFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.dice_group.lpbenchgen.dl.Parser;
import org.dice_group.lpbenchgen.lp.LPGenerator;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxPrefixNameShortFormProvider;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectUnionOfImpl;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class BaseLineSystem {

    private OWLOntology ontology;
    private BidirectionalShortFormProviderAdapter provider;
    private Parser parser;


    public  static void main(String[] args) throws Exception {
        if(args.length!=3){
            System.out.println("system ONTOLOGY BENCHMARK.ttl OUTPUT.ttl");
        }
        else {
            Model m = ModelFactory.createDefaultModel();
            m.read(new FileReader(args[1]), null, "TTL");
            BaseLineSystem system = new BaseLineSystem();
            system.loadOntology(new File(args[0]));
            Model predictions = system.addPredictions(m);
            RDFDataMgr.write(new BufferedOutputStream(new FileOutputStream(args[2])), predictions, Lang.TTL);
        }
    }

    public Model addPredictions(Model testModel) throws Exception {
        Model predictions = ModelFactory.createDefaultModel();
        Reasoner reasoner = createReasoner();
        List<Resource> problems = new ArrayList<Resource>();
        testModel.listStatements(null, RDF.type, LPGenerator.LEARNING_PROBLEM_CLASS).forEachRemaining(problem -> {
            problems.add(problem.getSubject());
        });
        AtomicInteger count = new AtomicInteger(0);
        for(Resource res : problems){
            Set<OWLNamedIndividual> positives = getExamples(testModel, res);
            String concept = createConcept(positives);
            addAllPredictions(res, reasoner, concept, positives, predictions,count);
        }
        return predictions;
    }

    private void addAllPredictions(Resource res, Reasoner reasoner, String concept, Set<OWLNamedIndividual> positives, Model predictions, AtomicInteger count) {
        OWLClassExpression classExpr=parser.parseManchesterConcept(concept);
        Set<String> gold = new HashSet<String>();
        for(OWLNamedIndividual ind : positives){
            gold.add(ind.getIRI().toString());
        }
        int current = count.getAndIncrement();
        Resource resultRes = ResourceFactory.createResource(LPGenerator.RESOURCE_PREFIX+"result_"+current);
        predictions.add(resultRes, ResourceFactory.createProperty(LPGenerator.PROPERTY_PREFIX+"pertainsTo"), res);
        predictions.add(resultRes, ResourceFactory.createProperty(LPGenerator.PROPERTY_PREFIX+"belongsToLP"), "true", XSDDatatype.XSDboolean);
        reasoner.getInstances(classExpr).getFlattened().forEach(individual -> {
            if(!gold.contains(individual.getIRI().toString())){
                //predictions.add(res, LPGenerator.RDF_PROPERTY_INCLUDE, ResourceFactory.createResource(individual.getIRI().toString()));
                //int current = count.getAndIncrement();
                //Resource resultRes = ResourceFactory.createResource(LPGenerator.RESOURCE_PREFIX+"result_"+current);
                predictions.add(resultRes, ResourceFactory.createProperty(LPGenerator.PROPERTY_PREFIX+"resource"), ResourceFactory.createResource(individual.getIRI().toString()));
                //predictions.add(resultRes, ResourceFactory.createProperty(LPGenerator.PROPERTY_PREFIX+"pertainsTo"), res);
                //predictions.add(resultRes, ResourceFactory.createProperty(LPGenerator.PROPERTY_PREFIX+"belongsToLP"), "true", XSDDatatype.XSDboolean);
            }
        });
    }

    private Reasoner createReasoner(){
        org.semanticweb.HermiT.Configuration confRes = new org.semanticweb.HermiT.Configuration();
        confRes.ignoreUnsupportedDatatypes=true;
        confRes.throwInconsistentOntologyException=false;
        return new Reasoner(confRes, ontology);
    }

    private Set<OWLNamedIndividual> getExamples(Model testModel, Resource res) {
        Set<OWLNamedIndividual> ret = new HashSet<OWLNamedIndividual>();
        testModel.listStatements(res, LPGenerator.RDF_PROPERTY_INCLUDE, (RDFNode) null).forEachRemaining(triple ->{
            ret.add(new OWLNamedIndividualImpl(IRI.create(triple.getObject().asResource().getURI())));
        });
        return ret;
    }

    public String createConcept(Set<OWLNamedIndividual> posExamples) throws IOException, Exception {
        Set<OWLClass> posAxioms = new HashSet<OWLClass>();
        for(OWLIndividual pos : posExamples) {
            ontology.getClassAssertionAxioms(pos).forEach(classAssertion ->{
                posAxioms.addAll(classAssertion.getClassesInSignature());
            });
        }

        ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
        renderer.setShortFormProvider(provider);
        List<OWLClassExpression> classes = new ArrayList<>();
        posAxioms.forEach(owlClass -> {classes.add(owlClass.getNNF());});

        OWLClassExpression pos = new OWLObjectUnionOfImpl(classes);
        return renderer.render(pos).replace("\n", " ");
    }


    public void loadOntology(File ontologyFile) throws IOException, Exception {
        parser = new Parser(ontologyFile.getAbsolutePath());
        ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(ontologyFile);

        provider = new BidirectionalShortFormProviderAdapter(Sets.newHashSet(ontology), new ManchesterOWLSyntaxPrefixNameShortFormProvider(ontology));

    }

}
