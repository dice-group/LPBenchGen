package org.dice_group.LPBenchGen.dl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.dice_group.LPBenchGen.sparql.IndividualRetriever;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectComplementOfImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectIntersectionOfImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectSomeValuesFromImpl;
import org.semanticweb.owlapi.reasoner.Node;

import java.util.*;

/**
 * Creating concepts based upon just a TBox
 */
public class OWLTBoxPositiveCreator implements OWLTBoxConceptCreator{

    private final List<String> originalTypes;
    private final Reasoner res;
    private IndividualRetriever retriever;
    private OWLOntology onto;
    public List<String> allowedTypes;
    private Parser parser;
    private OWLDataFactory dataFactory = new OWLDataFactoryImpl();
    private Map<String, Set<OWLAxiom>> type2axioms = new HashMap<String, Set<OWLAxiom>>();
    private Map<String, Collection<OWLClassExpression>> type2expr = new HashMap<String, Collection<OWLClassExpression>>();

    public int maxDepth=2;
    public int maxConceptLength=8;
    public int minConceptLength=4;
    public boolean inferDirectSuperClasses=true;

    public OWLTBoxPositiveCreator(IndividualRetriever retriever, OWLOntology onto, List<String> allowedTypes, Parser parser, Reasoner res){
        this.retriever=retriever;
        this.onto=onto;
        this.originalTypes = allowedTypes;
        this.allowedTypes=new ArrayList<String>(new HashSet<String>(allowedTypes));
        this.parser=parser;
        this.res=res;
    }

    public Collection<String> createDistinctConcepts(int noOfConcepts){
        Set<String> ret = new HashSet<String>();
        int toSmallCount=0;
        int noResults=0;
        for(OWLClassExpression concept : createConcepts(noOfConcepts)){

            if(getConceptLength(concept)<minConceptLength){
                toSmallCount++;
                continue;
            }
            if(!retriever.retrieveIndividualsForConcept(concept, 1, 1).isEmpty()) {

                ret.add(parser.render(concept));
                if(ret.size()>=noOfConcepts){
                    break;
                }
            }else{
                noResults++;
            }

        }
        List<String> ret2= new ArrayList<String>(ret);
        //keepRandom(ret2, noOfConcepts);
        System.out.println("Final "+ret2.size()+" concepts. ["+toSmallCount+" to small, "+noResults+" with no results/timeout]");
        return ret2;
    }

    protected Double getConceptLength(OWLClassExpression concept) {
        ConceptLengthCalculator renderer = new ConceptLengthCalculator();
        renderer.render(concept);
        return 1.0*renderer.conceptLength;
    }

    public Collection<OWLClassExpression> createConcepts(int noOfConcepts){
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
        System.out.println("Found "+concepts.size()+" theoretically possible concepts.");
        return concepts;
    }

    private Set<OWLAxiom> getAxiomsForClass(OWLClass owlClass) {

        Set<OWLAxiom> ret = new HashSet<OWLAxiom>();
        if(inferDirectSuperClasses && originalTypes.contains(owlClass.getIRI().toString())) {
            res.getSuperClasses(owlClass, true).forEach(node -> {
                node.getEntities().forEach(superClass -> {
                    onto.getReferencingAxioms(superClass).stream().filter(x -> x instanceof OWLObjectPropertyRangeAxiom || x instanceof OWLObjectPropertyDomainAxiom).forEach(
                            supClassAxiom -> {
                                ret.add(supClassAxiom);
                                String str = superClass.getIRI().toString();
                                if (!allowedTypes.contains(str))
                                    allowedTypes.add(str);

                            }
                    );
                    //TODO
                });
            });
        }
        ret.addAll(onto.getReferencingAxioms(owlClass));
        return ret;
    }

    private void keepRandom(List<String> concepts, int noOfConcepts) {
        Random rand =new Random();
        while(concepts.size()>noOfConcepts){
            concepts.remove(rand.nextInt(concepts.size()));
        }
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

    private Collection<OWLClassExpression> createSingleClassConcepts(Set<OWLAxiom> axioms, OWLClass current) {

        Collection<OWLClassExpression> ret = new ArrayList<OWLClassExpression>();

        axioms.forEach(axiom ->{
            ret.addAll(createConceptFromAxiom(axiom, Sets.newHashSet()));
        });
        return ret;
    }

    private Collection<OWLClassExpression> createConceptsFromClass(OWLClass owlClass, boolean onlyRange, int depth){
        Collection<OWLClassExpression> ret = new ArrayList<OWLClassExpression>();
        ret.add(owlClass);
        /*
    TODO:
        for class   -> get axioms class=domain -> C + C and (Vr.D)
                    -> get axioms class=range -> Vr.C + Vr.(C and (Vr2.D))
     */
        Set<OWLAxiom> rangeAxioms = new HashSet<OWLAxiom>();
        Set<OWLAxiom> domainAxioms = new HashSet<OWLAxiom>();
        getAxiomsForClass(owlClass).forEach(axiom ->{
            if(!onlyRange && axiom instanceof OWLObjectPropertyRangeAxiom){
                rangeAxioms.add(axiom);
            }
            if((axiom instanceof OWLDataPropertyDomainAxiom || axiom instanceof OWLObjectPropertyDomainAxiom)){
                domainAxioms.add(axiom);
            }
        });
        for(OWLAxiom ax : domainAxioms){
            if(ax instanceof  OWLObjectPropertyDomainAxiom){
                OWLObjectPropertyExpression prop = ((OWLObjectPropertyDomainAxiom)ax).getProperty();
                Collection<OWLClass> propClasses = getClassesForProperty(prop);
                OWLClassExpression owlClass2 = ((OWLObjectPropertyDomainAxiom) ax).getDomain();
                propClasses.remove(owlClass2);
                propClasses.remove(owlClass);
                propClasses.forEach(propClass ->{
                    if(allowedTypes.contains(propClass.getIRI().toString())) {
                        OWLClassExpression propRange = new OWLObjectSomeValuesFromImpl(prop, propClass);
                        OWLClassExpression expr = new OWLObjectIntersectionOfImpl(Lists.newArrayList(owlClass, propRange));
                        //if (!retriever.retrieveIndividualsForConcept(expr, 1).isEmpty()) {

                            ret.add(expr);

                            if (depth < maxDepth) {

                                createConceptsFromClass(propClass, false, depth + 1).forEach(concept -> {
                                    //OWLClassExpression iexpr = new OWLObjectIntersectionOfImpl(Lists.newArrayList(owlClass, concept));
                                    OWLClassExpression pexpr = new OWLObjectIntersectionOfImpl(Lists.newArrayList(owlClass2, new
                                            OWLObjectSomeValuesFromImpl(prop, concept)));
                                    if (getConceptLength(pexpr) < maxConceptLength) {
                                        OWLClassExpression newExpr = new OWLObjectIntersectionOfImpl(Lists.newArrayList(pexpr, concept));
                                            ret.add(newExpr);

                                    }
                                });
                            }
                        //}
                    }
                });
            }
        }
        //TODO check if onlyRange makes sense
        for(OWLAxiom ax : rangeAxioms) {
            if (ax instanceof OWLObjectPropertyRangeAxiom) {

                OWLObjectPropertyExpression prop = ((OWLObjectPropertyRangeAxiom)ax).getProperty();
                OWLClassExpression owlClass2 = ((OWLObjectPropertyRangeAxiom) ax).getRange();

                OWLClassExpression classRange = new OWLObjectSomeValuesFromImpl(prop, owlClass2);
                ret.add(classRange);
                Collection<OWLClass> propClasses = getClassesForProperty(prop);
                propClasses.remove(owlClass2);
                propClasses.remove(owlClass);

                propClasses.forEach(propClass ->{
                    if(allowedTypes.contains(propClass.getIRI().toString())) {
                        OWLClassExpression propRange = new OWLObjectSomeValuesFromImpl(prop, propClass);
                        OWLClassExpression expr = new OWLObjectIntersectionOfImpl(Lists.newArrayList(classRange, propRange));
                        //if (!retriever.retrieveIndividualsForConcept(expr, 1).isEmpty()) {

                            ret.add(expr);
                            if (depth < maxDepth) {
                                //TODO check if Intersection already in
                                createConceptsFromClass(propClass, false, depth + 1).forEach(concept -> {
                                    OWLClassExpression newExpr = new OWLObjectIntersectionOfImpl(Lists.newArrayList(expr, concept));
                                        ret.add(newExpr);

                                });
                            }
                       //}
                    }
                });
            }
        }
        ///TODO rangeAxioms
        //get all axioms which are domain, which are range
        return ret;
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


    private Collection<OWLClassExpression> createConceptFromAxiom(OWLAxiom axiom, Collection<OWLAxiom> current){
        Collection<OWLClassExpression> ret = new ArrayList<OWLClassExpression>();
        current.add(axiom);
        OWLClassExpression expr = getBaseExpression(axiom);
        //TODO only add if types are allowed
        ret.addAll(axiom.getNestedClassExpressions());
        ret.add(expr);
        if(expr==null){
            return ret;
        }
        //TODO not getClassesInSignature -> get Range Types if Axiom is range Property!
        //TODO rewrite completely
        Set<OWLClass> classesToHandle = axiom.getClassesInSignature();
        //classesToHandle.removeAll(current);
        classesToHandle.forEach(owlClass -> {
            Collection<OWLAxiom> axioms = getAxioms(owlClass);
            axioms.removeAll(current);
            for (OWLAxiom axiom2 : axioms) {
                for(OWLClassExpression expr2 : createConceptFromAxiom(axiom2, current)) {
                   OWLClassExpression expressions = new OWLObjectIntersectionOfImpl(Lists.newArrayList(expr, expr2));
                   try {
                       ret.add(expressions.getNNF());
                   }catch(Exception e){
                       e.printStackTrace();
                   }
                }
            }
        });
        return ret;
    }

    private OWLClassExpression getBaseExpression(OWLAxiom axiom) {
        //TODO
        OWLAxiomVisitorBase visitor = new OWLAxiomVisitorBase();
        axiom.accept(visitor);
        //if axiom is type -> return OWLClass
        //otherwise return dataPropertyExpr/objectPropertyExpr
        return visitor.getExpression();
    }

    private Collection<OWLAxiom> getAxioms(OWLClass owlClass){
        Collection<OWLAxiom> ret = new ArrayList<OWLAxiom>();
        Set<OWLAxiom> axioms = getAxiomsForClass(owlClass);
        axioms.forEach(axiom ->{
            if(checkAllowedAxiom(axiom)){
                ret.add(axiom);
            }
        });
        return ret;
    }

    private boolean checkAllowedAxiom(OWLAxiom axiom) {
        for(OWLClass owlClass : axiom.getClassesInSignature()){
            if(!allowedTypes.contains(owlClass.getIRI().toString())){
                return false;
            }
        }
        return true;
    }

    protected int[] getCardinalities(){
        return null;
    }
}
