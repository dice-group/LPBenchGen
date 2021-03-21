package org.dice_group.LPBenchGen.lp;

import com.google.common.collect.Lists;
import org.dice_group.LPBenchGen.config.Configuration;
import org.dice_group.LPBenchGen.config.PosNegExample;
import org.dice_group.LPBenchGen.dl.ABoxFiller;
import org.dice_group.LPBenchGen.dl.OWLNegationCreator;
import org.dice_group.LPBenchGen.dl.OWLTBoxPositiveCreator;
import org.dice_group.LPBenchGen.dl.Parser;
import org.dice_group.LPBenchGen.sparql.IndividualRetriever;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectIntersectionOfImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectUnionOfImpl;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class LPGenerator {

    private IndividualRetriever retriever;
    private long seed=0;
    private int minExamples;
    private int maxExamples;
    private Parser parser;
    private Map<String, Collection<String[]>> rulesMapping = new HashMap<String, Collection<String[]>>();
    private Map<String, Collection<Object[]>> dataRulesMapping = new HashMap<String, Collection<Object[]>>();
    private List<String> types;
    private OWLClassExpression typesExpr;
    private Reasoner res;


    public void createBenchmark(String configFile, String name) throws IOException, OWLOntologyCreationException {
        Configuration conf = Configuration.loadFromFile(configFile);
        parser = new Parser(conf.getOwlFile());
        org.semanticweb.HermiT.Configuration conf2 = new org.semanticweb.HermiT.Configuration();
        conf2.ignoreUnsupportedDatatypes=true;
        res = new Reasoner(conf2, parser.getOntology());
        //debugAdd();
        retriever = new IndividualRetriever(conf.getEndpoint());
        seed= conf.getSeed();
        types = conf.getTypes();
        minExamples=conf.getMinNoOfExamples();
        maxExamples=conf.getMaxNoOfExamples();


        Collection<LPProblem> problems = new ArrayList<LPProblem>();

        if(conf.getConcepts()==null) {

            conf.setConcepts(generateConcepts(conf.getMaxGenerateConcepts(), conf.getMinConceptLength(), conf.getMaxConceptLength(), conf.getMaxDepth(), conf.getInferDirectSuperClasses()));
        }
        createTypesExpr();
        int count=0;
        for(PosNegExample concept : conf.getConcepts()){
            boolean negativeGenerated=false;
            if(concept.getNegatives() == null || concept.getNegatives().size()==0){
                concept.setNegatives(generateNegativeConcepts(concept.getPositive()));
                negativeGenerated=true;
            }
            try {
                problems.add(generateLPProblem(concept, parser,negativeGenerated));
                count++;
                System.out.println("Finished problem generation " + count + "/" + conf.getConcepts().size());
            }catch(Exception e ){
                e.printStackTrace();
            }
        }
        //for all individuals in LP problem add them
        ABoxFiller filler = new ABoxFiller(retriever, types);
        fillAbox(filler, problems,conf.getMaxIndividualsPerExampleConcept());
        /*Collection<String> individuals = getIndividuals(problems, conf.getTypes(), conf.getMaxNoOfIndividuals(), conf.getMaxIndividualsPerExampleConcept());
        for(String individual : individuals){
            addIndividuals(parser.getOntology(), createIndividual(individual, conf.getTypes()));
        }*/
        for(LPProblem problem : problems) {
            cutProblems(problem, conf.getPercentageOfPositiveExamples(), conf.getPercentageOfNegativeExamples());
            if(!conf.isEndpointInfersRules()) {
                measure(problem, parser.getOntology());
            }
        }
        System.out.println("Ontology has now "+parser.getOntology().getIndividualsInSignature().size()+" Individuals");
        //debugAdd();
        saveOntology(name+"-ontology.owl", parser.getOntology());
        saveLPProblem(name+"-lp.json", problems);
    }

    private void debugAdd() {
        OWLAxiom axiom;
        OWLDataFactory factory = new OWLDataFactoryImpl();
        OWLObjectPropertyExpression pexpr = factory.getOWLObjectProperty(IRI.create("http://dbpedia.org/ontology/headquarter"));
        OWLClass orga = factory.getOWLClass("http://dbpedia.org/ontology/Organisation");
        axiom=factory.getOWLObjectPropertyRangeAxiom(pexpr, orga);
        parser.getOntology().add(axiom);
    }

    private void createTypesExpr() {
        List<OWLClassExpression> classes = new ArrayList<OWLClassExpression>();
        for(String type: types){
            classes.add(new OWLDataFactoryImpl().getOWLClass(IRI.create(type)));
        }
        typesExpr = new OWLObjectUnionOfImpl(classes);
    }

    private void measure(LPProblem problem, OWLOntology ontology) {
        org.semanticweb.HermiT.Configuration conf = new org.semanticweb.HermiT.Configuration();
        conf.ignoreUnsupportedDatatypes=true;
        conf.throwInconsistentOntologyException=false;

        Reasoner res = new Reasoner(conf, ontology);
        Set<OWLNamedIndividual> instances = res.getInstances(parser.parseManchesterConcept(problem.goldStandardConcept), true).getFlattened();
        Set<String> temp = new HashSet<String>();
        instances.forEach(i ->{
            temp.add(i.getIRI().toString());
        });
        Set<String> temp2 = new HashSet<String>();
        problem.positives.forEach(indi ->{
            temp2.add(indi);
        });
        temp2.removeAll(temp);
        if(temp2.size()!=0){
            System.out.println("Found "+temp+" not found.");
        }
        int negCount=0;
        Set<String> rem = new HashSet<String>();
        for(String p : problem.negatives){
            if(temp.contains(p)){
                rem.add(p);
                negCount++;
            }
        }
        problem.negatives.removeAll(rem);
        if(rem.size()>0) {
            System.out.println("Found " + negCount + " which shouldn't be found");
        }
    }

    private void fillAbox(ABoxFiller filler, Collection<LPProblem> problems, Integer maxIndividualsPerExampleConcept) {
        AtomicInteger count= new AtomicInteger();
        problems.forEach(problem ->{
            problem.positives = getRandom(new ArrayList<>(problem.positives), maxIndividualsPerExampleConcept);
            problem.negatives = getRandom(new ArrayList<>(problem.negatives), maxIndividualsPerExampleConcept);
            ArrayList<String> rem = new ArrayList<>();
            problem.positives.forEach(pos ->{
                if(!filler.addIndividualsFromConcept(problem.goldStandardConceptAsExpr(), pos, parser.getOntology())){
                    rem.add(pos);
                }
            });
            if(!rem.isEmpty()) {
                System.out.println("Errors occured at individuals, removing " + rem + " from positives");
                problem.positives.removeAll(rem);
            }
            rem.clear();
            problem.negatives.forEach(nes ->{
                //TODO remove if error
                OWLClassExpression negativeExpr = problem.getExpr(nes);
                AtomicReference<OWLClassExpression> actNegExa = new AtomicReference<>(negativeExpr);
                if(problem.negativeGenerated) {
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
                if(!filler.addIndividualsFromConcept(actNegExa.get(), nes, parser.getOntology())){
                    rem.add(nes);
                }
            });
            if(!rem.isEmpty()) {
                System.out.println("Errors occured at individuals, removing " + rem + " from negatives");
                problem.negatives.removeAll(rem);
                rem.clear();
            }
            count.getAndIncrement();
            System.out.println("Filled Abox for problem "+(count)+"/"+problems.size());
        });
    }

    private List<PosNegExample> generateConcepts(Integer maxGenerateConcepts, Integer minConceptLength, Integer maxConceptLength, Integer maxDepth, boolean inferDirectSuperClasses) {
        List<PosNegExample> examples =new ArrayList<PosNegExample>();

        OWLTBoxPositiveCreator creator = new OWLTBoxPositiveCreator(retriever, parser.getOntology(), types, parser, res);
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

    private Collection<String> getIndividuals(Collection<LPProblem> problems, List<String> types, Integer maxNoOfIndividuals,Integer maxNoOfIndividualsPerExampleConcept) {
        Collection<String> ret = new TreeSet<String>();

        for(String type : types){
            List<String> typeIndividuals = retriever.retrieveIndividualsForType(type);
            ret.addAll(getRandom(typeIndividuals, maxNoOfIndividuals));
        }
        for(LPProblem problem : problems){
            //TODO Axioms from
            problem.positives = getRandom(new ArrayList<>(problem.positives), maxNoOfIndividualsPerExampleConcept);
            problem.negatives = getRandom(new ArrayList<>(problem.negatives), maxNoOfIndividualsPerExampleConcept);
            ret.addAll(problem.positives);
            ret.addAll(problem.negatives);

            addRules(ret, problem.positives, problem.rules, problem.dataRules);
            addRules(ret, problem.negatives, problem.rules, problem.dataRules);
        }
        return ret;
    }

    private void addRules(Collection<String> addTo, Collection<String> examples, Collection<String> rules, Collection<OWLDataProperty> dataRules) {
        for (String uri : examples) {
            Collection<String[]> retrvRules = retriever.retrieveIndividualsForRule(uri, rules);
            Collection<Object[]> retrvDataRules = retriever.retrieveIndividualsForDataRule(uri, dataRules);
            dataRulesMapping.put(uri, retrvDataRules);
            rulesMapping.put(uri, retrvRules);
            retrvRules.forEach(rule -> {
                addTo.add(rule[1]);
            });
        }
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
        //TODO check core:Protein  and (core:encodedBy some core:Gene),  http://purl.uniprot.org/EMBLWGS/ECF0332865

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
            String shortFormType = parser.getShortName(type);
            if(conceptString.contains(shortFormType)) {
                conceptString = conceptString.replace(shortFormType, getIntersectSubClasses(type, combiner));
            }
        }
        //parser.getShortName(conceptString);
        return new OWLObjectIntersectionOfImpl(Lists.newArrayList(parser.parseManchesterConcept(conceptString), typesExpr));
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

    private CharSequence getUnionSubClasses(String type) {
        OWLDataFactory factory = new OWLDataFactoryImpl();
        StringBuilder builder = new StringBuilder("( ").append(parser.getShortName(type)).append(" OR ");
        AtomicInteger count= new AtomicInteger();
        res.getSubClasses(factory.getOWLClass(type)).getFlattened().stream()
                .filter(subClass -> types.contains(subClass.getIRI().toString())).forEach(containedSubClass ->{
                    builder.append(parser.getShortName(containedSubClass.getIRI().toString()));
                    builder.append(" OR ");
                    count.getAndIncrement();
        });
        if(count.get() ==0) {
            return parser.getShortName(type);
        }
        return builder.subSequence(0, builder.length() - 4) + ")";
    }

    private CharSequence getIntersectSubClasses(String type, String combiner) {
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
        /*
        for(OWLClass subClass : res.getSuperClasses(factory.getOWLClass(type)).getFlattened()){
            if(types.contains(subClass.getIRI().toString())){
                builder.append(parser.getShortName(subClass.getIRI().toString()));
                builder.append(" ").append(combiner).append(" ");
                count.getAndIncrement();
            }
        }
        */

        if(count.get() ==0) {
            return parser.getShortName(type);
        }
        return builder.subSequence(0, builder.length() - 4) + ")";
    }

    public Collection<OWLAxiom> createIndividual(String uri, Collection<String> typesAllowed){
        OWLDataFactory factory = new OWLDataFactoryImpl();
        Collection<OWLAxiom> axioms = new ArrayList<OWLAxiom>();
        //1. retrieve all types (which are also in the config) for uri as subject
        Collection<String> types = retriever.retrieveTypesForIndividual(uri);
        Collection<String> disallowed = new HashSet<String>();
        for(String type : types){
            if(!typesAllowed.contains(type)){
                disallowed.add(type);
            }
        }
        types.removeAll(disallowed);
        OWLNamedIndividual individual = factory.getOWLNamedIndividual(IRI.create(uri));
        //3. create with OWLDataFactory assertions for all retrieved types
        for(String type : types){
            OWLClassExpression expr = factory.getOWLClass(IRI.create(type));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(expr, individual);
            axioms.add(axiom);
        }

        if(rulesMapping.containsKey(uri)) {
            for (String[] rule : rulesMapping.get(uri)) {
                OWLNamedIndividual obj = factory.getOWLNamedIndividual(IRI.create(rule[1]));
                OWLObjectProperty prop = factory.getOWLObjectProperty(IRI.create(rule[0]));
                OWLAxiom axiom = factory.getOWLObjectPropertyAssertionAxiom(prop, individual, obj);
                axioms.add(axiom);

            }
        }

        if(dataRulesMapping.containsKey(uri)) {
            for (Object[] rule : dataRulesMapping.get(uri)) {
                OWLDataProperty prop = (OWLDataProperty) rule[0];
                OWLAxiom axiom = factory.getOWLDataPropertyAssertionAxiom(prop, individual, (OWLLiteral)rule[1]);
                axioms.add(axiom);

            }
        }
        return axioms;
    }

    public void addIndividuals(OWLOntology ontology, Collection<OWLAxiom> individuals){
        for(OWLAxiom axiom : individuals){
            ontology.addAxiom(axiom);
        }
    }

    public void saveOntology(String output, OWLOntology ontology){
        try(FileOutputStream fos = new FileOutputStream(output))   {
            ontology.saveOntology(fos);
        } catch (IOException | OWLOntologyStorageException e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
}
