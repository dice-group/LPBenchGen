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
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.io.File;
import java.util.Collection;

/**
 * OWL Ontology Parser
 *
 * @author Lixi Alié Conrads
 */
public class Parser {
    private final ManchesterOWLSyntaxPrefixNameShortFormProvider prefix;
    private final OWLOntology ontology;
    private final BidirectionalShortFormProviderAdapter provider;
    private final ManchesterOWLSyntaxClassExpressionParser parser;

    private final ManchesterOWLSyntaxOWLObjectRendererImpl renderer;
    private final OWLDataFactory owlDataFactory;


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
        ontology = manager.loadOntologyFromOntologyDocument(new File(ontologyFile));
        OWLOntology owlOntology = manager.loadOntologyFromOntologyDocument(IRI.create("http://www.w3.org/2002/07/owl"));
        prefix = new ManchesterOWLSyntaxPrefixNameShortFormProvider(ontology);
        provider = new BidirectionalShortFormProviderAdapter(Sets.newHashSet(ontology, owlOntology), prefix);
        OWLEntityChecker checker = new ShortFormEntityChecker(provider);
        OWLDataFactory dataFactory = new OWLDataFactoryImpl();
        parser = new ManchesterOWLSyntaxClassExpressionParser(dataFactory, checker);
        renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
        renderer.setShortFormProvider(provider);
        owlDataFactory = new OWLDataFactoryImpl();
    }

    /**
     * Parse manchester concept owl class expression.
     *
     * @param concept the concept
     * @return the owl class expression
     */
    public OWLClassExpression parseManchesterConcept(String concept) {
        return parser.parse(concept);
    }

    public OWLClass parseOWLClass(IRI iri) {
        return new OWLClassImpl(iri);
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
     * Render a Class Expression to Manchester Syntax.
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
        return provider.getShortForm(owlDataFactory.getOWLClass(uri));
    }
}
