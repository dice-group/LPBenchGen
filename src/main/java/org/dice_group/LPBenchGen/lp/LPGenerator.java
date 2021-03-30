package org.dice_group.LPBenchGen.lp;

import com.google.common.collect.Lists;
import org.dice_group.LPBenchGen.config.Configuration;
import org.dice_group.LPBenchGen.config.PosNegExample;
import org.dice_group.LPBenchGen.dl.ABoxFiller;
import org.dice_group.LPBenchGen.dl.OWLNegationCreator;
import org.dice_group.LPBenchGen.dl.OWLTBoxPositiveCreator;
import org.dice_group.LPBenchGen.dl.Parser;
import org.dice_group.LPBenchGen.dl.visitors.ConceptSubClassesExchanger;
import org.dice_group.LPBenchGen.sparql.IndividualRetriever;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectIntersectionOfImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectUnionOfImpl;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class LPGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LPGenerator.class.getName());

    private IndividualRetriever retriever;
    private long seed=0;
    private int minExamples;
    private int maxExamples;
    private Parser parser;
    private List<String> types;
    private OWLClassExpression typesExpr;
    private Reasoner res;

    private void init(Configuration conf) throws OWLOntologyCreationException {
        parser = new Parser(conf.getOwlFile());
        org.semanticweb.HermiT.Configuration conf2 = new org.semanticweb.HermiT.Configuration();
        conf2.ignoreUnsupportedDatatypes=true;
        res = new Reasoner(conf2, parser.getOntology());
        //debugAdd();
        retriever = new IndividualRetriever(conf.getEndpoint());
        seed= conf.getSeed();
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
    }

    private Collection<LPProblem> generateProblems(List<PosNegExample> concepts){
        Collection<LPProblem> problems = new ArrayList<LPProblem>();
        int count=0;
        for(PosNegExample concept : concepts){
            boolean negativeGenerated=false;
            if(concept.getNegatives() == null || concept.getNegatives().size()==0){
                LOGGER.info("Generating negative concepts for {}th problem.", count+1);
                concept.setNegatives(generateNegativeConcepts(concept.getPositive()));
                LOGGER.info("Generated {} concepts for {}th problem.",concept.getNegatives().size(), count+1);
                negativeGenerated=true;
            }
            try {
                problems.add(generateLPProblem(concept, parser,negativeGenerated));
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
            cutProblems(problem, percOfPositiveExamples, percOfNegativeExamples);

        }
    }

    public void createBenchmark(String configFile, String name) throws IOException, OWLOntologyCreationException {
        Configuration conf = Configuration.loadFromFile(configFile);
        init(conf);

        if(conf.getConcepts()==null) {
            LOGGER.info("Generating concepts now...");
            conf.setConcepts(generateConcepts(conf.getMaxGenerateConcepts(), conf.getMinConceptLength(), conf.getMaxConceptLength(), conf.getMaxDepth(), conf.getInferDirectSuperClasses(), conf.getNamespace()));
            LOGGER.info("Generated {} positive concepts.", conf.getConcepts().size());
        }
        createTypesExpr();

        LOGGER.info("Starting to generate examples from concepts");
        Collection<LPProblem> problems = generateProblems(conf.getConcepts());

        //for all individuals in LP problem add them
        LOGGER.info("Filling ABox now.");
        ABoxFiller filler = new ABoxFiller(retriever, types);
        fillAbox(filler, problems,conf.getMaxIndividualsPerExampleConcept());
        LOGGER.info("Filled ABox.");

        LOGGER.info("Validating problems now using reasoner.");
        validateProblems(problems, conf.isEndpointInfersRules(), conf.getPercentageOfPositiveExamples(), conf.getPercentageOfNegativeExamples());
        LOGGER.info("Ontology has now {} Individuals", parser.getOntology().getIndividualsInSignature().size());

        if(conf.isRemoveLiterals()){
            LOGGER.info("Removing Literals from Ontology now.");
            removeLiteralsFromOntology();
        }
        LOGGER.info("Finished generation, saving now...");
        saveOntology(name+"-ontology.ttl", parser.getOntology());
        saveLPProblem(name+"-lp.json", problems);
        LOGGER.info("Benchmark generation done.");
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
        for(String pos : problem.positives){
            if(!res.hasType(factory.getOWLNamedIndividual(pos), problem.goldStandardConceptExpr, true)){
                LOGGER.warn("FP: {}", pos);
                rem.add(pos);
            }
        }
        problem.positives.removeAll(rem);
        rem.clear();
        for(String nes : problem.negatives){
            if(res.hasType(factory.getOWLNamedIndividual(nes), problem.goldStandardConceptExpr, true)){
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
            AtomicReference<OWLClassExpression> actNegExa = fillNegativeConcept(negativeExpr, problem.negativeGenerated);
            if(!filler.addIndividualsFromConcept(actNegExa.get(), nes, parser.getOntology())){
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

        OWLTBoxPositiveCreator creator = new OWLTBoxPositiveCreator(retriever, parser.getOntology(), types, parser, res, namespace);
        creator.minConceptLength=minConceptLength;
        creator.maxConceptLength=maxConceptLength;
        creator.maxDepth=maxDepth;
        creator.inferDirectSuperClasses=inferDirectSuperClasses;
        creator.createDistinctConcepts(maxGenerateConcepts).forEach(concept -> {
            PosNegExample example = new PosNegExample();
            example.setPositive(concept);
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
        creator.addNeccTypes(types, typesExpr);
        List<OWLClassExpression> concepts= creator.negationConcepts;

        return concepts;
    }

    private void cutProblems(LPProblem problem, Double percentageOfPositiveExamples, Double percentageOfNegativeExamples) {
        keepPercentage(problem.positives, percentageOfPositiveExamples);
        keepPercentage(problem.negatives, percentageOfNegativeExamples);
    }

    private Collection<String> getRandom(List<String> list, Integer max) {
        int maxNoOfIndividuals = Math.min(max, list.size());
        Set<Integer> indexes = new HashSet<Integer>();
        Random rand = new Random(seed);
        while(indexes.size()<maxNoOfIndividuals) {
            indexes.add(rand.nextInt(list.size()));
        }
        Collection<String> ret = new HashSet<String>();
        for(Integer index : indexes){
            ret.add(list.get(index));
        }
        return ret;
    }

    private List<String> keepPercentage(Collection<String> removeFrom, Double percentage) {
        List<String> removed = new ArrayList<String>();
        Double max = removeFrom.size()*percentage;
        if(max<minExamples){
            max=minExamples*1.0;
        }
        if(max>maxExamples){
            max=maxExamples*1.0;
        }
        Collection<String> keep = getRandom(new ArrayList(removeFrom), max.intValue());
        removeFrom.clear();
        removeFrom.addAll(keep);
        return removed;
    }

    private LPProblem generateLPProblem(PosNegExample concept, Parser parser, boolean negativeGenerated) {
        LPProblem problem = new LPProblem();
        problem.negativeGenerated=negativeGenerated;
        problem.goldStandardConcept=concept.getPositive();

        OWLClassExpression pos = parser.parseManchesterConcept(concept.getPositive());
        pos = transformConceptSubClasses(pos, "OR", false);
        problem.goldStandardConceptExpr=pos;
        problem.rules = parser.getRulesInExpr(pos, problem.dataRules);

        problem.positives.addAll(retriever.retrieveIndividualsForConcept(pos));
        for(OWLClassExpression neg : concept.getNegatives()){
            OWLClassExpression conc = neg;
            if(negativeGenerated){
                conc = transformConceptSubClasses(neg, "OR", true);
            }

            List<String> retrieved = retriever.retrieveIndividualsForConcept(conc);
            problem.negatives.addAll(retrieved);
            problem.rules.addAll(parser.getRulesInExpr(neg, problem.dataRules));
            OWLClassExpression finalConc = conc;
            retrieved.forEach(retr -> {
                problem.negativeMap.put(retr, finalConc);
            });
        }
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

    public void addIndividuals(OWLOntology ontology, Collection<OWLAxiom> individuals){
        for(OWLAxiom axiom : individuals){
            ontology.addAxiom(axiom);
        }
    }

    public void saveOntology(String output, OWLOntology ontology){
        try(FileOutputStream fos = new FileOutputStream(output))   {
            ontology.saveOntology(new TurtleDocumentFormat(), fos);
        } catch (IOException | OWLOntologyStorageException e) {
            LOGGER.error("Could not store ontology. ", e);
        }

    }

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
