package org.dice_group.lpbenchgen.dl.creator;

import com.google.common.collect.Lists;
import org.dice_group.lpbenchgen.config.Configuration;
import org.dice_group.lpbenchgen.config.PosNegExample;
import org.dice_group.lpbenchgen.dl.ConceptLengthCalculator;
import org.dice_group.lpbenchgen.dl.ManchesterRenderer;
import org.dice_group.lpbenchgen.dl.OWLTBoxConceptCreator;
import org.dice_group.lpbenchgen.dl.Parser;
import org.dice_group.lpbenchgen.sparql.IndividualRetriever;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
    private int testLimit = 1;
    private final double negationMutationRatio;

    /**
     * Instantiates a new OWL TBox Creator
     *
     * @param conf         the configuration to use
     * @param retriever    the retriever
     * @param ontology     the onto
     * @param allowedTypes the allowed types
     * @param parser       the parser
     * @param res          the res
     * @param namespace    the allowed namespace
     */
    public OWLTBoxPositiveCreator(Configuration conf, IndividualRetriever retriever, OWLOntology ontology, List<String> allowedTypes, Parser parser, OWLReasoner res, String namespace) {
        this.retriever = retriever;
        this.onto = ontology;
        this.originalTypes = allowedTypes;
        this.allowedTypes = new ArrayList<>(new HashSet<>(allowedTypes));
        this.parser = parser;
        this.res = res;
        this.namespace = namespace;
        if (conf.isStrict()) {
            testLimit = conf.getMinNoOfExamples();
        }
        this.strict = conf.isStrict();
        minConceptLength = conf.getMinConceptLength();
        maxConceptLength = conf.getMaxConceptLength();
        maxDepth = conf.getMaxDepth();
        seed = conf.getSeed();
        negationMutationRatio = conf.getNegationMutationRatio();
        inferDirectSuperClasses = conf.getInferDirectSuperClasses();
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
    public Collection<PosNegExample> createDistinctConcepts(int noOfConcepts, boolean withExamples) {
        AtomicInteger tooSmallCount = new AtomicInteger(0);
        AtomicInteger noResults = new AtomicInteger(0);
        ArrayList<OWLClassExpression> concepts = (ArrayList<OWLClassExpression>) createConcepts();

        Iterator<OWLClassExpression> iterator = concepts.iterator();
        List<PosNegExample> ret = new ArrayList<>();
        while (iterator.hasNext()) {
            int i = 0;
            List<OWLClassExpression> part = new ArrayList<>(64);
            while (iterator.hasNext() && i++ < 64) {
                part.add(iterator.next());
            }


            List<PosNegExample> part_ret = part.parallelStream().map(concept -> {
                LOGGER.info("Candidate concept: {}", ManchesterRenderer.renderNNF(concept));
                if (getConceptLength(concept) < minConceptLength) {
                    LOGGER.info("  too short");
                    tooSmallCount.incrementAndGet();
                    return new PosNegExample();
                } else if (withExamples) {
                    try {
                        LOGGER.info("  generating example individuals");
                        int size = retriever.retrieveIndividualsForConcept(concept, testLimit, 5, true).size();
                        LOGGER.info("  satisfied by {} individuals", size);
                        if ((strict && size == testLimit) || (!strict && size > 0)) {
                            OWLNegationCreator creator = new OWLNegationCreator();
                            concept.accept(creator);
                            //creator.prune();
                            int negativeSize = 0;
                            LOGGER.info("  {} negative concepts", creator.negationConcepts.size());
                            for (OWLClassExpression negativeConcept : creator.negationConcepts) {
                                negativeSize += retriever.retrieveIndividualsForConcept(negativeConcept, testLimit, 5, true).size();
                            }
                            LOGGER.info("  {} negative individuals", negativeSize);
                            if ((strict && negativeSize >= testLimit) || (!strict && negativeSize > 0)) {
                                PosNegExample example = new PosNegExample();
                                example.setPositive(parser.render(concept));
                                example.setNegativeGenerated(true);
                                example.setNegativesExpr(creator.negationConcepts);
                                LOGGER.info("  concept accepted");
                                return example;
                            } else {
                                tooSmallCount.incrementAndGet();
                                LOGGER.info("  concept discarded");
                                return new PosNegExample();
                            }
                        } else {
                            tooSmallCount.incrementAndGet();
                            LOGGER.info("  concept discarded");
                            return new PosNegExample();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        noResults.incrementAndGet();
                        LOGGER.info("  error");
                        return new PosNegExample();
                    }
                } else {
                    LOGGER.info("  not generating example individuals");
                    PosNegExample example = new PosNegExample();
                    example.setPositive(parser.render(concept));
                    example.setNegativeGenerated(true);
                    LOGGER.info("  concept accepted");
                    return example;
                }
            }).filter(example -> {
                return example.getPositive() != null && example.getPositive().compareTo("") != 0;
            }).distinct().collect(Collectors.toList());
            ret.addAll(part_ret);
            if (ret.size() >= noOfConcepts) {
                while (ret.size() > noOfConcepts) {
                    ret.remove(ret.size() - 1);
                }
                break;
            }
        }

        LOGGER.info("Final {} concepts. [{} too small, {} with no results/timeout]", ret.size(), tooSmallCount.get(), noResults.get());
        return ret;
    }

    /**
     * Gets the concept length of a class expression
     *
     * @param concept the concept
     * @return the concept length
     */
    protected Double getConceptLength(OWLClassExpression concept) {
        return 1.0 * ConceptLengthCalculator.calc(concept);
    }


    /**
     * Creates a list of class expressions
     *
     * @return a list of class expressions
     */
    public Collection<OWLClassExpression> createConcepts() {
        List<OWLClassExpression> concepts = new LinkedList<>();
        List<String> allowedTypes = new ArrayList<>(this.allowedTypes);
        for (String type : allowedTypes) {
            if (!type.equals("http://www.w3.org/2002/07/owl#Thing")) {
                createConceptsFromClass(dataFactory.getOWLClass(IRI.create(type)), concepts);
                concepts = concepts.stream().distinct().collect(Collectors.toList());
            }
        }

        for (String type : allowedTypes) {
            if (!type.equals("http://www.w3.org/2002/07/owl#Thing")) {
                createConceptsFromClass(dataFactory.getOWLClass(IRI.create(type)), concepts);
                concepts = concepts.stream().distinct().collect(Collectors.toList());
            }
        }

        int maxChainableLength = maxConceptLength - 2;
        if (maxChainableLength > 0) {
            ArrayList<AbstractMap.SimpleEntry<Integer, OWLClassExpression>> chainingCandidates = new ArrayList<>();
            for (OWLClassExpression clx : concepts) {
                int conceptLength = ConceptLengthCalculator.calc(clx);
                if (conceptLength <= maxChainableLength)
                    chainingCandidates.add(new AbstractMap.SimpleEntry<>(conceptLength, clx));
            }
            for (int i = 0; i < chainingCandidates.size(); i++) {
                AbstractMap.SimpleEntry<Integer, OWLClassExpression> left = chainingCandidates.get(i);
                for (int j = i; j < chainingCandidates.size(); j++) {
                    AbstractMap.SimpleEntry<Integer, OWLClassExpression> right = chainingCandidates.get(j);
                    if (left.getKey() + right.getKey() <= maxConceptLength) {
                        concepts.add(new OWLObjectUnionOfImpl(Arrays.asList(left.getValue(), right.getValue())));
                        concepts.add(new OWLObjectIntersectionOfImpl(Arrays.asList(left.getValue(), right.getValue())));
                    }

                }
            }
        }
        concepts = concepts.parallelStream().map(OWLClassExpression::getNNF).distinct().collect(Collectors.toList());
        LOGGER.info("Found {} theoretically possible concepts.", concepts.size());
        Collections.shuffle(concepts, new Random(seed));
        return concepts;
    }

    private Set<OWLAxiom> getAxiomsForClass(OWLClass owlClass) {

        Set<OWLAxiom> ret = new HashSet<>();
        if (inferDirectSuperClasses && originalTypes.contains(owlClass.getIRI().toString())) {
            res.getSuperClasses(owlClass, true).forEach(node -> node.getEntities().forEach(superClass -> {
                if (namespace == null || superClass.getIRI().toString().startsWith(namespace)) {
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
     *
     * @param owlClass start point of the class expressions
     * @return list of class expressions
     */
    protected void createConceptsFromClass(OWLClass owlClass, Collection<OWLClassExpression> ret) {
        ret.add(owlClass);
        addNegationMutation(ret, owlClass);
        createConceptFromExpression(owlClass, ret);
        getAxiomsForClass(owlClass).forEach(axiom -> {
            if (axiom instanceof OWLObjectPropertyRangeAxiom) {
                createConceptFromExpression(owlClass, ((OWLObjectPropertyRangeAxiom) axiom), ret);
            }

        });
    }

    private void createConceptFromExpression(OWLClass start, OWLObjectPropertyRangeAxiom ax, Collection<OWLClassExpression> ret) {
        if (1 <= maxDepth) {
            OWLObjectPropertyExpression prop = ax.getProperty();
            OWLClassExpression[] property_expressions = {new OWLObjectSomeValuesFromImpl(prop, start), new OWLObjectAllValuesFromImpl(prop, start)};
            for (OWLClassExpression propExpr : property_expressions)
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
        if (getConceptLength(pexpr) + 1 <= maxConceptLength) {
            double mutate = negationMutationRandom.nextDouble();
            if (mutate <= negationMutationRatio) {
                ret.add(new OWLObjectComplementOfImpl(pexpr).getNNF());
            }
        }
    }

    private void createConceptFromExpression(OWLClass start, Collection<OWLClassExpression> ret) {
        ret.addAll(createConceptFromExpression(start, getRangePropertiesForClass(start), 1));
    }


    private Collection<OWLClassExpression> createConceptFromExpression(OWLClassExpression start, Collection<OWLObjectPropertyExpression> properties, int depth) {
        Collection<OWLClassExpression> ret = new ArrayList<>();
        if (depth <= maxDepth) {

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
        res.getSuperClasses(owlClass, false).getFlattened().forEach(inferredClass -> getAxiomsForClass(inferredClass).forEach(axiom -> {
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
        for (OWLClass clasz : ret) {
            tmp.addAll(res.getSubClasses(clasz, false).getFlattened().stream().filter(x -> allowedTypes.contains(x.getIRI().toString())).collect(Collectors.toList()));
        }
        ret.addAll(tmp);
        ret.remove(dataFactory.getOWLClass("http://www.w3.org/2002/07/owl#Nothing"));
        ret.remove(dataFactory.getOWLClass("http://www.w3.org/2002/07/owl#Thing"));

        return ret;
    }


}
