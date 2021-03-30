package org.dice_group.LPBenchGen.dl;

import com.google.common.collect.Sets;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxPrefixNameShortFormProvider;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

public class Parser {


    private OWLOntology ontology;
    private OWLOntology owlOntology;
    private BidirectionalShortFormProviderAdapter provider;


    public OWLOntology getOntology() {
        return ontology;
    }

    public void setOntology(OWLOntology ontology) {
        this.ontology = ontology;
    }

    public Parser(String ontologyFile) throws OWLOntologyCreationException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        ontology = manager.loadOntologyFromOntologyDocument(new File(ontologyFile));
        owlOntology = manager.loadOntologyFromOntologyDocument(IRI.create("http://www.w3.org/2002/07/owl"));
        provider = new BidirectionalShortFormProviderAdapter(Sets.newHashSet(ontology, owlOntology), new ManchesterOWLSyntaxPrefixNameShortFormProvider(ontology));

    }

    public  OWLClassExpression parseManchesterConcept(String concept){
        OWLEntityChecker checker = new ShortFormEntityChecker(provider);

        OWLDataFactory dataFactory = new OWLDataFactoryImpl();


        ManchesterOWLSyntaxClassExpressionParser parser = new ManchesterOWLSyntaxClassExpressionParser(dataFactory, checker);
        return parser.parse(concept);
    }

    public Collection<String> getRulesInExpr(OWLClassExpression ce, Collection<OWLDataProperty> dataRules){
        Collection<String> rules = new HashSet<String>();

        //only Object Properties for now.
        ce.getObjectPropertiesInSignature().forEach(prop -> {
            rules.add(prop.getIRI().toString());
        });
        ce.getDataPropertiesInSignature().forEach(prop->{
            dataRules.add(prop);
        });
        return rules;
    }

    public String render(OWLClassExpression concept) {
        ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
        renderer.setShortFormProvider(provider);
        return renderer.render(concept);
    }

    public String getShortName(String uri) {
        return provider.getShortForm(new OWLDataFactoryImpl().getOWLClass(uri));
    }
}
