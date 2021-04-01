package org.dice_group.lpbenchgen.dl;

import com.google.common.collect.Sets;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxPrefixNameShortFormProvider;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

/**
 * The type Parser.
 *
 * @author Lixi Alié Conrads
 */
public class Parser {


    private OWLOntology ontology;
    private OWLOntology owlOntology;
    private BidirectionalShortFormProviderAdapter provider;


    /**
     * Gets ontology.
     *
     * @return the ontology
     */
    public OWLOntology getOntology() {
        return ontology;
    }

    /**
     * Sets ontology.
     *
     * @param ontology the ontology
     */
    public void setOntology(OWLOntology ontology) {
        this.ontology = ontology;
    }

    /**
     * Instantiates a new Parser.
     *
     * @param ontologyFile the ontology file
     * @throws OWLOntologyCreationException the owl ontology creation exception
     */
    public Parser(String ontologyFile) throws OWLOntologyCreationException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        ontology = manager.loadOntologyFromOntologyDocument(new File(ontologyFile));
        owlOntology = manager.loadOntologyFromOntologyDocument(IRI.create("http://www.w3.org/2002/07/owl"));
        provider = new BidirectionalShortFormProviderAdapter(Sets.newHashSet(ontology, owlOntology), new ManchesterOWLSyntaxPrefixNameShortFormProvider(ontology));

    }

    /**
     * Parse manchester concept owl class expression.
     *
     * @param concept the concept
     * @return the owl class expression
     */
    public  OWLClassExpression parseManchesterConcept(String concept){
        OWLEntityChecker checker = new ShortFormEntityChecker(provider);

        OWLDataFactory dataFactory = new OWLDataFactoryImpl();


        ManchesterOWLSyntaxClassExpressionParser parser = new ManchesterOWLSyntaxClassExpressionParser(dataFactory, checker);
        return parser.parse(concept);
    }

    /**
     * Get rules in expr collection.
     *
     * @param ce        the ce
     * @param dataRules the data rules
     * @return the collection
     */
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

    /**
     * Render string.
     *
     * @param concept the concept
     * @return the string
     */
    public String render(OWLClassExpression concept) {
        ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
        renderer.setShortFormProvider(provider);
        return renderer.render(concept);
    }

    /**
     * Gets short name.
     *
     * @param uri the uri
     * @return the short name
     */
    public String getShortName(String uri) {
        return provider.getShortForm(new OWLDataFactoryImpl().getOWLClass(uri));
    }
}
