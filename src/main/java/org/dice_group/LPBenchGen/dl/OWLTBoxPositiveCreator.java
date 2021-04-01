package org.dice_group.LPBenchGen.dl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.dice_group.LPBenchGen.sparql.IndividualRetriever;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectComplementOfImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectIntersectionOfImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectSomeValuesFromImpl;
import org.semanticweb.owlapi.reasoner.Node;

import java.util.*;

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
    public OWLTBoxPositiveCreator(IndividualRetriever retriever, OWLOntology onto, List<String> allowedTypes, Parser parser, Reasoner res, String allowedNamespace){
        this.retriever=retriever;
        this.onto=onto;
        this.originalTypes = allowedTypes;
        this.allowedTypes=new ArrayList<String>(new HashSet<String>(allowedTypes));
        this.parser=parser;
        this.res=res;
        this.namespace=allowedNamespace;
    }

    public Collection<String> createDistinctConcepts(int noOfConcepts){
        Set<String> ret = new HashSet<String>();
        int toSmallCount=0;
        int noResults=0;
        for(OWLClassExpression concept : createConcepts()){

            if(getConceptLength(concept)<minConceptLength){
                toSmallCount++;
                continue;
            }
            if(!retriever.retrieveIndividualsForConcept(concept, 1, 5).isEmpty()) {

                ret.add(parser.render(concept));
                if(ret.size()>=noOfConcepts){
                    break;
                }
            }else{
                noResults++;
            }

        }
        List<String> ret2= new ArrayList<String>(ret);
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
        concepts.addAll(createOverlappingClassConcepts());
        LOGGER.info("Found {} theoretically possible concepts.", concepts.size());
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


    private Collection<? extends OWLClassExpression> createOverlappingClassConcepts() {
        Collection<OWLClassExpression> ret = new ArrayList<OWLClassExpression>();

        for(int i=0;i<allowedTypes.size()-1;i++) {
            for (int j = i + 1; j < allowedTypes.size(); j++) {
                if (canIntersect(allowedTypes.get(i), allowedTypes.get(j))) {
                    ret.addAll(combineExpressions(type2expr.get(allowedTypes.get(i)), type2expr.get(allowedTypes.get(j))));
                }
            }
        }
        return ret;
    }

    private boolean canIntersect(String type1, String type2) {
        //only in SPARQL
        return false;
    }


    private Collection<OWLClassExpression> combineExpressions(Collection<OWLClassExpression> owlClassExpressions, Collection<OWLClassExpression> owlClassExpressions1) {
        Collection<OWLClassExpression> concepts = new ArrayList<OWLClassExpression>();
        Random rand = new Random();
        for(OWLClassExpression expr1 : owlClassExpressions){
            for(OWLClassExpression expr2 : owlClassExpressions1){
                int i = rand.nextInt(2);
                if(i==0) {
                    concepts.add(new OWLObjectIntersectionOfImpl(Lists.newArrayList(expr1, expr2)));
                }
                else if(i==1){
                    concepts.add(new OWLObjectIntersectionOfImpl(Lists.newArrayList(expr1, new OWLObjectComplementOfImpl(expr2))));
                }
                else{
                    concepts.add(new OWLObjectIntersectionOfImpl(Lists.newArrayList(new OWLObjectComplementOfImpl(expr1), expr2)));
                }
            }
        }
        return concepts;
    }

    private void createConceptFromRangeAxiom(OWLObjectPropertyRangeAxiom ax, int depth, Collection<OWLClassExpression> ret, OWLClass owlClass){
        OWLObjectPropertyExpression prop = ax.getProperty();
        OWLClassExpression owlClass2 = ax.getRange();
        owlClass2 = pruneUnionExpr(owlClass2);
        OWLClassExpression classRange =null;
        if(owlClass2 != null) {
            classRange = new OWLObjectSomeValuesFromImpl(prop, owlClass2);
            ret.add(classRange);
        }
        Collection<OWLClass> propClasses = getClassesForProperty(prop);
        propClasses.remove(owlClass2);
        propClasses.remove(owlClass);

        OWLClassExpression finalClassRange = classRange;
        propClasses.forEach(propClass ->{
            if(finalClassRange!=null && allowedTypes.contains(propClass.getIRI().toString())) {
                OWLClassExpression propRange = new OWLObjectSomeValuesFromImpl(prop, propClass);
                propRange = pruneUnionExpr(propRange);
                if(propRange!=null){
                    OWLClassExpression expr = new OWLObjectIntersectionOfImpl(Lists.newArrayList(finalClassRange, propRange));
                    //if (!retriever.retrieveIndividualsForConcept(expr, 1,1 ).isEmpty()) {

                    ret.add(expr);
                    if (depth < maxDepth) {
                        createConceptsFromClass(propClass, false, depth + 1).forEach(concept -> {
                            OWLClassExpression newExpr = new OWLObjectIntersectionOfImpl(Lists.newArrayList(expr, concept));
                            ret.add(newExpr);

                        });
                    }
                }
            }
        });
    }

    private void createConceptFromDomainAxiom(OWLObjectPropertyDomainAxiom ax, int depth, Collection<OWLClassExpression> ret, OWLClass owlClass){
        OWLObjectPropertyExpression prop = ax.getProperty();
        Collection<OWLClass> propClasses = getClassesForProperty(prop);

        OWLClassExpression owlClass2 = ax.getDomain();
        owlClass2 = pruneUnionExpr(owlClass2);

        propClasses.remove(owlClass2);
        propClasses.remove(owlClass);
        OWLClassExpression finalOwlClass = owlClass2;
        propClasses.forEach(propClass ->{
            if(finalOwlClass !=null &&  allowedTypes.contains(propClass.getIRI().toString())) {
                OWLClassExpression propRange = new OWLObjectSomeValuesFromImpl(prop, propClass);
                propRange = pruneUnionExpr(propRange);
                if(propRange!=null){

                    OWLClassExpression expr = new OWLObjectIntersectionOfImpl(Lists.newArrayList(owlClass, propRange));
                    //if (!retriever.retrieveIndividualsForConcept(expr, 1, 1).isEmpty()) {

                    ret.add(expr);

                    if (depth < maxDepth) {
                        createConceptsFromClass(propClass, false, depth + 1).forEach(concept -> {
                            //OWLClassExpression iexpr = new OWLObjectIntersectionOfImpl(Lists.newArrayList(owlClass, concept));
                            OWLClassExpression pexpr = new OWLObjectIntersectionOfImpl(Lists.newArrayList(finalOwlClass, new
                                    OWLObjectSomeValuesFromImpl(prop, concept)));
                            if (getConceptLength(pexpr) < maxConceptLength) {
                                OWLClassExpression newExpr = new OWLObjectIntersectionOfImpl(Lists.newArrayList(pexpr, concept));
                                ret.add(newExpr);
                            }
                        });
                    }
                }
            }
        });
    }


    private Collection<OWLClassExpression> createConceptsFromClass(OWLClass owlClass, boolean onlyRange, int depth){
        Collection<OWLClassExpression> ret = new ArrayList<OWLClassExpression>();
        ret.add(owlClass);

        getAxiomsForClass(owlClass).forEach(axiom ->{
            if(!onlyRange && axiom instanceof OWLObjectPropertyRangeAxiom){
                createConceptFromRangeAxiom(((OWLObjectPropertyRangeAxiom) axiom), depth,ret, owlClass);
            }
            if(axiom instanceof OWLObjectPropertyDomainAxiom){
                createConceptFromDomainAxiom(((OWLObjectPropertyDomainAxiom) axiom), depth,ret, owlClass);
            }
        });
        return ret;
    }

    private OWLClassExpression pruneUnionExpr(OWLClassExpression propRange) {
        List<OWLClass> classes = new ArrayList<>();
        if(propRange instanceof OWLObjectUnionOf){
            ((OWLObjectUnionOf)propRange).classesInSignature().filter(x-> allowedTypes.contains(x.getIRI().toString())).forEach(type ->{
                classes.add(type);
            });
            if(classes.isEmpty()){
                return null;
            }
            return dataFactory.getOWLObjectUnionOf(classes);

        }
        return propRange;
    }

    private Collection<OWLClass> getClassesForProperty(OWLObjectPropertyExpression prop) {
        Collection<OWLClass> ret = new HashSet<OWLClass>();
        onto.getReferencingAxioms(prop.getNamedProperty()).forEach(axiom ->{
            if(axiom instanceof OWLObjectPropertyRangeAxiom){
                ret.addAll(axiom.getClassesInSignature());
            }
        });
        return ret;
    }

    /**
     * Get cardinalities int [ ].
     *
     * @return the int [ ]
     */
    protected int[] getCardinalities(){
        return null;
    }
}
