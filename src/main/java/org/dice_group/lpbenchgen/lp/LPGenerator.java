package org.dice_group.lpbenchgen.lp;

import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.OpenlletReasonerFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.dice_group.lpbenchgen.config.Configuration;
import org.dice_group.lpbenchgen.config.PosNegExample;
import org.dice_group.lpbenchgen.dl.ABoxFiller;
import org.dice_group.lpbenchgen.dl.OWLTBoxConceptCreator;
import org.dice_group.lpbenchgen.dl.creator.OWLNegationCreator;
import org.dice_group.lpbenchgen.dl.creator.OWLTBoxPositiveCreator;
import org.dice_group.lpbenchgen.dl.Parser;
import org.dice_group.lpbenchgen.sparql.IndividualRetriever;
import org.dice_group.lpbenchgen.sparql.retriever.ModelClosedWorldIndividualRetriever;
import org.dice_group.lpbenchgen.sparql.retriever.ModelOpenWorldIndividualRetriever;
import org.dice_group.lpbenchgen.sparql.retriever.SPARQLClosedWorldIndividualRetriever;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * The  Learning Problem Generator
 *
 * @author Lixi Alié Conrads
 */
public class LPGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LPGenerator.class.getName());
    public static final String RDF_PREFIX = "https://lpbenchgen.org/";
    public static final String RESOURCE_PREFIX = RDF_PREFIX + "resource/";
    public static final String PROPERTY_PREFIX = RDF_PREFIX + "property/";

    public static final String RDF_PREFIX_LP = RESOURCE_PREFIX + "lp_";
    public static final Resource LEARNING_PROBLEM_CLASS = ResourceFactory.createResource(RDF_PREFIX + "class/LearningProblem");
    public static final Property RDF_PROPERTY_INCLUDE = ResourceFactory.createProperty(PROPERTY_PREFIX + "includesResource");
    public static final Property RDF_PROPERTY_EXCLUDE = ResourceFactory.createProperty(PROPERTY_PREFIX + "excludesResource");
    public static final Property RDF_PROPERTY_CONCEPT = ResourceFactory.createProperty(PROPERTY_PREFIX + "concept");


    protected IndividualRetriever retriever;
    protected Parser parser;
    protected List<OWLClass> types;
    protected OWLReasoner res;
    private boolean isABoxGenerated = false;
    private boolean isSPARQLEndpoint = false;
    protected Configuration conf;

    /**
     * Initializes the generator with a Configuration
     *
     * @param conf The configuration to initialize with.
     * @throws OWLOntologyCreationException if the ontology inside the configuration cannot be created
     * @throws FileNotFoundException        If the owl File is not found
     */
    public void init(Configuration conf) throws OWLOntologyCreationException, FileNotFoundException {
        parser = new Parser(conf.getOwlFile());
        conf.prepare(parser);
        this.conf = conf;

        res = createReasoner();
        if (new File(conf.getEndpoint()).exists()) {
            if (conf.isOpenWorldAssumption()) {
                retriever = new ModelOpenWorldIndividualRetriever(conf.getEndpoint());
            } else {
                retriever = new ModelClosedWorldIndividualRetriever(conf.getEndpoint());
            }
        } else if (validUrl(conf.getEndpoint())) {
            if (conf.isOpenWorldAssumption()) {
                LOGGER.warn("You're using a SPARQL endpoint and an OpenWorld assumption, this might lead to empty problems.");
            }
            retriever = new SPARQLClosedWorldIndividualRetriever(conf.getEndpoint());
            isSPARQLEndpoint = true;
        } else {
            LOGGER.error("Endpoint is neither rdf/ontology file (file does not exists), nor valid URL");
            System.exit(1);
        }
        types = conf.getTypes().stream()
                .map(parser::parseManchesterConcept)
                .filter(AsOWLClass::isOWLClass)
                .map(type -> (OWLClass) type)
                .collect(Collectors.toList());

        if (types.isEmpty()) {
            Set<OWLClass> classes = parser.getOntology().getClassesInSignature();
            classes.stream()
                    .filter(cl -> !cl.getIRI().isReservedVocabulary())
                    .forEach(cl -> types.add(cl));
        }
    }

    private boolean validUrl(String endpoint) {
        try {
            URI.create(endpoint);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Collection<LPProblem> generateProblems(List<PosNegExample> concepts) {
        Collection<LPProblem> problems = new ArrayList<>();
        int count = 0;
        for (PosNegExample concept : concepts) {
            boolean negativeGenerated = concept.isNegativeGenerated();
            try {
                problems.add(generateLPProblem(concept, parser, negativeGenerated));
                count++;
                LOGGER.info("Finished generating examples for {}/{} problem.", count, concepts.size());
            } catch (Exception e) {
                LOGGER.error("Could not generate problem. ", e);
            }
        }
        return problems;
    }

    private OWLReasoner createReasoner() {
        OpenlletReasoner res = OpenlletReasonerFactory.getInstance().createReasoner(parser.getOntology());
        res.prepareReasoner();
        return res;
    }

    private void validateProblems(Collection<LPProblem> problems, boolean openWorldAssumption, double percOfPositiveExamples, double percOfNegativeExamples, boolean setActualPositives) {
        OWLReasoner res = createReasoner();
        if (!isABoxGenerated) {
            if (retriever instanceof ModelOpenWorldIndividualRetriever) {
                res = ((ModelOpenWorldIndividualRetriever) retriever).getReasoner();
            }
            //otherwise CWA -> we don't need the res.
            //Note: OWA+SPARQl+!isABoxGenerated is disallowed
        }
        List<LPProblem> emptyProblems = new ArrayList<>();
        for (LPProblem problem : problems) {
            if (openWorldAssumption) {
                measure(problem, res, setActualPositives);
                if (problem.positives.size() == 0 || problem.negatives.size() == 0) {
                    emptyProblems.add(problem);
                }
            }
            if (percOfPositiveExamples < 1.0 || percOfNegativeExamples < 1.0) {
                cutProblems(problem, percOfPositiveExamples, percOfNegativeExamples);
            }
        }
        problems.removeAll(emptyProblems);
    }


    /**
     * Generates an ABox fitting for the problems and storing that at name-ontology.ttl
     *
     * @param problems the problems the abox should be created for
     * @param fill     The Ontology to fill the ABox into
     * @return the filled ontology
     */
    public OWLOntology generateABox(Collection<LPProblem> problems, OWLOntology fill) {
        //for all individuals in LP problem add them
        LOGGER.info("Filling ABox now.");
        ABoxFiller filler = new ABoxFiller(retriever, types, conf.getAboxResultRetrievalLimit());
        fillAbox(filler, problems, fill);
        LOGGER.info("Filled ABox.");

        if (conf.isRemoveLiterals()) {
            LOGGER.info("Removing Literals from Ontology now.");
            removeLiteralsFromOntology();
        }
        return fill;
    }

    /**
     * Creates the actual benchmark.
     *
     * @param configFile   the benchmark configuration File
     * @param generateABox if an ABox should be generated
     * @return the generated benchmark
     * @throws IOException                  the io exception
     * @throws OWLOntologyCreationException the owl ontology creation exception
     */
    public LPBenchmark createBenchmark(String configFile, boolean generateABox) throws IOException, OWLOntologyCreationException {
        conf = Configuration.loadFromFile(configFile);
        return createBenchmark(conf, generateABox);
    }

    /**
     * Creates the actual benchmark.
     *
     * @param conf         the benchmark configuration
     * @param generateABox if an abox should be generated
     * @return the generated benchmark
     * @throws IOException                  the io exception
     * @throws OWLOntologyCreationException the owl ontology creation exception
     */
    public LPBenchmark createBenchmark(Configuration conf, boolean generateABox) throws IOException, OWLOntologyCreationException {
        this.conf = conf;
        init(conf);
        if (!generateABox && conf.isOpenWorldAssumption() && isSPARQLEndpoint) {
            LOGGER.error("Using a SPARQL endpoint with OWA and not generating an ABox is currently not possible.");
            System.exit(1);
        }
        if (conf.getConcepts() == null) {
            LOGGER.info("Generating concepts now...");
            conf.setConcepts(generateConcepts(conf.getMaxGenerateConcepts(), conf.getNamespace()));
            LOGGER.info("Generated {} positive concepts.", conf.getConcepts().size());
        } else {
            //generate negative for all concepts that do not have set negatives.
            generateNegativeConcepts(conf.getConcepts());
        }
        LOGGER.info("Starting to generate examples from concepts");
        Collection<LPProblem> problems = generateProblems(conf.getConcepts());
        OWLOntology abox = null;
        if (generateABox) {
            abox = generateABox(problems, parser.getOntology());
            isABoxGenerated = true;
        }
        LPBenchmark benchmark = createBenchmark(conf, problems);
        benchmark.setABox(abox);
        validateProblems(benchmark.getTrain(), conf.isOpenWorldAssumption(), 1.0, conf.getPercentageOfNegativeExamples(), true);
        validateProblems(benchmark.getGold(), conf.isOpenWorldAssumption(), 1.0, 1.0, true);
        validateProblems(benchmark.getTest(), conf.isOpenWorldAssumption(), conf.getPercentageOfPositiveExamples(), conf.getPercentageOfNegativeExamples(), false);

        return benchmark;
    }

    private void generateNegativeConcepts(List<PosNegExample> concepts) {
        List<PosNegExample> remove = new ArrayList<>();
        for (PosNegExample example : concepts) {
            if (example.getNegativesExpr().isEmpty()) {
                LOGGER.info("Generating negative examples for concept {}", example.getPositive());
                OWLNegationCreator creator = new OWLNegationCreator();
                OWLClassExpression concept = parser.parseManchesterConcept(example.getPositive());
                concept.accept(creator);
                //creator.prune();
                int negativeSize = 0;
                for (OWLClassExpression negativeConcept : creator.negationConcepts) {
                    negativeSize += retriever.retrieveIndividualsForConcept(negativeConcept, conf.getMinNoOfExamples(), 5, true).size();
                }
                if ((conf.isStrict() && negativeSize >= conf.getMinNoOfExamples()) || (!conf.isStrict() && negativeSize > 0)) {
                    example.setNegativeGenerated(true);
                    example.setNegativesExpr(creator.negationConcepts);
                } else {
                    LOGGER.warn("Couldn't retrieve enough negative examples for concept {}. Removing problem", example.getPositive());
                    remove.add(example);
                }
            }
        }
        concepts.removeAll(remove);
    }

    /**
     * <pre>
     * Creates the train, test and gold standard (test) files for the benchmark configuration and the problems
     *
     * If Open World Assumption is used it will also validate the problems using an Openllet Reasoner.
     * </pre>
     *
     * @param conf     the benchmark configuration
     * @param problems the problems the abox should be created for
     * @return The LPBenchmark containing train, test, gold and abox
     */
    protected LPBenchmark createBenchmark(Configuration conf, Collection<LPProblem> problems) {
        List<LPProblem> train = new ArrayList<>();
        List<LPProblem> gold = new ArrayList<>();
        List<LPProblem> test = new ArrayList<>();
        Double split = conf.getSplitContainment();
        problems = problems.stream().filter(problem -> problem.negatives.size() > 0).collect(Collectors.toList());
        double trainSize = split * problems.size();
        AtomicInteger count = new AtomicInteger();
        problems.stream().forEach(problem -> {
            if (count.get() < trainSize) {
                train.add(problem);
            } else {
                gold.add(problem);
                test.add(problem.getCopy());
            }
            count.getAndIncrement();
        });


        LPBenchmark benchmark = new LPBenchmark();
        benchmark.setTest(test);
        benchmark.setGold(gold);
        benchmark.setTrain(train);
        return benchmark;
    }

    /**
     * <pre>
     * Will save the abox, the train, test and gold standard datasets
     * Depending on the format will save these in json or turtle format to
     * name-train.ttl name-test.ttl name-test-goldstd.ttl
     * </pre>
     *
     * @param benchmark the benchmark
     * @param name      the benchmark name
     * @param format    json or rdf
     */
    public void saveLPBenchmark(LPBenchmark benchmark, String name, String format) {
        if (benchmark.getAbox() != null) {
            LOGGER.info("Saving generated ABox ");
            saveOntology(name + "-ontology.ttl", benchmark.getAbox());
            LOGGER.info("ABox saved.");
        }


        LOGGER.info("Generating benchmark files now... ");

        if (format.equals("json")) {
            saveLPProblemAsJSON(name + "-train.json", benchmark.getTrain(), true, true);
            saveLPProblemAsJSON(name + "-test-goldstd.json", benchmark.getGold(), false, true);
            saveLPProblemAsJSON(name + "-test.json", benchmark.getTest(), true, false);
        } else {
            saveLPProblemAsRDF(name + "-train.ttl", benchmark.getTrain(), true, true);
            saveLPProblemAsRDF(name + "-test-goldstd.ttl", benchmark.getGold(), false, true);
            saveLPProblemAsRDF(name + "-test.ttl", benchmark.getTest(), true, false);
        }
        LOGGER.info("Benchmark files saved... ");

    }

    private void saveLPProblemAsRDF(String out, Collection<LPProblem> problems, boolean includeNegative, boolean addConcepts) {
        Model m = ModelFactory.createDefaultModel();
        m.setNsPrefix("lpres", RDF_PREFIX + "resource/");
        m.setNsPrefix("lpprop", RDF_PREFIX + "property/");
        m.setNsPrefix("lpclass", RDF_PREFIX + "class/");
        for (String prefix : parser.getPrefix().getPrefixName2PrefixMap().keySet()) {
            if (!prefix.equals(":")) {
                m.setNsPrefix(prefix.replace(":", ""), parser.getPrefix().getPrefixName2PrefixMap().get(prefix));
            }
        }
        AtomicInteger count = new AtomicInteger(1);
        problems.forEach(problem -> {
            //System.out.println(count.get()+"\t"+problem.goldStandardConcept);
            addProblemToModel(m, problem, count.getAndIncrement(), includeNegative, addConcepts);

        });
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(out))) {
            RDFDataMgr.write(bos, m, Lang.TTL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addProblemToModel(Model m, LPProblem problem, int id, boolean includeNegative, boolean addConcepts) {
        /*
          :lp_1 a :LearningProblem;
          :includesResource :res_1  , :res_2
          :excludesResource :res_1  , :res_3 , :res_4 .
         */
        Resource res = ResourceFactory.createResource(RDF_PREFIX_LP + id);
        m.add(res, RDF.type, LEARNING_PROBLEM_CLASS);
        if (addConcepts) {
            m.add(res, RDF_PROPERTY_CONCEPT, problem.goldStandardConcept);
        }
        problem.positives.forEach(include -> m.add(res, RDF_PROPERTY_INCLUDE, ResourceFactory.createResource(include.getIRI().toString())));
        if (includeNegative) {
            problem.negatives.forEach(exclude -> m.add(res, RDF_PROPERTY_EXCLUDE, ResourceFactory.createResource(exclude.getIRI().toString())));
        }
    }


    private void measure(LPProblem problem, OWLReasoner res, boolean setActualPositives) {
        LOGGER.info("Checking concept {}.", problem.goldStandardConcept);

        Set<OWLNamedIndividual> actual = res.getInstances(problem.goldStandardConceptExpr).getFlattened();

        if (setActualPositives) {
            problem.positives = actual;
        } else {
            problem.positives.removeAll(problem.positives.stream()
                    .filter(pos -> {
                        if (!actual.contains(pos)) {
                            LOGGER.warn("FP: {}", pos);
                            return true;
                        }
                        return false;

                    }).collect(Collectors.toSet()));
        }
        problem.negatives.removeAll(
                problem.negatives.stream()
                        .filter(nes -> {
                            if (actual.contains(nes)) {
                                LOGGER.warn("FN: {}", nes);
                                LOGGER.warn("Negative concept: {}", parser.render(problem.negativeMap.get(nes).getNNF()));
                                return true;
                            }
                            return false;

                        })
                        .collect(Collectors.toSet())
        );
    }

    private void fillABoxForProblem(LPProblem problem, ABoxFiller filler, OWLOntology onto) {
        { // add individuals from positive concepts
            List<OWLNamedIndividual> rem = problem.positives
                    .stream()
                    .filter(pos -> !filler.addIndividualsFromConcept(problem.goldStandardConceptAsExpr(), pos, onto))
                    .collect(Collectors.toList());

            if (!rem.isEmpty()) {
                LOGGER.info("Errors occurred at individuals, removing {} from positives.", rem);
                problem.positives.removeAll(rem);
            }
        }
        { // add individual from negatives
            List<OWLNamedIndividual> rem = problem.negatives.stream()
                    .filter(nes -> !filler.addIndividualsFromConcept(problem.getExpr(nes), nes, onto))
                    .collect(Collectors.toList());
            if (!rem.isEmpty()) {
                LOGGER.info("Errors occurred at individuals, removing {} from negatives.", rem);
                problem.negatives.removeAll(rem);
            }
        }
    }

    private void fillAbox(ABoxFiller filler, Collection<LPProblem> problems, OWLOntology fill) {
        AtomicInteger count = new AtomicInteger();
        AtomicInteger emptyCount = new AtomicInteger();
        AtomicLong size = new AtomicLong(problems.size());
        problems.forEach(problem -> {
            fillABoxForProblem(problem, filler, fill);
            count.getAndIncrement();
            if (problem.positives.isEmpty() || problem.negatives.isEmpty()) {
                emptyCount.getAndIncrement();
                size.getAndDecrement();
            }
            LOGGER.info("Filled ABox for problem {}/{} [{} empty]", count, size, emptyCount.get());
        });
    }


    private List<PosNegExample> generateConcepts(Integer maxGenerateConcepts, String namespace) {

        OWLTBoxConceptCreator creator = new OWLTBoxPositiveCreator(conf, retriever, parser.getOntology(), types, parser, res, namespace);
        List<PosNegExample> examples = new ArrayList<>(creator.createDistinctConcepts(maxGenerateConcepts));
        this.types = creator.getAllowedTypes();
        return examples;
    }


    private void cutProblems(LPProblem problem, Double percentageOfPositiveExamples, Double percentageOfNegativeExamples) {
        keepPercentage(problem.positives, percentageOfPositiveExamples);
        keepPercentage(problem.negatives, percentageOfNegativeExamples);
    }

    private Collection<OWLNamedIndividual> getRandom(List<OWLNamedIndividual> list, Integer max) {
        int maxNoOfIndividuals = Math.min(max, list.size());
        Random rand = new Random(conf.getSeed());
        Collections.shuffle(list, rand);
        Collection<OWLNamedIndividual> ret = new HashSet<>();
        int i = 0;
        while (ret.size() < maxNoOfIndividuals) {
            ret.add(list.get(i++));
        }
        return ret;
    }

    private List<OWLNamedIndividual> keepPercentage(Collection<OWLNamedIndividual> removeFrom, Double percentage) {
        List<OWLNamedIndividual> removed = new ArrayList<>(removeFrom);
        double max = Math.max(1, removeFrom.size() * percentage);
        if (max < conf.getMinNoOfExamples() && !conf.isStrict()) {
            max = conf.getMinNoOfExamples() * 1.0;
        }
        if (max > conf.getMaxNoOfExamples()) {
            max = conf.getMaxNoOfExamples() * 1.0;
        }

        Collection<OWLNamedIndividual> keep = getRandom(new ArrayList<>(removeFrom), (int) max);
        removed.removeAll(keep);
        removeFrom.removeAll(removed);
        return removed;
    }

    private LPProblem generateLPProblem(PosNegExample concept, Parser parser, boolean negativeGenerated) {
        LPProblem problem = new LPProblem();
        problem.negativeGenerated = negativeGenerated;
        problem.goldStandardConcept = concept.getPositive();

        OWLClassExpression pos = parser.parseManchesterConcept(concept.getPositive());
        problem.goldStandardConceptExpr = pos;
        problem.objectProperties = parser.getRulesInExpr(pos, problem.dataProperties);

        problem.positives.addAll(retriever.retrieveIndividualsForConcept(pos, conf.getPositiveLimit(), 180, true));
        for (OWLClassExpression neg : concept.getNegativesExpr()) {
            // TODO: this looks wrong and especially as if this is intended to be a copy
            OWLClassExpression conc = neg;

            List<OWLNamedIndividual> retrieved = retriever.retrieveIndividualsForConcept(conc, conf.getNegativeLimit(), 180, true);
            problem.negatives.addAll(retrieved);
            problem.objectProperties.addAll(parser.getRulesInExpr(neg, problem.dataProperties));
            OWLClassExpression finalConc = conc;
            retrieved.forEach(retr -> problem.negativeMap.put(retr, finalConc));
        }
        List<OWLNamedIndividual> negativeShuffle = new ArrayList<>(problem.negatives);
        Collections.shuffle(negativeShuffle, new Random(conf.getSeed()));
        problem.negatives = new HashSet<>(negativeShuffle);
        return problem;
    }


    /**
     * Add individuals.
     *
     * @param ontology    the ontology
     * @param individuals the individuals
     */
    public void addIndividuals(OWLOntology ontology, Collection<OWLAxiom> individuals) {
        for (OWLAxiom axiom : individuals) {
            ontology.addAxiom(axiom);
        }
    }

    /**
     * Save ontology.
     *
     * @param output   the output
     * @param ontology the ontology
     */
    public void saveOntology(String output, OWLOntology ontology) {
        try (FileOutputStream fos = new FileOutputStream(output)) {
            ontology.saveOntology(new TurtleDocumentFormat(), fos);
        } catch (IOException | OWLOntologyStorageException e) {
            LOGGER.error("Could not store ontology. ", e);
        }

    }

    /**
     * Saves the LP problems as json.
     *
     * @param output          the output
     * @param problems        the problems
     * @param includeNegative if negatives should be saved as well
     * @param addConcepts     if concepts should stored as well
     */
    private void saveLPProblemAsJSON(String output, Collection<LPProblem> problems, boolean includeNegative, boolean addConcepts) {
        try (PrintWriter pw = new PrintWriter(output)) {
            // print end
            pw.print("[\n");
            AtomicInteger count = new AtomicInteger();
            long size = problems.stream().filter(x -> x.positives.size() > 0 && x.negatives.size() > 0).count();
            problems.stream().filter(x -> x.positives.size() > 0 && x.negatives.size() > 0).forEach(problem -> {
                pw.print("\t{\n\t");
                if (addConcepts) {
                    pw.print("\"concept\": \"");
                    pw.print(problem.goldStandardConcept.replace("\n", " "));
                    pw.print("\",\n\t");
                }
                pw.print("\"positives\": [\n");
                writeCollection(problem.positives, pw);
                pw.print("\n\t]");
                if (includeNegative) {
                    pw.print(",\n\t\"negatives\": [\n");
                    writeCollection(problem.negatives, pw);
                    pw.print("\n\t]");
                }
                pw.print("\n\t}");
                count.getAndIncrement();
                if (count.get() < size) {
                    pw.print(",\n");
                }
            });
            pw.print("\n]");
        } catch (FileNotFoundException e) {
            LOGGER.error("Could not write LP Problem as file was not found.", e);
        }
    }

    private void writeCollection(Collection<OWLNamedIndividual> collection, PrintWriter pw) {
        int count = 0;
        for (OWLNamedIndividual individual : collection) {
            pw.print("\t\t\"");
            pw.print(individual.getIRI());
            pw.print("\"");
            count++;
            if (count < collection.size()) {
                pw.print(",\n");
            }

        }
    }


    /**
     * Remove literals from ontology.
     */
    private void removeLiteralsFromOntology() {
        OWLOntology ontology = this.parser.getOntology();
        List<OWLDataPropertyAssertionAxiom> rem = new ArrayList<>();
        ontology.getAxioms().stream().filter(x -> x instanceof OWLDataPropertyAssertionAxiom).forEach(ax -> {
            OWLDataPropertyAssertionAxiom axiom = (OWLDataPropertyAssertionAxiom) ax;
            rem.add(axiom);
        });
        ontology.remove(rem);
    }
}
