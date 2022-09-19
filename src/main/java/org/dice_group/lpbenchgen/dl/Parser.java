package org.dice_group.lpbenchgen.dl;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxPrefixNameShortFormProvider;
import org.semanticweb.owlapi.manchestersyntax.renderer.ParserException;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;

import java.io.File;
import java.util.Collection;
import java.util.Set;

/**
 * OWL Ontology Parser
 *
 * @author Lixi Alié Conrads
 */
public class Parser {
    private final ManchesterOWLSyntaxPrefixNameShortFormProvider prefix;
    private final OWLOntology ontology;
    private final BidirectionalShortFormProviderAdapter provider;
    private final ManchesterOWLSyntaxParser parser;
    private final ManchesterOWLSyntaxOWLObjectRendererImpl renderer;


    public ManchesterOWLSyntaxPrefixNameShortFormProvider getPrefix() {
        return prefix;
    }

    /**
     * Gets ontology.
     *
     * @return the ontology
     */
    public OWLOntology getOntology() {
        return ontology;
    }

    /**
     * Instantiates a new Parser.
     *
     * @param ontologyFile the ontology file
     * @throws OWLOntologyCreationException the owl ontology creation exception
     */
    public Parser(String ontologyFile) throws OWLOntologyCreationException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        this.ontology = manager.loadOntologyFromOntologyDocument(new File(ontologyFile));
        try {
            OWLOntology ignore = manager.loadOntologyFromOntologyDocument(IRI.create("http://www.w3.org/2002/07/owl"));
        } catch (OWLOntologyCreationException ignore) {// never hit
        }

        {
            Set<OWLOntology> ontologies = manager.getOntologies(); // my OWLOntologyManager
            prefix = new ManchesterOWLSyntaxPrefixNameShortFormProvider(
                    manager.getOntologyFormat(ontology));
            provider = new BidirectionalShortFormProviderAdapter(
                    ontologies, prefix);
        }

        {
            parser = OWLManager.createManchesterParser();
            parser.setDefaultOntology(ontology); // my ontology
            ShortFormEntityChecker checker = new ShortFormEntityChecker(provider);
            parser.setOWLEntityChecker(checker);
        }
        {
            renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
            renderer.setShortFormProvider(provider);
        }
    }

    /**
     * Parse manchester concept owl class expression.
     *
     * @param concept the concept
     * @return the owl class expression
     */
    public OWLClassExpression parseManchesterConcept(String concept) throws ParserException {
        parser.setStringToParse(concept);
        return parser.parseClassExpression();
    }

    public OWLClass parseOWLClass(String iri_string) {
        return new OWLClassImpl(IRI.create(iri_string));
    }

    /**
     * Get rules in expressions.
     *
     * @param ce        the class expression where data and object properties are extracted
     * @param dataRules data properties in signature if ce are written to dataRules
     * @return object properties in signature of ce
     */
    public Collection<OWLObjectProperty> getRulesInExpr(OWLClassExpression ce, Collection<OWLDataProperty> dataRules) {
        // data properties
        dataRules.addAll(ce.getDataPropertiesInSignature());
        // object properties
        return ce.getObjectPropertiesInSignature();

    }

    /**
     * Render a Class Expression to Manchester Syntax. This uses the PrefixManager from the Parser and abbreviates all IRIs possible.
     *
     * @param concept the concept
     * @return the string
     */
    public String render(OWLClassExpression concept) {
        return renderer.render(concept);
    }

    /**
     * Gets a short form for a URI
     *
     * @param uri the uri
     * @return the short name
     */
    public String getShortName(String uri) {
        return provider.getShortForm(new OWLClassImpl(IRI.create(uri)));
    }
}
