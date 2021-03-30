package org.dice_group.LPBenchGen.cleaner;

import com.google.common.collect.Sets;
import org.apache.jena.query.*;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class OntologyInconsistentFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(OntologyInconsistentFinder.class.getName());

    //TODO if ?s has no type, then thats fine. {?s PROP ?o. FILTER(NOT EXISTS(?s a ?type))} UNION {?s PROP ?o. FILTER(NOT EXISTS(?o a ?type))} UNION
    //DO not say incosistent, but saying there are individuals who are not in any case directly of such a class.
    private static final String DOMAIN_PROP_QUERY = " { {?s PROP ?o . PROP rdfs:domain ?domain. ?s a ?type . ?type rdfs:subClassOf* ?supertype. FILTER(?supertype != ?domain && ?type != ?domain) }}";
    private static final String RANGE_PROP_QUERY = " {{ ?s PROP ?o . PROP rdfs:range ?range. ?o a ?type . ?type rdfs:subClassOf* ?supertype. FILTER(?supertype != ?range && ?type != ?range) }}";

    private static final String DOMAIN_TYPES_BGP = " { ?s <PROP> ?o ; a ?type . ?type rdfs:subClassOf* ?supertype } ";
    private static final String RANGE_TYPES_BGP = " { ?s <PROP> ?o . ?o a ?type . ?type rdfs:subClassOf* ?supertype } ";

    //checks if an I is directly inconsistent, interesting for Person and Country
    private static final String DISJOINT_CLASSES = " { {?s a TYPE1, TYPE2} UNION {?s a ?subType1. ?subType1 rdfs:subClassOf* TYPE1 . ?s a ?subType2. ?subType2 rdfs:subClassOf* TYPE2 .  } }";

    //TODO checks if an Ind has a class and a disjoint can be inferred from a property
    private static final String DOMAIN_PROP_QUERY_DISJOINT_CLASSES
            = " {{?s ?prop ?o . ?s a <TYPE1>. {{?prop rdfs:domain <TYPE2>} UNION {?prop rdfs:domain ?subType2. ?subType2 rdfs:subClassOf* <TYPE2>}} }" +
            " UNION {?s ?prop ?o . ?s a ?subType1. ?subType1 rdfs:subClassOf* <TYPE1> . " +
            " {{?prop rdfs:domain <TYPE2>} UNION {?prop rdfs:domain ?subType2. ?subType2 rdfs:subClassOf* <TYPE2>}} } }";
    private static final String RANGE_PROP_QUERY_DISJOINT_CLASSES
            = " {{?s ?prop ?o . ?o a <TYPE1>. {{?prop rdfs:range <TYPE2>} UNION {?prop rdfs:range ?subType2. ?subType2 rdfs:subClassOf* <TYPE2>}} }" +
            " UNION {?s ?prop ?o . ?s a ?subType1. ?subType1 rdfs:subClassOf* <TYPE1> . " +
            " {{?prop rdfs:range <TYPE2>} UNION {?prop rdfs:range ?subType2. ?subType2 rdfs:subClassOf* <TYPE2>}} } }";

    //checks if disjoint classes can be inferred from properties, interesting for Person and Country
    private static final String PROP_QUERY_DISJOINT_CLASSES_DOMAIN2
            = " {?s ?p1 ?o1 . {{?p1 rdfs:domain <TYPE1>} UNION {?p1 rdfs:domain ?subType2. ?subType2 rdfs:subClassOf* <TYPE1>}}." +
              "  ?s ?p2 ?o2 . {{?p2 rdfs:domain <TYPE2>} UNION {?p2 rdfs:domain ?subType2. ?subType2 rdfs:subClassOf* <TYPE2>}} }";
    private static final String PROP_QUERY_DISJOINT_CLASSES_DOMAIN_RANGE
            = " {?s ?p1 ?o1 . {{?p1 rdfs:domain <TYPE1>} UNION {?p1 rdfs:domain ?subType2. ?subType2 rdfs:subClassOf* <TYPE1>}}." +
            "  ?o2 ?p2 ?s . {{?p2 rdfs:range <TYPE2>} UNION {?p2 rdfs:range ?subType2. ?subType2 rdfs:subClassOf* <TYPE2>}} }";
    private static final String PROP_QUERY_DISJOINT_CLASSES_RANGE2
            = " {?s1 ?p1 ?o . {{?p1 rdfs:range <TYPE1>} UNION {?p1 rdfs:range ?subType2. ?subType2 rdfs:subClassOf* <TYPE1>}}." +
            "  ?s2 ?p2 ?o . {{?p2 rdfs:range <TYPE2>} UNION {?p2 rdfs:range ?subType2. ?subType2 rdfs:subClassOf* <TYPE2>}} }";

    private Reasoner res;
    private OWLDataFactory factory = new OWLDataFactoryImpl();
    private OWLOntology ontology;
    private String endpoint;
    private TreeNode root;
    private Set<String> rangeDone = new HashSet<String>();
    private Set<String> domainDone = new HashSet<String>();

    public static void main(String[] args) throws OWLOntologyCreationException, FileNotFoundException, OWLOntologyStorageException {
        if(args.length==3) {
            OntologyInconsistentFinder finder = new OntologyInconsistentFinder();
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(args[0]));
            LOGGER.info("Trying to find inconsistencies now.");
            finder.printInconsistents(args[1], ontology);
            LOGGER.info("Saving fixed ontology now.");
            ontology.saveOntology(new FileOutputStream(new File(args[3])));
            LOGGER.info("Finished finding and exchanging inconsistencies.");
        }
        else{
            printHelp();
        }
    }

    private static void printHelp() {
        System.out.println("ontologyCleaner INPUT_ONTOLOGY SPARQL_ENDPOINT OUTPUT");
    }

    public void printInconsistents(String endpoint, OWLOntology ontology) throws FileNotFoundException, OWLOntologyStorageException {
        this.endpoint=endpoint;
        Configuration conf = new Configuration();
        this.ontology=ontology;
        conf.ignoreUnsupportedDatatypes=true;
        res = new Reasoner(conf, ontology);
        buildTree();
        long co = ontology.getAxioms().stream().filter(ax -> ax instanceof OWLDisjointClassesAxiom).count();
        AtomicInteger cu = new AtomicInteger();
        ontology.getAxioms().stream().filter(ax -> ax instanceof OWLDisjointClassesAxiom).forEach( axiom -> {
            cu.getAndIncrement();
            System.out.println(cu+"/"+co);
            OWLDisjointClassesAxiom ax = (OWLDisjointClassesAxiom) axiom;
            Set<OWLClass> disjointClasses = ax.getClassesInSignature();
            Iterator<OWLClass> it = disjointClasses.iterator();
            OWLClass cl1 = it.next();
            OWLClass cl2 = it.next();
            //TODO check if there exists an inconsistency for properties -> suggestExchange
            // DOMAIN_PROP_QUERY_DISJOINT_CLASSES
            // RANGE_PROP_QUERY_DISJOINT_CLASSES
            String bgp = DOMAIN_PROP_QUERY_DISJOINT_CLASSES;
            bgp = bgp.replace("TYPE1", cl1.getIRI().toString()).replace("TYPE2", cl2.getIRI().toString());
            getSingleValue("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> SELECT DISTINCT ?prop "+bgp, "prop").forEach(property->{
                if(!domainDone.contains(property)) {
                    suggestExchange(property, false);
                    domainDone.add(property);
                }
            });
            bgp = RANGE_PROP_QUERY_DISJOINT_CLASSES;
            bgp = bgp.replace("TYPE1", cl1.getIRI().toString()).replace("TYPE2", cl2.getIRI().toString());
            getSingleValue("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> SELECT DISTINCT ?prop "+bgp, "prop").forEach(property->{
                if(!rangeDone.contains(property)) {
                    suggestExchange(property, true);
                    rangeDone.add(property);
                }
            });
            bgp = DOMAIN_PROP_QUERY_DISJOINT_CLASSES;
            bgp = bgp.replace("TYPE2", cl1.getIRI().toString()).replace("TYPE1", cl2.getIRI().toString());
            getSingleValue("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> SELECT DISTINCT ?prop "+bgp, "prop").forEach(property->{
                if(!domainDone.contains(property)) {
                    suggestExchange(property, false);
                    domainDone.add(property);
                }
            });
            bgp = RANGE_PROP_QUERY_DISJOINT_CLASSES;
            bgp = bgp.replace("TYPE2", cl1.getIRI().toString()).replace("TYPE1", cl2.getIRI().toString());
            getSingleValue("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> SELECT DISTINCT ?prop "+bgp, "prop").forEach(property->{
                if(!rangeDone.contains(property)) {
                    suggestExchange(property, true);
                    rangeDone.add(property);
                }
            });


            //TODO check all direct inconsistencies,
            // DISJOINT_CLASSES,
            // PROP_QUERY_DISJOINT_CLASSES_DOMAIN2,
            // PROP_QUERY_DISJOINT_CLASSES_DOMAIN_RANGE
            // PROP_QUERY_DISJOINT_CLASSES_RANGE2
            //we cannot do much about them, but it's nice to know.
        });
        //ontology.saveOntology(new FileOutputStream(new File("fixed-ontology.owl")));
    }

    public void buildTree(){
        OWLClass owlThing =factory.getOWLClass("http://www.w3.org/2002/07/owl#Thing");
        Set<OWLClass> subClasses = res.getSubClasses(owlThing, true).getFlattened();
        root = new TreeNode();
        root.reprClass=owlThing.getIRI().toString();
        addDSF(subClasses, root);
    }

    public void addDSF(Set<OWLClass> subClasses, TreeNode current){
        for(OWLClass sCL : subClasses){
            TreeNode node = new TreeNode();
            node.reprClass=sCL.getIRI().toString();
            current.children.add(node);
            Set<OWLClass> subClassesNew = res.getSubClasses(sCL, true).getFlattened();
            addDSF(subClassesNew, node);
        }
    }


    public Set<String> getSingleValue(String queryStr, String var){
        Query q = QueryFactory.create(queryStr);
        QueryExecution qexec = QueryExecutionFactory.sparqlService(endpoint, q);
        //qexec.setTimeout(60000);
        Set<String> ret = new HashSet<String>();

        try {


        ResultSet res = qexec.execSelect();
        while(res.hasNext()){
            QuerySolution sol = res.next();
            ret.add(sol.get(var).toString());
        }
        }catch(Exception e){
            LOGGER.error("Could not get Values for {}", queryStr);
            LOGGER.error("Could not get Values due to", e);
        }
        return ret;
    }

    public void suggestExchange(String property, boolean isRange){
        //get a Class which all Individuals contain. -> worst case owl:Thing
        //For the property -> getMostSpecific Class
        //get all superTypes for property as annoying as it is.
        // Build a tree -> get node which has no neighbours, and maximize(h) -> that is the most speicfic class
        Set<String> classes = getClassesForProperty(property, isRange);
        OWLClassExpression cl = getMostSpecificClass(classes);
        if(isRange){
            LOGGER.info("Suggesting new Range Class for Property {} .", property);
            LOGGER.info("Class : {} .", cl);
            exchangeRangeAxiom(factory.getOWLObjectProperty(IRI.create(property)), cl);

        }else{
            LOGGER.info("Suggesting new Domain Class for Property {} .",property);
            LOGGER.info("Class : {} .", cl);
            exchangeDomainAxiom(factory.getOWLObjectProperty(IRI.create(property)), cl);
        }
    }

    private void exchangeDomainAxiom(OWLObjectProperty owlObjectProperty, OWLClassExpression cl) {
        Set<OWLObjectPropertyDomainAxiom> ax = new HashSet<>();
        ontology.getObjectPropertyDomainAxioms(owlObjectProperty).forEach(x -> {
            ax.add(x);
        });
        ontology.remove(ax);
        OWLObjectPropertyDomainAxiom newRange = factory.getOWLObjectPropertyDomainAxiom(owlObjectProperty, cl);
        ontology.add(newRange);
    }

    private void exchangeRangeAxiom(OWLObjectProperty owlObjectProperty, OWLClassExpression cl) {
        Set<OWLObjectPropertyRangeAxiom> ax = new HashSet<>();
        ontology.getObjectPropertyRangeAxioms(owlObjectProperty).forEach(x -> {
            ax.add(x);
        });
        ontology.remove(ax);
        OWLObjectPropertyRangeAxiom newRange = factory.getOWLObjectPropertyRangeAxiom(owlObjectProperty, cl);
        ontology.add(newRange);
    }


    private Set<String> getClassesForProperty(String property, boolean isRange) {
        String bgp= DOMAIN_TYPES_BGP.replace("PROP", property);

        if(isRange){
            bgp = RANGE_TYPES_BGP.replace("PROP", property);
        }
        String query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> SELECT DISTINCT ?supertype "+bgp;
        Query q = QueryFactory.create(query.toString());
        QueryExecution qexec = QueryExecutionFactory.sparqlService(endpoint, q);
        ResultSet res = qexec.execSelect();
        Set<String> ret = new HashSet<String>();
        while(res.hasNext()){
            QuerySolution sol = res.next();
            ret.add(sol.get("supertype").toString());
        }
        return ret;
    }

    public Set<String> buildTree(Set<String> classes){
        Set<String> paths=null;
        for(String cl : classes){
            Set<String> path = new HashSet<String>();
            res.getSuperClasses(factory.getOWLClass(IRI.create(cl))).getFlattened().forEach(x->{
                path.add(x.getIRI().toString());
            });
            if(paths==null){
                paths = path;
            }
            else {
                paths.removeAll(path);
                if(paths.isEmpty()){
                    return Sets.newHashSet("owl:Thing");
                }
            }
        }
        return paths;
    }

    /**
     * Returns the most specific class of the set.
     *
     * @param classes
     * @return
     */
    public OWLClassExpression getMostSpecificClass(Set<String> classes){
        //TODO group all classes by the
        /*
            T
            SameClass
          C1  C2    C3 <-- use UNION of these
          ....
         */
        //TODO for every node in Tree -> get all Nodes in children. If size>1 -> UNION

        Set<OWLClass> spec = new HashSet<OWLClass>();

            List<TreeNode> nodes = root.children;
            for(TreeNode n : nodes){
                if(classes.contains(n.reprClass)){
                    spec.add(factory.getOWLClass(n.reprClass));
                }
            }


        return factory.getOWLObjectUnionOf(spec);
    }


    public void printDebug(String endpoint, String query, String[] vars){
        StringBuilder select = new StringBuilder("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> SELECT DISTINCT");
        for(String v : vars){
            select.append(" ").append(v).append(" ");
            System.out.print(v+"\t");
        }
        System.out.println("");

        select.append(query);
        Query q = QueryFactory.create(select.toString());
        QueryExecution qexec = QueryExecutionFactory.sparqlService(endpoint, q);
        ResultSet res = qexec.execSelect();

        while(res.hasNext()){
            QuerySolution sol = res.next();
            for(String v : vars) {
                System.out.print(sol.get(v.replace("?", "")).toString()+"\t");
            }
            System.out.println("");
        }
    }
}
