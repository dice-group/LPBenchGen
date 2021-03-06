package org.dice_group.lpbenchgen.dl.creator;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.dice_group.lpbenchgen.config.Configuration;
import org.dice_group.lpbenchgen.config.PosNegExample;
import org.dice_group.lpbenchgen.dl.ConceptLengthCalculator;
import org.dice_group.lpbenchgen.dl.OWLTBoxConceptCreator;
import org.dice_group.lpbenchgen.dl.Parser;
import org.dice_group.lpbenchgen.sparql.IndividualRetriever;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Creating positive and negative concepts based upon a TBox and checks if they have results using an ABox
 *
 * @author Lixi Alié Conrads
 */
public class OWLTBoxPositiveCreator implements OWLTBoxConceptCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OWLTBoxPositiveCreator.class.getName());

    private final List<String> originalTypes;
    private final OWLReasoner res;
    private final String namespace;
    private final boolean strict;
    private final IndividualRetriever retriever;
    private final OWLOntology onto;
    private final List<String> allowedTypes;
    private final Parser parser;
    private final OWLDataFactory dataFactory = new OWLDataFactoryImpl();
    private Random negationMutationRandom;
    private final long seed;
    private final int maxDepth;
    private final int maxConceptLength;
    private final int minConceptLength;
    private final boolean inferDirectSuperClasses;
    private int testLimit=1;
    private final double negationMutationRatio;

    /**
     * Instantiates a new OWL TBox Creator
     *
     * @param conf             the configuration to use
     * @param retriever        the retriever
     * @param ontology             the onto
     * @param allowedTypes     the allowed types
     * @param parser           the parser
     * @param res              the res
     * @param namespace the allowed namespace
     */
    public OWLTBoxPositiveCreator(Configuration conf, IndividualRetriever retriever, OWLOntology ontology, List<String> allowedTypes, Parser parser, OWLReasoner res, String namespace) {
        this.retriever=retriever;
        this.onto=ontology;
        this.originalTypes = allowedTypes;
        this.allowedTypes= new ArrayList<>(new HashSet<>(allowedTypes));
        this.parser=parser;
        this.res=res;
        this.namespace=namespace;
        if(conf.isStrict()){
            testLimit=conf.getMinNoOfExamples();
        }
        this.strict= conf.isStrict();
        minConceptLength=conf.getMinConceptLength();
        maxConceptLength=conf.getMaxConceptLength();
        maxDepth=conf.getMaxDepth();
        seed=conf.getSeed();
        negationMutationRatio=conf.getNegationMutationRatio();
        inferDirectSuperClasses=conf.getInferDirectSuperClasses();
        negationMutationRandom = new Random(seed);

    }

    /**
     * returns all allowed types. (will include the direct retrieved ones if inferDirectSuperClasses is true
     *
     * @return the allowed types
     */
    public List<String> getAllowedTypes() {
        return allowedTypes;
    }

    @Override
    public Collection<PosNegExample> createDistinctConcepts(int noOfConcepts){
        Set<PosNegExample> ret = new HashSet<>();
        int toSmallCount=0;
        int noResults=0;
        for(OWLClassExpression concept : createConcepts()){
            if(getConceptLength(concept)<minConceptLength){
                toSmallCount++;
                continue;
            }
            try {
                int size =retriever.retrieveIndividualsForConcept(concept, testLimit, 5, true).size();
                if ((strict && size==testLimit) || (!strict && size>0) ){
                    OWLNegationCreator creator = new OWLNegationCreator();
                    concept.accept(creator);
                    //creator.prune();
                    int negativeSize=0;
                    for(OWLClassExpression negativeConcept : creator.negationConcepts) {
                        negativeSize += retriever.retrieveIndividualsForConcept(negativeConcept, testLimit, 5, true).size();
                    }
                    if ((strict && negativeSize>=testLimit) || (!strict && negativeSize>0) ) {
                        PosNegExample example = new PosNegExample();
                        example.setPositive(parser.render(concept));
                        example.setNegativeGenerated(true);
                        example.setNegativesExpr(creator.negationConcepts);
                        ret.add(example);
                        if (ret.size() >= noOfConcepts) {
                            break;
                        }
                    }
                    else{
                        noResults++;
                    }
                } else {
                    noResults++;
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        List<PosNegExample> ret2= new ArrayList<>(ret);
        LOGGER.info("Final {} concepts. [{} to small, {} with no results/timeout]", ret2.size(), toSmallCount, noResults);
        return ret2;
    }

    /**
     * Gets the concept length of a class expression
     *
     * @param concept the concept
     * @return the concept length
     */
    protected Double getConceptLength(OWLClassExpression concept) {
        ConceptLengthCalculator renderer = new ConceptLengthCalculator();
        renderer.render(concept);
        return 1.0*renderer.conceptLength;
    }


    /**
     * Creates a list of class expressions
     * @return a list of class expressions
     */
    public Collection<OWLClassExpression> createConcepts(){
        List<OWLClassExpression> concepts = new ArrayList<>();
        List<String> allowedTypes = new ArrayList<>(this.allowedTypes);
        for(String type:  allowedTypes){
            if(!type.equals("http://www.w3.org/2002/07/owl#Thing")) {
                Collection<OWLClassExpression> ret = createConceptsFromClass(dataFactory.getOWLClass(IRI.create(type)));
                concepts.addAll(ret);
            }
        }
        concepts = concepts.stream().distinct().collect(Collectors.toList());

        LOGGER.info("Found {} theoretically possible concepts.", concepts.size());
        Collections.shuffle(concepts, new Random(seed));
        return concepts;
    }

    private Set<OWLAxiom> getAxiomsForClass(OWLClass owlClass) {

        Set<OWLAxiom> ret = new HashSet<>();
        if(inferDirectSuperClasses && originalTypes.contains(owlClass.getIRI().toString())) {
            res.getSuperClasses(owlClass, true).forEach(node -> node.getEntities().forEach(superClass -> {
                if(namespace==null || superClass.getIRI().toString().startsWith(namespace)) {
                    onto.getReferencingAxioms(superClass).stream().filter(x -> x instanceof OWLObjectPropertyRangeAxiom || x instanceof OWLObjectPropertyDomainAxiom).forEach(
                            supClassAxiom -> {

                                ret.add(supClassAxiom);
                                String str = superClass.getIRI().toString();
                                if (!allowedTypes.contains(str))
                                    allowedTypes.add(str);

                            }
                    );
                }
            }));
        }
        ret.addAll(onto.getReferencingAxioms(owlClass));
        return ret;
    }


    /**
     * Creates a list of class expressions starting at the owlClass
     * @param owlClass start point of the class expressions
     * @return list of class expressions
     */
    protected Collection<OWLClassExpression> createConceptsFromClass(OWLClass owlClass){
        Collection<OWLClassExpression> ret = new ArrayList<>();
        ret.add(owlClass);
        addNegationMutation(ret, owlClass);
        createConceptFromExpression(owlClass, ret);
        getAxiomsForClass(owlClass).forEach(axiom ->{
            if(axiom instanceof OWLObjectPropertyRangeAxiom){
                createConceptFromExpression(owlClass, ((OWLObjectPropertyRangeAxiom) axiom), ret);
            }

        });
        return ret;
    }

    private void createConceptFromExpression(OWLClass start, OWLObjectPropertyRangeAxiom ax, Collection<OWLClassExpression> ret) {
        if(1 <= maxDepth) {
            OWLObjectPropertyExpression prop = ax.getProperty();
            OWLClassExpression propExpr = new OWLObjectSomeValuesFromImpl(prop, start);
            if (getConceptLength(propExpr) <= maxConceptLength) {
                ret.add(propExpr);

                addNegationMutation(ret, propExpr);
                for (OWLClassExpression expr : createConceptFromExpression(start, getRangePropertiesForClass(start), 1 + 1)) {
                    OWLClassExpression pexpr = new OWLObjectSomeValuesFromImpl(prop, expr);
                    if (getConceptLength(pexpr) <= maxConceptLength) {
                        addNegationMutation(ret, pexpr);
                        ret.add(pexpr);
                    }
                }
                for (OWLClass inferredClass : res.getSubClasses(start).getFlattened()) {
                    if (allowedTypes.contains(inferredClass.getIRI().toString())) {
                        OWLClassExpression negationPropExpr = new OWLObjectSomeValuesFromImpl(prop, inferredClass);
                        ret.add(negationPropExpr);
                        addNegationMutation(ret, negationPropExpr);
                        for (OWLClassExpression expr : createConceptFromExpression(inferredClass, getRangePropertiesForClass(inferredClass), 1 + 1)) {
                            OWLClassExpression pexpr = new OWLObjectSomeValuesFromImpl(prop, expr);
                            if (getConceptLength(pexpr) <= maxConceptLength) {
                                addNegationMutation(ret, pexpr);
                                ret.add(pexpr);
                            }

                        }
                    }
                }
            }
        }

    }

    private void addNegationMutation(Collection<OWLClassExpression> ret, OWLClassExpression pexpr) {
            if (getConceptLength(pexpr)+1 <= maxConceptLength) {
                double mutate = negationMutationRandom.nextDouble();
                if(mutate <= negationMutationRatio){
                    ret.add(new OWLObjectComplementOfImpl(pexpr).getNNF());
                }
            }
        }

    private void createConceptFromExpression(OWLClass start, Collection<OWLClassExpression> ret) {
        ret.addAll(createConceptFromExpression(start, getRangePropertiesForClass(start), 1));
    }


    private Collection<OWLClassExpression> createConceptFromExpression(OWLClassExpression start, Collection<OWLObjectPropertyExpression> properties, int depth) {
        Collection<OWLClassExpression> ret = new ArrayList<>();
        if(depth<=maxDepth) {

            for (OWLObjectPropertyExpression p0 : properties) {
                for (OWLClass rangeClass : getClassesForProperty(p0)) {
                    OWLClassExpression propRange2 = new OWLObjectSomeValuesFromImpl(p0, rangeClass);

                    OWLClassExpression pexpr = new OWLObjectIntersectionOfImpl(Lists.newArrayList(start, propRange2));
                    if (getConceptLength(pexpr) <= maxConceptLength) {
                        addNegationMutation(ret, pexpr);
                        ret.add(pexpr);
                    }

                    for (OWLClassExpression expr : createConceptFromExpression(rangeClass, getRangePropertiesForClass(rangeClass), depth + 1)) {
                        OWLClassExpression propRange3 = new OWLObjectSomeValuesFromImpl(p0, expr);
                        OWLClassExpression pexpr2 = new OWLObjectIntersectionOfImpl(Lists.newArrayList(start, propRange3));
                        if (getConceptLength(pexpr2) <= maxConceptLength) {
                            addNegationMutation(ret, pexpr2);
                            ret.add(pexpr2);
                        }
                    }
                }
            }


        }
        return ret;
    }


    private Collection<OWLObjectPropertyExpression> getRangePropertiesForClass(OWLClass owlClass) {
        Collection<OWLObjectPropertyExpression> ret = new ArrayList<>();
        getAxiomsForClass(owlClass).forEach(axiom -> {
            if (axiom instanceof OWLObjectPropertyDomainAxiom) {
                OWLObjectPropertyDomainAxiom axiom1 = dataFactory.getOWLObjectPropertyDomainAxiom(((OWLObjectPropertyDomainAxiom) axiom).getProperty(), owlClass);
                ret.add(axiom1.getProperty());
            }
        });
        res.getSuperClasses(owlClass, false).getFlattened().forEach(inferredClass-> getAxiomsForClass(inferredClass).forEach(axiom -> {
            if (axiom instanceof OWLObjectPropertyDomainAxiom) {
                OWLObjectPropertyDomainAxiom axiom1 = dataFactory.getOWLObjectPropertyDomainAxiom(((OWLObjectPropertyDomainAxiom) axiom).getProperty(), owlClass);
                ret.add(axiom1.getProperty());
            }
        }));
        return ret;
    }

    private Collection<OWLClass> getClassesForProperty(OWLObjectPropertyExpression prop) {
        Collection<OWLClass> ret = res.getObjectPropertyRanges(prop, false).getFlattened().stream().filter(x -> allowedTypes.contains(x.getIRI().toString())).collect(Collectors.toList());
        Collection<OWLClass> tmp = new HashSet<>();
        for(OWLClass clasz : ret){
            tmp.addAll(res.getSubClasses(clasz, false).getFlattened().stream().filter(x -> allowedTypes.contains(x.getIRI().toString())).collect(Collectors.toList()));
        }
        ret.addAll(tmp);
        ret.remove(dataFactory.getOWLClass("http://www.w3.org/2002/07/owl#Nothing"));
        ret.remove(dataFactory.getOWLClass("http://www.w3.org/2002/07/owl#Thing"));

        return ret;
    }


}
