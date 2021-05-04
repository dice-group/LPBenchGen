package org.dice_group.lpbenchgen.dl;

import com.google.common.collect.Lists;
import org.dice_group.lpbenchgen.config.PosNegExample;
import org.dice_group.lpbenchgen.sparql.IndividualRetriever;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Creating concepts based upon just a TBox
 *
 * @author Lixi Alié Conrads
 */
public class OWLTBoxPositiveCreator implements OWLTBoxConceptCreator{

    private static final Logger LOGGER = LoggerFactory.getLogger(OWLTBoxPositiveCreator.class.getName());

    private final List<String> originalTypes;
    private final Reasoner res;
    private final String namespace;
    private final boolean strict;
    private IndividualRetriever retriever;
    private OWLOntology onto;
    /**
     * The Allowed types.
     */
    public List<String> allowedTypes;
    private Parser parser;
    private OWLDataFactory dataFactory = new OWLDataFactoryImpl();
    private Map<String, Set<OWLAxiom>> type2axioms = new HashMap<String, Set<OWLAxiom>>();
    private Map<String, Collection<OWLClassExpression>> type2expr = new HashMap<String, Collection<OWLClassExpression>>();

    private Random negationMutationRandom;

    public long seed=0;

    /**
     * The Max depth.
     */
    public int maxDepth=2;
    /**
     * The Max concept length.
     */
    public int maxConceptLength=8;
    /**
     * The Min concept length.
     */
    public int minConceptLength=4;
    /**
     * The Infer direct super classes.
     */
    public boolean inferDirectSuperClasses=true;
    private int testLimit=1;
    public int maxLateralDepth=0;
    public double negationMutationRatio;

    /**
     * Instantiates a new Owlt box positive creator.
     *
     * @param retriever        the retriever
     * @param onto             the onto
     * @param allowedTypes     the allowed types
     * @param parser           the parser
     * @param res              the res
     * @param allowedNamespace the allowed namespace
     */
    public OWLTBoxPositiveCreator(IndividualRetriever retriever, OWLOntology onto, List<String> allowedTypes, Parser parser, Reasoner res, String allowedNamespace, boolean strict, Integer minimum){
        this.retriever=retriever;
        this.onto=onto;
        this.originalTypes = allowedTypes;
        this.allowedTypes=new ArrayList<String>(new HashSet<String>(allowedTypes));
        this.parser=parser;
        this.res=res;
        this.namespace=allowedNamespace;
        if(strict){
            testLimit=minimum;
        }
        this.strict= strict;
    }

    public Collection<PosNegExample> createDistinctConcepts(int noOfConcepts){
        negationMutationRandom = new Random(seed);
        Set<PosNegExample> ret = new HashSet<PosNegExample>();
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
                        example.setNegatives(creator.negationConcepts);
                        ret.add(example);
                        if (ret.size() >= noOfConcepts) {
                            break;
                        }
                    }
                } else {
                    noResults++;
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        List<PosNegExample> ret2= new ArrayList<PosNegExample>(ret);
        LOGGER.info("Final {} concepts. [{} to small, {} with no results/timeout]", ret2.size(), toSmallCount, noResults);
        return ret2;
    }

    /**
     * Gets concept length.
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
     * Create concepts collection.
     *
     * @return the collection
     */
    public Collection<OWLClassExpression> createConcepts(){
        List<OWLClassExpression> concepts = new ArrayList<OWLClassExpression>();
        List<String> allowedTypes = new ArrayList<String>(this.allowedTypes);
        allowedTypes.forEach(type -> {
            if(!type.equals("http://www.w3.org/2002/07/owl#Thing")) {
                Set<OWLAxiom> axioms = getAxiomsForClass(dataFactory.getOWLClass(IRI.create(type)));
                type2axioms.put(type, axioms);
                Collection<OWLClassExpression> ret = createConceptsFromClass(dataFactory.getOWLClass(IRI.create(type)), false, 0);
                type2expr.put(type, ret);
                concepts.addAll(ret);
            }
        });

        LOGGER.info("Found {} theoretically possible concepts.", concepts.size());

        Collections.shuffle(concepts, new Random(seed));
        return concepts;
    }

    private Set<OWLAxiom> getAxiomsForClass(OWLClass owlClass) {

        Set<OWLAxiom> ret = new HashSet<OWLAxiom>();
        if(inferDirectSuperClasses && originalTypes.contains(owlClass.getIRI().toString())) {
            res.getSuperClasses(owlClass, true).forEach(node -> {
                node.getEntities().forEach(superClass -> {
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
                });
            });
        }
        ret.addAll(onto.getReferencingAxioms(owlClass));
        return ret;
    }


    private Collection<OWLClassExpression> createConceptsFromClass(OWLClass owlClass, boolean onlyRange, int depth){
        Collection<OWLClassExpression> ret = new ArrayList<OWLClassExpression>();
        ret.add(owlClass);
        addNegationMutation(ret, owlClass);
        //TODO create OR with other Classes if both are not sub/super classes of each other and or concats allowed
        createConceptFromExpression(owlClass, depth, ret);
        getAxiomsForClass(owlClass).forEach(axiom ->{
            if(axiom instanceof OWLObjectPropertyRangeAxiom){
                createConceptFromExpression(((OWLObjectPropertyRangeAxiom) axiom), depth,ret);
            }

        });
        return ret;
    }

    private void createConceptFromExpression(OWLObjectPropertyRangeAxiom ax, int depth, Collection<OWLClassExpression> ret) {
        OWLObjectPropertyExpression prop = ax.getProperty();
        OWLClassExpression owlClass2 = ax.getRange();
        OWLClassExpression propExpr = new OWLObjectSomeValuesFromImpl(prop, owlClass2);
        if(getConceptLength(propExpr) <= maxConceptLength) {
            ret.add(propExpr);

            addNegationMutation(ret, propExpr);
            for (OWLClassExpression expr : createConceptFromExpression(owlClass2, getRangePropertiesForClass((OWLClass) owlClass2), depth)) {
                OWLClassExpression pexpr = new OWLObjectSomeValuesFromImpl(prop, expr);
                if (getConceptLength(pexpr) <= maxConceptLength) {
                    addNegationMutation(ret, pexpr);
                    ret.add(pexpr);
                }
            }
            for (OWLClass inferredClass : res.getSubClasses(owlClass2).getFlattened()) {
                if (allowedTypes.contains(inferredClass.getIRI().toString())) {
                    OWLClassExpression negationPropExpr = new OWLObjectSomeValuesFromImpl(prop, inferredClass);
                    ret.add(negationPropExpr);
                    addNegationMutation(ret, negationPropExpr);
                    for (OWLClassExpression expr : createConceptFromExpression(inferredClass, getRangePropertiesForClass((OWLClass) inferredClass), depth)) {
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

    private void addNegationMutation(Collection<OWLClassExpression> ret, OWLClassExpression pexpr) {
            if (getConceptLength(pexpr)+1 <= maxConceptLength) {
                double mutate = negationMutationRandom.nextDouble();
                if(mutate <= negationMutationRatio){
                    ret.add(new OWLObjectComplementOfImpl(pexpr).getNNF());
                }
            }
        }

    private void createConceptFromExpression(OWLClass start, int depth, Collection<OWLClassExpression> ret) {
        ret.addAll(createConceptFromExpression(start, getRangePropertiesForClass(start), depth));
    }

    //TODO allow random negative Mutations
    private Collection<OWLClassExpression> createConceptFromExpression(OWLClassExpression start, Collection<OWLObjectPropertyExpression> properties, int depth) {
        Collection<OWLClassExpression> ret = new ArrayList<OWLClassExpression>();
        List<OWLClassExpression> sub = new ArrayList<OWLClassExpression>();

        for(OWLObjectPropertyExpression p0 : properties){
            for(OWLClass rangeClass : getClassesForProperty(p0)){
                OWLClassExpression propRange2 = new OWLObjectSomeValuesFromImpl(p0, rangeClass);
                sub.add(propRange2);

                OWLClassExpression pexpr = new OWLObjectIntersectionOfImpl(Lists.newArrayList(start, propRange2));
                if (getConceptLength(pexpr) <= maxConceptLength) {
                    addNegationMutation(ret, pexpr);
                    ret.add(pexpr);
                }

                for(OWLClassExpression expr : createConceptFromExpression(rangeClass, getRangePropertiesForClass(rangeClass), depth+1)){
                    OWLClassExpression propRange3 = new OWLObjectSomeValuesFromImpl(p0, expr);
                    sub.add(propRange3);
                    OWLClassExpression pexpr2 = new OWLObjectIntersectionOfImpl(Lists.newArrayList(start, propRange3));
                    if (getConceptLength(pexpr2) <= maxConceptLength) {
                        addNegationMutation(ret, pexpr2);
                        ret.add(pexpr2);
                    }
                }
            }
        }

        for(List<OWLClassExpression> lateral : lateralSub(sub)){
            lateral.add(start);
            OWLClassExpression pexpr = new OWLObjectIntersectionOfImpl(lateral);
            if (getConceptLength(pexpr) <= maxConceptLength) {
                addNegationMutation(ret, pexpr);
                ret.add(pexpr);
            }
        }
        return ret;
    }

    private List<List<OWLClassExpression>> lateralSub(List<OWLClassExpression> sub) {
        List<List<OWLClassExpression>> lateral  = new ArrayList<List<OWLClassExpression>>();
        for(int i =0; i<sub.size();i++){
            OWLClassExpression current = sub.get(i);
            addLateral(lateral, sub, i+1, Lists.newArrayList(current), 1);
        }

        return lateral;
     }

    private void addLateral(List<List<OWLClassExpression>> lateral, List<OWLClassExpression> sub, int i, List<OWLClassExpression> current, int depth) {
        if(depth>maxLateralDepth){
            return;
        }
        for(int j=i;j<sub.size();j++){
            List<OWLClassExpression> newList = new ArrayList<>(current);
            newList.add(sub.get(j));
            int size=newList.size()-1;
            for(OWLClassExpression cexpr : newList) {
                size += getConceptLength(cexpr);
            }
            if (size < maxConceptLength) {
                lateral.add(newList);
                addLateral(lateral, sub, j, newList, depth + 1);
            }
        }
    }

    private Collection<OWLObjectPropertyExpression> getRangePropertiesForClass(OWLClass owlClass) {
        Collection<OWLObjectPropertyExpression> ret = new ArrayList<OWLObjectPropertyExpression>();
        getAxiomsForClass(owlClass).forEach(axiom -> {
            if (axiom instanceof OWLObjectPropertyDomainAxiom) {
                OWLObjectPropertyDomainAxiom axiom1 = dataFactory.getOWLObjectPropertyDomainAxiom(((OWLObjectPropertyDomainAxiom) axiom).getProperty(), owlClass);
                ret.add(axiom1.getProperty());
            }
        });
        res.getSuperClasses(owlClass, false).getFlattened().forEach(inferredClass-> {
            getAxiomsForClass(inferredClass).forEach(axiom -> {
                if (axiom instanceof OWLObjectPropertyDomainAxiom) {
                    OWLObjectPropertyDomainAxiom axiom1 = dataFactory.getOWLObjectPropertyDomainAxiom(((OWLObjectPropertyDomainAxiom) axiom).getProperty(), owlClass);
                    ret.add(axiom1.getProperty());
                }
            });
        });
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
