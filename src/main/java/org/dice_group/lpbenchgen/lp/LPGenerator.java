package org.dice_group.lpbenchgen.lp;

import com.google.common.collect.Lists;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.dice_group.lpbenchgen.config.Configuration;
import org.dice_group.lpbenchgen.config.PosNegExample;
import org.dice_group.lpbenchgen.dl.*;
import org.dice_group.lpbenchgen.dl.visitors.ConceptSubClassesExchanger;
import org.dice_group.lpbenchgen.sparql.IndividualRetriever;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectIntersectionOfImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectUnionOfImpl;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * The type Lp generator.
 *
 * @author Lixi Alié Conrads
 */
public class LPGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LPGenerator.class.getName());
    public static final String RDF_PREFIX = "https://lpbenchgen.org/";
    public static final String RESOURCE_PREFIX = RDF_PREFIX+"resource/";
    public static final String PROPERTY_PREFIX = RDF_PREFIX+"property/";

    public static final String RDF_PREFIX_LP = RESOURCE_PREFIX+"lp_";
    public static final Resource LEARNING_PROBLEM_CLASS = ResourceFactory.createResource(RDF_PREFIX+"class/LearningProblem");
    public static final Property RDF_PROPERTY_INCLUDE = ResourceFactory.createProperty(PROPERTY_PREFIX+"includesResource");
    public static final Property RDF_PROPERTY_EXCLUDE = ResourceFactory.createProperty(PROPERTY_PREFIX+"excludesResource");


    private IndividualRetriever retriever;
    private long seed=0;
    private int minExamples;
    private int maxExamples;
    private int positiveLimit;
    private int negativeLimit;
    private Parser parser;
    private List<String> types;
    private OWLClassExpression typesExpr;
    private Reasoner res;
    private boolean strict;
    private int maxLateralDepth;
    private Double negationMutationRatio;

    private void init(Configuration conf) throws OWLOntologyCreationException {
        parser = new Parser(conf.getOwlFile());
        org.semanticweb.HermiT.Configuration conf2 = new org.semanticweb.HermiT.Configuration();
        conf2.ignoreUnsupportedDatatypes=true;
        res = new Reasoner(conf2, parser.getOntology());
        //debugAdd();
        retriever = new IndividualRetriever(conf.getEndpoint());
        seed= conf.getSeed();
        negationMutationRatio = conf.getNegationMutationRatio();
        types = conf.getTypes();
        if(types==null || types.isEmpty()){
            types=new ArrayList<>();
            Set<OWLClass> classes = parser.getOntology().getClassesInSignature();
            classes.forEach(cl -> {
                if(!(cl.getIRI().toString().startsWith("http://www.w3.org/2002/07/owl#") ||
                        cl.getIRI().toString().startsWith("http://www.w3.org/1999/02/22-rdf-syntax-ns#") ||
                        cl.getIRI().toString().startsWith("http://www.w3.org/2000/01/rdf-schema#"))) {
                    types.add(cl.getIRI().toString());
                }
            });
        }
        minExamples=conf.getMinNoOfExamples();
        maxExamples=conf.getMaxNoOfExamples();
        positiveLimit = conf.getPositiveLimit();
        negativeLimit = conf.getNegativeLimit();
        maxLateralDepth = conf.getMaxLateralDepth();
        strict = conf.isStrict();
    }

    private Collection<LPProblem> generateProblems(List<PosNegExample> concepts){
        Collection<LPProblem> problems = new ArrayList<LPProblem>();
        int count=0;
        for(PosNegExample concept : concepts){
            boolean negativeGenerated=concept.isNegativeGenerated();
            try {
                problems.add(generateLPProblem(concept, parser, negativeGenerated));
                count++;
                LOGGER.info("Finished generating examples for {}/{} problem.", count, concepts.size());
            }catch(Exception e ){
                LOGGER.error("Could not generate problem. ", e);
            }
        }
        return problems;
    }

    private Reasoner createReasoner(){
        org.semanticweb.HermiT.Configuration confRes = new org.semanticweb.HermiT.Configuration();
        confRes.ignoreUnsupportedDatatypes=true;
        confRes.throwInconsistentOntologyException=false;
        return new Reasoner(confRes, parser.getOntology());
    }

    private void validateProblems(Collection<LPProblem> problems, boolean isEndpointInfersRules, double percOfPositiveExamples, double percOfNegativeExamples){
        Reasoner res = createReasoner();

        for(LPProblem problem : problems) {
            if(!isEndpointInfersRules) {
                measure(problem, parser.getOntology(), res);
            }
            if(percOfPositiveExamples<1.0 || percOfNegativeExamples<1.0)
                cutProblems(problem, percOfPositiveExamples, percOfNegativeExamples);

        }
    }


    public void createConceptBenchmark(Configuration conf, Collection<LPProblem> problems, String name, String format) throws IOException, OWLOntologyCreationException {

        //for all individuals in LP problem add them
        LOGGER.info("Filling ABox now.");
        ABoxFiller filler = new ABoxFiller(retriever, types);
        fillAbox(filler, problems,conf.getMaxIndividualsPerExampleConcept());
        LOGGER.info("Filled ABox.");

        if(conf.isRemoveLiterals()){
            LOGGER.info("Removing Literals from Ontology now.");
            removeLiteralsFromOntology();
        }
        LOGGER.info("Finished generation, saving now...");
        saveOntology(name+"-ontology.ttl", parser.getOntology());
        LOGGER.info("Validating problems now using reasoner.");
        validateProblems(problems, conf.isEndpointInfersRules(), conf.getPercentageOfPositiveExamples(), conf.getPercentageOfNegativeExamples());
        LOGGER.info("Ontology has now {} Individuals", parser.getOntology().getIndividualsInSignature().size());

        if(format.equals("json")) {
            saveLPProblem(name+"-lp.json", problems);
        }
        else{
            saveLPProblemAsRDF(name+"-lp.ttl", problems, true);
        }
        LOGGER.info("Benchmark generation done.");
    }

    /**
     * Create benchmark. TODO: return Benchmark object, save that
     *
     * @param configFile the config file
     * @param name       the name
     * @throws IOException                  the io exception
     * @throws OWLOntologyCreationException the owl ontology creation exception
     */
    public void createBenchmark(String configFile, String name, String benchmarkType, String format) throws IOException, OWLOntologyCreationException {
        Configuration conf = Configuration.loadFromFile(configFile);
        init(conf);

        if(conf.getConcepts()==null) {
            LOGGER.info("Generating concepts now...");
            conf.setConcepts(generateConcepts(conf.getMaxGenerateConcepts(), conf.getMinConceptLength(), conf.getMaxConceptLength(), conf.getMaxDepth(), conf.getInferDirectSuperClasses(), conf.getNamespace()));

            /*
            if(concept.getNegatives() == null || concept.getNegatives().size()==0){
                LOGGER.info("Generating negative concepts for {}th problem.", count+1);
                concept.setNegatives(generateNegativeConcepts(concept.getPositive()));
                LOGGER.info("Generated {} concepts for {}th problem.",concept.getNegatives().size(), count+1);
                negativeGenerated=true;
            }*/
            LOGGER.info("Generated {} positive concepts.", conf.getConcepts().size());
        }
        createTypesExpr();

        LOGGER.info("Starting to generate examples from concepts");
        Collection<LPProblem> problems = generateProblems(conf.getConcepts());
        if(benchmarkType.toLowerCase().equals("concept")){
            createConceptBenchmark(conf, problems, name, format);
        }
        else if(benchmarkType.toLowerCase().equals("containment")){
            createContainmentBenchmark(conf, problems, name, format);
        }


    }

    public void createContainmentBenchmark(Configuration conf, Collection<LPProblem> problems, String name, String format){
        List<LPProblem> train = new ArrayList<LPProblem>();
        List<LPProblem> gold = new ArrayList<LPProblem>();
        List<LPProblem> test = new ArrayList<LPProblem>();
        Double split = conf.getSplitContainment();
        problems = problems.stream().filter(problem -> problem.negatives.size()>0).collect(Collectors.toList());
        Double trainSize = split*problems.size();
        AtomicInteger count = new AtomicInteger();
        problems.stream().forEach(problem -> {
            if(count.get() <trainSize){
                train.add(problem);
            }
            else{
                gold.add(problem);
                test.add(problem);
            }
            count.getAndIncrement();
        });
        validateProblems(train, true, 1.0, conf.getPercentageOfNegativeExamples());
        validateProblems(gold, true, 1.0, 1.0);

        if(format.equals("json")) {
            saveLPProblem(name+"-train.json", train);
            saveLPProblem(name+"-test-goldstd.json", gold);
            validateProblems(test, true, conf.getPercentageOfPositiveExamples(), conf.getPercentageOfNegativeExamples());
            saveLPProblem(name+"-test.json", test);
        }
        else{
            saveLPProblemAsRDF(name+"-train.ttl", train, true);
            saveLPProblemAsRDF(name+"-test-goldstd.ttl", gold, false);
            validateProblems(test, true, conf.getPercentageOfPositiveExamples(), conf.getPercentageOfNegativeExamples());
            saveLPProblemAsRDF(name+"-test.ttl", test, true);
        }

    }

    private void saveLPProblemAsRDF(String out, Collection<LPProblem> problems, boolean includeNegative) {
        Model m = ModelFactory.createDefaultModel();
        m.setNsPrefix("lpres", RDF_PREFIX+"resource/");
        m.setNsPrefix("lpprop", RDF_PREFIX+"property/");
        m.setNsPrefix("lpclass", RDF_PREFIX+"class/");
        for(String prefix : parser.getPrefix().getPrefixName2PrefixMap().keySet()){
            if(!prefix.equals(":")){
                m.setNsPrefix(prefix.replace(":",""), parser.getPrefix().getPrefixName2PrefixMap().get(prefix));
            }
        }
        AtomicInteger count= new AtomicInteger(1);
        problems.forEach(problem -> {
            System.out.println(count.get()+"\t"+problem.goldStandardConcept);
            addProblemToModel(m, problem, count.getAndIncrement(), includeNegative);

        });
        try(BufferedOutputStream bos  = new BufferedOutputStream(new FileOutputStream(out))) {
            RDFDataMgr.write(bos, m, Lang.TTL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addProblemToModel(Model m, LPProblem problem, int id, boolean includeNegative) {
        /*
          :lp_1 a :LearningProblem;
          :includesResource :res_1  , :res_2
          :excludesResource :res_1  , :res_3 , :res_4 .
         */
        Resource res = ResourceFactory.createResource(RDF_PREFIX_LP+id);
        m.add(res, RDF.type, LEARNING_PROBLEM_CLASS);
        problem.positives.forEach(include ->{
            m.add(res, RDF_PROPERTY_INCLUDE, ResourceFactory.createResource(include));
        });
        if(includeNegative) {
            problem.negatives.forEach(exclude -> {
                m.add(res, RDF_PROPERTY_EXCLUDE, ResourceFactory.createResource(exclude));
            });
        }
    }

    private void createTypesExpr() {
        List<OWLClassExpression> classes = new ArrayList<OWLClassExpression>();
        for(String type: types){
            classes.add(new OWLDataFactoryImpl().getOWLClass(IRI.create(type)));
        }
        typesExpr = new OWLObjectUnionOfImpl(classes);
    }

    private void measure(LPProblem problem, OWLOntology ontology, Reasoner res) {
        LOGGER.info("Checking concept {}.",problem.goldStandardConcept);

        OWLDataFactory factory = new OWLDataFactoryImpl();

        Set<String> rem = new HashSet<>();
        Set<String> actual = res.getInstances(problem.goldStandardConceptExpr).getFlattened().stream().map(x -> x.getIRI().toString()).collect(Collectors.toSet());
        for(String pos : problem.positives){
            if(!actual.contains(pos)){
            //if(!res.hasType(factory.getOWLNamedIndividual(pos), problem.goldStandardConceptExpr, false)){
                LOGGER.warn("FP: {}", pos);
                rem.add(pos);
            }
        }
        problem.positives.removeAll(rem);
        rem.clear();
        for(String nes : problem.negatives){
            if(actual.contains(nes)){
            //if(res.hasType(factory.getOWLNamedIndividual(nes), problem.goldStandardConceptExpr, false)){
                LOGGER.warn("FN: {}", nes);
                LOGGER.warn("Negative concept: {}", parser.render(problem.negativeMap.get(nes).getNNF()));
                rem.add(nes);
            }
        }
        problem.negatives.removeAll(rem);
    }

    private AtomicReference<OWLClassExpression>  fillNegativeConcept(OWLClassExpression negativeExpr, boolean isNegativeGenerated){
        AtomicReference<OWLClassExpression> actNegExa = new AtomicReference<>(negativeExpr);
        if(isNegativeGenerated) {
            negativeExpr.components().forEach(x->{
                if(x instanceof List){
                    ((List)x).forEach(e->{

                        if (!((OWLClassExpression)e).equals(typesExpr)) {
                            actNegExa.set((OWLClassExpression) e);
                        }
                    });

                }else {
                    if (!x.equals(typesExpr)) {
                        actNegExa.set((OWLClassExpression) x);
                    }
                }
            });
        }
        return actNegExa;
    }

    private void fillAboxForProblem(LPProblem problem, ABoxFiller filler, Integer maxIndividualsPerExampleConcept){
        problem.positives = getRandom(new ArrayList<>(problem.positives), maxIndividualsPerExampleConcept);
        problem.negatives = getRandom(new ArrayList<>(problem.negatives), maxIndividualsPerExampleConcept);
        ArrayList<String> rem = new ArrayList<>();
        problem.positives.forEach(pos ->{
            if(!filler.addIndividualsFromConcept(problem.goldStandardConceptAsExpr(), pos, parser.getOntology())){
                rem.add(pos);
            }
        });
        if(!rem.isEmpty()) {
            LOGGER.info("Errors occured at individuals, removing {} from positives.", rem);
            problem.positives.removeAll(rem);
        }
        rem.clear();
        problem.negatives.forEach(nes ->{
            OWLClassExpression negativeExpr = problem.getExpr(nes);
            //AtomicReference<OWLClassExpression> actNegExa = fillNegativeConcept(negativeExpr, problem.negativeGenerated);
            if(!filler.addIndividualsFromConcept(negativeExpr, nes, parser.getOntology())){
                rem.add(nes);
            }
        });
        if(!rem.isEmpty()) {
            LOGGER.info("Errors occured at individuals, removing {} from negatives.", rem);
            problem.negatives.removeAll(rem);
            rem.clear();
        }
    }

    private void fillAbox(ABoxFiller filler, Collection<LPProblem> problems, Integer maxIndividualsPerExampleConcept) {
        AtomicInteger count= new AtomicInteger();
        AtomicInteger emptyCount= new AtomicInteger();
        AtomicLong size = new AtomicLong(problems.size());
        problems.forEach(problem ->{
            fillAboxForProblem(problem, filler, maxIndividualsPerExampleConcept);
            count.getAndIncrement();
            if(problem.positives.isEmpty() || problem.negatives.isEmpty()){
                emptyCount.getAndIncrement();
                size.getAndDecrement();
            }
            LOGGER.info("Filled Abox for problem {}/{} [{} empty]", count, size, emptyCount.get());
        });
    }

    private List<PosNegExample> generateConcepts(Integer maxGenerateConcepts, Integer minConceptLength, Integer maxConceptLength, Integer maxDepth, boolean inferDirectSuperClasses, String namespace) {
        List<PosNegExample> examples =new ArrayList<PosNegExample>();

        OWLTBoxPositiveCreator creator = new OWLTBoxPositiveCreator(retriever, parser.getOntology(), types, parser, res, namespace, strict, minExamples);
        creator.minConceptLength=minConceptLength;
        creator.maxConceptLength=maxConceptLength;
        creator.maxDepth=maxDepth;
        creator.seed=seed;
        creator.negationMutationRatio=negationMutationRatio;
        creator.maxLateralDepth=maxLateralDepth;
        creator.inferDirectSuperClasses=inferDirectSuperClasses;
        creator.createDistinctConcepts(maxGenerateConcepts).forEach(example -> {
            examples.add(example);
        });
        this.types=creator.allowedTypes;
        return examples;
    }

    private List<OWLClassExpression> generateNegativeConcepts(String positiveConcept){
        //concept -> Expr -> visit -> negations
        OWLClassExpression expr = parser.parseManchesterConcept(positiveConcept);
        OWLNegationCreator creator = new OWLNegationCreator();
        expr.accept(creator);
        creator.prune();
        //creator.addNeccTypes(types, typesExpr);
        List<OWLClassExpression> concepts= creator.negationConcepts;

        return concepts;
    }

    private void cutProblems(LPProblem problem, Double percentageOfPositiveExamples, Double percentageOfNegativeExamples) {
        keepPercentage(problem.positives, percentageOfPositiveExamples);
        keepPercentage(problem.negatives, percentageOfNegativeExamples);
    }

    private Collection<String> getRandom(List<String> list, Integer max) {
        int maxNoOfIndividuals = Math.min(max, list.size());
        Random rand = new Random(seed);
        Collections.shuffle(list, rand);
        Collection<String> ret = new HashSet<String>();
        int i=0;
        while(ret.size()<maxNoOfIndividuals){
            ret.add(list.get(i++));
        }
        return ret;
    }

    private List<String> keepPercentage(Collection<String> removeFrom, Double percentage) {
        List<String> removed = new ArrayList<String>(removeFrom);
        Double max = removeFrom.size()*percentage;
        if(max<minExamples && !strict){
            max=minExamples*1.0;
        }
        if(max>maxExamples){
            max=maxExamples*1.0;
        }

        Collection<String> keep = getRandom(new ArrayList(removeFrom), max.intValue());
        removed.removeAll(keep);
        removeFrom.removeAll(removed);
        return removed;
    }

    private LPProblem generateLPProblem(PosNegExample concept, Parser parser, boolean negativeGenerated) {
        LPProblem problem = new LPProblem();
        problem.negativeGenerated=negativeGenerated;
        problem.goldStandardConcept=concept.getPositive();

        OWLClassExpression pos = parser.parseManchesterConcept(concept.getPositive());
        //pos = transformConceptSubClasses(pos, "OR", false);
        problem.goldStandardConceptExpr=pos;
        problem.rules = parser.getRulesInExpr(pos, problem.dataRules);

        problem.positives.addAll(retriever.retrieveIndividualsForConcept(pos, positiveLimit,180, true));
        for(OWLClassExpression neg : concept.getNegatives()){
            OWLClassExpression conc = neg;
            if(negativeGenerated){
                //conc = transformConceptSubClasses(neg, "OR", true);
            }

            List<String> retrieved = retriever.retrieveIndividualsForConcept(conc,  negativeLimit, 180, true);
            problem.negatives.addAll(retrieved);
            problem.rules.addAll(parser.getRulesInExpr(neg, problem.dataRules));
            OWLClassExpression finalConc = conc;
            retrieved.forEach(retr -> {
                problem.negativeMap.put(retr, finalConc);
            });
        }
        List<String> negativeShuffle = new ArrayList<>(problem.negatives);
        Collections.shuffle(negativeShuffle, new Random(seed));
        problem.negatives=new HashSet<String>(negativeShuffle);
        return problem;
    }

    private OWLClassExpression transformConceptSubClasses(OWLClassExpression neg, String combiner, boolean rem) {
        OWLClassExpression expr =neg;
        if(rem){
             expr = removeExpr(neg);
        }
        String conceptString = parser.render(expr);

        for(String type : types){
            ConceptSubClassesExchanger exchanger = new ConceptSubClassesExchanger();
            exchanger.replace=type;
            exchanger.replacer=parser.parseManchesterConcept(getUnionSubClasses(type, combiner).toString());
            expr.accept(exchanger);
        }
        return new OWLObjectIntersectionOfImpl(Lists.newArrayList(expr, typesExpr));
    }

    private OWLClassExpression removeExpr(OWLClassExpression neg) {
        AtomicReference<OWLClassExpression> actNegExa = new AtomicReference<>(neg);
            neg.components().forEach(x->{
                if(x instanceof List){
                    ((List)x).forEach(e->{
                        if (!((OWLClassExpression)e).equals(typesExpr)) {
                            actNegExa.set((OWLClassExpression) e);
                        }
                    });

                }else {
                    if (!x.equals(typesExpr)) {
                        actNegExa.set((OWLClassExpression) x);
                    }
                }
            });
            return actNegExa.get();

    }

    private CharSequence getUnionSubClasses(String type, String combiner) {
        OWLDataFactory factory = new OWLDataFactoryImpl();
        StringBuilder builder = new StringBuilder("( ").append(parser.getShortName(type)).append(" OR ");
        AtomicInteger count= new AtomicInteger();
        for(OWLClass subClass : res.getSubClasses(factory.getOWLClass(type)).getFlattened()){
            if(types.contains(subClass.getIRI().toString())){
                builder.append(parser.getShortName(subClass.getIRI().toString()));
                builder.append(" ").append(combiner).append(" ");
                count.getAndIncrement();
            }
        }

        if(count.get() ==0) {
            return parser.getShortName(type);
        }
        return builder.subSequence(0, builder.length() - 4) + ")";
    }

    /**
     * Add individuals.
     *
     * @param ontology    the ontology
     * @param individuals the individuals
     */
    public void addIndividuals(OWLOntology ontology, Collection<OWLAxiom> individuals){
        for(OWLAxiom axiom : individuals){
            ontology.addAxiom(axiom);
        }
    }

    /**
     * Save ontology.
     *
     * @param output   the output
     * @param ontology the ontology
     */
    public void saveOntology(String output, OWLOntology ontology){
        try(FileOutputStream fos = new FileOutputStream(output))   {
            ontology.saveOntology(new TurtleDocumentFormat(), fos);
        } catch (IOException | OWLOntologyStorageException e) {
            LOGGER.error("Could not store ontology. ", e);
        }

    }

    /**
     * Save lp problem.
     *
     * @param output   the output
     * @param problems the problems
     */
    public void saveLPProblem(String output, Collection<LPProblem> problems){
        try(PrintWriter pw = new PrintWriter(output)){
            // print end
            pw.print("[\n");
            AtomicInteger count= new AtomicInteger();
            long size= problems.stream().filter(x -> x.positives.size()>0 && x.negatives.size()>0).count();
            problems.stream().filter(x -> x.positives.size()>0 && x.negatives.size()>0).forEach(problem -> {
                if(problem.positives.size()>0 && problem.negatives.size()>0) {
                    pw.print("\t{\n\t\"concept\": \"");
                    pw.print(problem.goldStandardConcept.replace("\n", " "));
                    pw.print("\",\n\t\"positives\": [\n");
                    writeCollection(problem.positives, pw);
                    pw.print("\n\t],\n\t\"negatives\": [\n");
                    writeCollection(problem.negatives, pw);
                    pw.print("\n\t]\n\t}");
                    count.getAndIncrement();
                    if (count.get() < size) {
                        pw.print(",\n");
                    }
                }
            });
            pw.print("\n]");
        } catch (FileNotFoundException e) {
            LOGGER.error("Could not write LP Problem as file was not found.", e);
        }
    }

    private void writeCollection(Collection<String> collection, PrintWriter pw) {
        int count=0;
        for(String uri : collection){
            pw.print("\t\t\"");
            pw.print(uri);
            pw.print("\"");
            count++;
            if(count<collection.size()){
                pw.print(",\n");
            }

        }
    }


    /**
     * Remove literals from ontology.
     */
    public void removeLiteralsFromOntology(){
        OWLOntology ontology = this.parser.getOntology();
        List<OWLDataPropertyAxiom> rem = new ArrayList<OWLDataPropertyAxiom>();
        ontology.getAxioms().stream().filter(x -> x instanceof OWLDataPropertyAxiom).forEach(ax ->{
            OWLDataPropertyAxiom axiom = (OWLDataPropertyAxiom) ax;
            rem.add(axiom);
        });
        ontology.remove(rem);
    }
}
